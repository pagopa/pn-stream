package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.configs.EnvironmentConfig;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.exceptions.PnTooManyRequestException;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import it.pagopa.pn.stream.utils.MetricUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@CustomLog
@RequiredArgsConstructor
public abstract class PnStreamServiceImpl {

    protected final StreamEntityDao streamEntityDao;
    protected final PnStreamConfigs pnStreamConfigs;
    protected final StreamUtils streamUtils;


    protected enum StreamEntityAccessMode {READ, WRITE}

    protected Mono<StreamEntity> getStreamEntityToWrite(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, boolean checkRetryAfter) {
        return getStreamEntityToWrite(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, false, checkRetryAfter)
            ;
    }
    protected Mono<StreamEntity> getStreamEntityToWrite(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, boolean ignoreVersion, boolean checkRetryAfter) {
        return filterEntity(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, streamId, StreamEntityAccessMode.WRITE, ignoreVersion, checkRetryAfter)
            .filter(entity -> !(CollectionUtils.isEmpty(entity.getGroups()) && !CollectionUtils.isEmpty(xPagopaPnCxGroups))
            || apiVersion(xPagopaPnApiVersion).equals(pnStreamConfigs.getFirstVersion()) //Se e' v10 non ho vincoli
            || ignoreVersion
            );
    }

    private Mono<StreamEntity> filterEntity(String xPagopaPnApiVersion, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, UUID streamId, StreamEntityAccessMode mode, boolean ignoreVersion, boolean checkRetryAfter) {
        final String apiV10 = pnStreamConfigs.getFirstVersion();
        return streamEntityDao.getWithRetryAfter(xPagopaPnCxId, streamId.toString())
                .flatMap(tuple -> {
                    if (checkRetryAfter && tuple.getT2().isPresent()) {
                        return checkRetryAfter(xPagopaPnCxId, xPagopaPnApiVersion, streamId, tuple.getT2().get(), tuple.getT1());
                    }
                    return Mono.just(tuple.getT1());
                })
                .filter(streamEntity ->
                        apiV10.equals(xPagopaPnApiVersion)
                                || (
                                mode == StreamEntityAccessMode.WRITE ? StreamUtils.checkGroups(streamEntity.getGroups(), xPagopaPnCxGroups) : StreamUtils.checkGroups(xPagopaPnCxGroups, streamEntity.getGroups())
                        )
                )
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Pa " + xPagopaPnCxId + " groups (" + join(xPagopaPnCxGroups) + ") is not allowed to see this streamId " + streamId)))
                .filter(filterMasterRequest(ignoreVersion, apiV10, xPagopaPnApiVersion, mode, xPagopaPnCxGroups))
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Only master key can change streamId " + streamId)))
                .filter(streamEntity -> ignoreVersion
                        || apiVersion(xPagopaPnApiVersion).equals(entityVersion(streamEntity))
                        || (streamEntity.getVersion() == null && apiV10.equals(xPagopaPnApiVersion))
                )
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Pa " + xPagopaPnCxId + " version " + apiVersion(xPagopaPnApiVersion) + " is trying to access streamId " + streamId + ": api version mismatch")));
    }

    private Mono<StreamEntity> checkRetryAfter(String xPagopaPnCxId, String xPagopaPnApiVersion, UUID streamId, StreamRetryAfter entityRetry, StreamEntity streamEntity) {
        if (Instant.now().isBefore(entityRetry.getRetryAfter())) {
            log.warn("Pa {} version {} is trying to access streamId {}: retry after not expired", xPagopaPnCxId, apiVersion(xPagopaPnApiVersion), streamId);
            if(Boolean.TRUE.equals(pnStreamConfigs.getEnableStreamStats())) {
                log.logMetric(List.of(MetricUtils.generateGeneralMetric(xPagopaPnCxId, streamId.toString(), StreamStatsEnum.RETRY_AFTER_VIOLATION.name(), 1, Instant.now().toEpochMilli())), "Logging metric : " + StreamStatsEnum.RETRY_AFTER_VIOLATION.name());
            }
            return ignoreOrThrowException(streamEntity);
        }
        return Mono.just(streamEntity);
    }

    private Mono<StreamEntity> ignoreOrThrowException(StreamEntity streamEntity) {
        if (Boolean.TRUE.equals(pnStreamConfigs.getRetryAfterEnabled())) {
            return Mono.error(new PnTooManyRequestException("Pa " + streamEntity.getPaId() + " version " + apiVersion(streamEntity.getVersion()) + " is trying to access streamId " + streamEntity.getStreamId() + ": retry after not expired"));
        }
        return Mono.just(streamEntity);
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
        return xPagopaPnApiVersion != null ? xPagopaPnApiVersion : pnStreamConfigs.getCurrentVersion();
    }
    protected String entityVersion(StreamEntity streamEntity){
        return ObjectUtils.defaultIfNull(streamEntity.getVersion(), pnStreamConfigs.getFirstVersion());
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
