package it.pagopa.pn.stream.service.impl;

import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_STREAM_STREAMNOTFOUND;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.exceptions.PnNotFoundException;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public abstract class StreamServiceImpl {

    protected final StreamEntityDao streamEntityDao;
    protected final PnStreamConfigs pnStreamConfigs;


    protected enum StreamEntityAccessMode {READ, WRITE}

    protected Mono<StreamEntity> getStreamEntityToRead(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId) {
        return filterEntity(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, StreamEntityAccessMode.READ, false);
    }
    protected Mono<StreamEntity> getStreamEntityToWrite(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId) {
        return getStreamEntityToWrite(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, false)
            ;
    }
    protected Mono<StreamEntity> getStreamEntityToWrite(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, boolean ignoreVersion) {
        return filterEntity(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, StreamEntityAccessMode.WRITE, ignoreVersion)
            .filter(entity -> !(CollectionUtils.isEmpty(entity.getGroups()) && !CollectionUtils.isEmpty(xPagopaPnCxGroups))
            || apiVersion(xPagopaPnApiVersion).equals(pnStreamConfigs.getWebhook().getFirstVersion()) //Se e' v10 non ho vincoli
            || ignoreVersion
            );
    }
    private Mono<StreamEntity> filterEntity(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, StreamEntityAccessMode mode, boolean ignoreVersion) {
        final String apiV10 = pnStreamConfigs.getWebhook().getFirstVersion();
        return streamEntityDao.get(xPagopaPnCxId,streamId.toString())
            .switchIfEmpty(Mono.error(
                new PnNotFoundException("Not found"
                    , String.format("Stream %s non found for Pa %s",streamId.toString(),xPagopaPnCxId)
                    , ERROR_CODE_STREAM_STREAMNOTFOUND)))
            .filter(streamEntity ->
                apiV10.equals(xPagopaPnApiVersion)
                || (
                    mode == StreamEntityAccessMode.WRITE ?  StreamUtils.checkGroups(streamEntity.getGroups(), xPagopaPnCxGroups) : StreamUtils.checkGroups(xPagopaPnCxGroups, streamEntity.getGroups())
                )
            )
            .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Pa " + xPagopaPnCxId + " groups (" + join(xPagopaPnCxGroups)+ ") is not allowed to see this streamId " + streamId)))
            .filter(filterMasterRequest(ignoreVersion,apiV10,xPagopaPnApiVersion,mode,xPagopaPnCxGroups))
            .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Only master key can change streamId " + streamId)))
            .filter(streamEntity -> ignoreVersion
                || apiVersion(xPagopaPnApiVersion).equals(entityVersion(streamEntity))
                || (streamEntity.getVersion() == null && apiV10.equals(xPagopaPnApiVersion))
            )
            .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Pa " + xPagopaPnCxId + " version " + apiVersion(xPagopaPnApiVersion)+ " is trying to access streamId " + streamId +": api version mismatch")));
    }

    private Predicate<StreamEntity> filterMasterRequest(boolean ignoreVersion, String apiV10, String xPagopaPnApiVersion, StreamEntityAccessMode mode, List<String> xPagopaPnCxGroups ) {
        return streamEntity -> {
            if (apiV10.equals(entityVersion(streamEntity)) && ignoreVersion){
                return true;
            }

            boolean isMaster = (!apiV10.equals(apiVersion(xPagopaPnApiVersion)) && !apiV10.equals(entityVersion(streamEntity)))
                    && mode == StreamEntityAccessMode.WRITE
                    && (CollectionUtils.isEmpty(streamEntity.getGroups()) && !CollectionUtils.isEmpty(xPagopaPnCxGroups));

            //Se non sono master non posso agire in scrittura su stream senza gruppi: solo per v23
            return !isMaster;

        };
    }

    protected String join(List<String> list){
        return list == null ? "" : String.join(",", list);
    }

    protected String apiVersion(String xPagopaPnApiVersion){
        return xPagopaPnApiVersion != null ? xPagopaPnApiVersion : pnStreamConfigs.getWebhook().getCurrentVersion();
    }
    protected String entityVersion(StreamEntity streamEntity){
        return ObjectUtils.defaultIfNull(streamEntity.getVersion(), pnStreamConfigs.getWebhook().getFirstVersion());
    }

    @NotNull
    protected PnAuditLogEvent generateAuditLog(PnAuditLogEventType pnAuditLogEventType, String message, String[] arguments) {
        String logMessage = MessageFormatter.arrayFormat(message, arguments).getMessage();
        PnAuditLogBuilder auditLogBuilder = new PnAuditLogBuilder();
        PnAuditLogEvent logEvent;
        logEvent = auditLogBuilder.before(pnAuditLogEventType, "{}", logMessage)
                .build();
        return logEvent;
    }

    protected String groupString(List<String> groups){
        return groups==null ? null : String.join(",",groups);
    }
}
