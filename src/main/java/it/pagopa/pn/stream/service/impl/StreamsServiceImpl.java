package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.exceptions.PnStreamMaxStreamsCountReachedException;
import it.pagopa.pn.stream.exceptions.PnStreamNotFoundException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV28;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamListElement;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV28;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamRequestV28;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToDtoStreamMapper;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToStreamListDtoStreamMapper;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.StreamsService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Predicate;

import static it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter.RETRY_PREFIX;

@Service
@Slf4j
public class StreamsServiceImpl extends PnStreamServiceImpl implements StreamsService {

    public static final String ERROR_CREATING_STREAM = "error creating stream";
    public static final int DELAY = 60;
    private final SchedulerService schedulerService;
    private final PnExternalRegistryClient pnExternalRegistryClient;

    private final int purgeDeletionWaittime;

    public StreamsServiceImpl(StreamEntityDao streamEntityDao, SchedulerService schedulerService, StreamUtils streamUtils
            , PnStreamConfigs pnStreamConfigs, PnExternalRegistryClient pnExternalRegistryClient) {
        super(streamEntityDao, pnStreamConfigs, streamUtils);
        this.schedulerService = schedulerService;
        this.pnExternalRegistryClient = pnExternalRegistryClient;
        this.purgeDeletionWaittime = pnStreamConfigs.getPurgeDeletionWaittime();
    }

    @Override
    public Mono<StreamMetadataResponseV28> createEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV28> streamCreationRequest) {
        final String apiV10 = pnStreamConfigs.getFirstVersion();
        String msg = "createEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion};

        return streamCreationRequest.doOnNext(payload -> {
                    String[] fullArgs = ArrayUtils.add(args, payload.toString());
                    generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg + ", request={} ", fullArgs).log();
                }).flatMap(x ->
                        (x.getReplacedStreamId() == null ? checkStreamCount(xPagopaPnCxId) : Mono.just(Boolean.TRUE)).then(Mono.just(x))
                )
                .map(streamCreationRequestV28 -> {
                    if (Objects.isNull(streamCreationRequestV28.getWaitForAccepted())) {
                        streamCreationRequestV28.setWaitForAccepted(Boolean.FALSE);
                    }
                    return streamCreationRequestV28;
                })
                .flatMap(dto -> {
                    List<String> allowedGroups = CollectionUtils.isEmpty(xPagopaPnCxGroups)
                            ? pnExternalRegistryClient.getGroups(xPagopaPnUid, xPagopaPnCxId)
                            : xPagopaPnCxGroups;

                    if (CollectionUtils.isEmpty(dto.getGroups()) && !apiV10.equals(xPagopaPnApiVersion) && !CollectionUtils.isEmpty(xPagopaPnCxGroups)) {
                        return Mono.error(new PnStreamForbiddenException("Not Allowed empty groups for apikey with groups " + groupString(xPagopaPnCxGroups) + " when Api Version is " + xPagopaPnApiVersion));
                    } else {
                        return StreamUtils.checkGroups(dto.getGroups(), allowedGroups) ?
                                saveOrReplace(dto, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion)
                                : Mono.error(new PnStreamForbiddenException("Not Allowed groups " + groupString(dto.getGroups())));
                    }
                }).map(EntityToDtoStreamMapper::entityToDto).doOnSuccess(newEntity -> generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateSuccess().log()).doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_CREATE, msg, args).generateFailure(ERROR_CREATING_STREAM, err).log());
    }

    private Mono<StreamEntity> saveOrReplace(StreamCreationRequestV28 dto, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        return dto.getReplacedStreamId() == null
                ? streamEntityDao.save(DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), xPagopaPnApiVersion, dto))
                : replaceStream(xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, dto);
    }

    @Override
    public Mono<Void> deleteEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "deleteEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId ={} ";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString()};

        generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).log();

        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId, false)
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Cannot delete Stream")))
                .flatMap(filteredEntity ->
                        streamEntityDao.delete(xPagopaPnCxId, streamId.toString())
                                .then(Mono.fromSupplier(() -> {
                                    schedulerService.scheduleStreamEvent(streamId.toString(), null, purgeDeletionWaittime, StreamEventType.PURGE_STREAM);
                                    return null;
                                })))
                .doOnSuccess(empty -> generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateSuccess().log()).doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_DELETE, msg, args).generateFailure("error deleting stream", err).log()).then();
    }

    @Override
    public Mono<StreamMetadataResponseV28> getEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "getEventStream xPagopaPnUid={}, xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString());
        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).log();

        return streamEntityDao.get(xPagopaPnCxId, streamId.toString())
                .switchIfEmpty(Mono.error(new PnStreamNotFoundException("Stream  " + streamId + " not found ")))
                .filter(streamEntity -> entityVersion(streamEntity).equals(apiVersion(xPagopaPnApiVersion)))
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Stream  " + streamId + " cannot be accessed by  xPagopaPnCxId=" + xPagopaPnCxId)))
                .map(EntityToDtoStreamMapper::entityToDto)
                .doOnSuccess(entity ->
                        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateSuccess().log()
                )
                .doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateFailure("error getting stream", err).log());
    }

    @Override
    public Flux<StreamListElement> listEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion) {
        String msg = "listEventStream xPagopaPnUid={}, xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion);
        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).log();

        return streamEntityDao.findByPa(xPagopaPnCxId)
                .filter(entity -> !entity.getStreamId().startsWith(RETRY_PREFIX))
                .map(EntityToStreamListDtoStreamMapper::entityToDto)
                .doOnComplete(() ->
                        generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateSuccess().log()
                )
                .doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_READ, msg, args.toArray(new String[0])).generateFailure("error listing streams", err).log());
    }

    @Override
    public Mono<StreamMetadataResponseV28> updateEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV28> streamRequest) {
        String msg = "updateEventStream xPagopaPnUid={},xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={}, request={} ";
        List<String> args = Arrays.asList(xPagopaPnUid, xPagopaPnCxId, groupString(xPagopaPnCxGroups), streamId.toString(), xPagopaPnApiVersion);

        return streamRequest.doOnNext(payload -> {
                    List<String> values = new ArrayList<>(args);
                    values.add(payload.toString());
                    generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, values.toArray(new String[0])).log();
                })
                .flatMap(request -> getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId, false)
                        .filter(checkDisableDate())
                        .switchIfEmpty(Mono.error(new PnStreamForbiddenException(String.format("Stream [%s] is disabled, cannot be updated", streamId))))
                        .filter(filterUpdateRequest(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, request))
                        .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Not supported operation, groups cannot be removed")))
                        .map(r -> DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, streamId.toString(), xPagopaPnApiVersion, request))
                        .map(entity -> {
                            entity.setEventAtomicCounter(null);
                            entity.setSorting(null);
                            return entity;
                        })
                        .flatMap(streamEntityDao::update)
                        .map(EntityToDtoStreamMapper::entityToDto))
                .doOnSuccess(newEntity -> generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0]))
                        .generateSuccess().log()).doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_UPDATE, msg, args.toArray(new String[0]))
                        .generateFailure("error updating stream", err).log());
    }

    private Predicate<StreamEntity> filterUpdateRequest(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, StreamRequestV28 request) {
        return r -> {
            //Da master se non restringo i gruppi sullo stream OK
            if (CollectionUtils.isEmpty(r.getGroups())
                    && CollectionUtils.isEmpty(request.getGroups())
                    && CollectionUtils.isEmpty(xPagopaPnCxGroups)) {
                return true;
            }

            if (CollectionUtils.isEmpty(r.getGroups()) && !CollectionUtils.isEmpty(request.getGroups())) {
                return false;
            }

            if (!request.getGroups().containsAll(getGroups(r))) {
                return false;
            }

            List<String> allowedGroups = (CollectionUtils.isEmpty(xPagopaPnCxGroups) && !request.getGroups().isEmpty())
                    ? pnExternalRegistryClient.getGroups(xPagopaPnUid, xPagopaPnCxId)
                    : xPagopaPnCxGroups;

            return allowedGroups.containsAll(getGroups(r));
        };
    }

    private Predicate<StreamEntity> checkDisableDate() {
        return r -> {
            //Non posso aggiornare stream disabilitato
            if (r.getDisabledDate() != null) {
                log.error("Stream is disabled, cannot be updated!");
                return false;
            }
            return true;
        };
    }

    @Override
    public Mono<StreamMetadataResponseV28> disableEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId) {
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={}";
        String[] args = new String[]{xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();

        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId, false)
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Not supported operation, stream not owned")))
                .filter(streamEntity -> streamEntity.getDisabledDate() == null)
                .switchIfEmpty(
                        Mono.error(new PnStreamForbiddenException("Not supported operation, stream already disabled")))
                .flatMap(streamEntity ->
                        streamEntityDao.disable(streamEntity).map(EntityToDtoStreamMapper::entityToDto))
                .doOnSuccess(ok ->
                        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log())
                .doOnError(err ->
                        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure("Error in disableEventStream").log());
    }

    private Mono<Boolean> checkStreamCount(String xPagopaPnCxId) {
        return streamEntityDao.findByPa(xPagopaPnCxId)
                .filter(entity -> !entity.getStreamId().startsWith(RETRY_PREFIX))
                .filter(streamEntity -> streamEntity.getDisabledDate() == null)
                .collectList().flatMap(list -> {
                    if (list.size() >= streamUtils.retrieveMaxStreamsNumber(xPagopaPnCxId)) {
                        return Mono.error(new PnStreamMaxStreamsCountReachedException());
                    } else {
                        return Mono.just(Boolean.TRUE);
                    }
                });
    }

    private Mono<StreamEntity> replaceStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, StreamCreationRequestV28 dto) {
        StreamEntity streamEntity = DtoToEntityStreamMapper.dtoToEntity(xPagopaPnCxId, UUID.randomUUID().toString(), xPagopaPnApiVersion, dto);
        String msg = "disableEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, disabledStreamId={}, streamId={}";
        String[] args = new String[]{xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, dto.getReplacedStreamId().toString(), dto.getReplacedStreamId().toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).log();
        return getStreamEntityToWrite(xPagopaPnApiVersion, xPagopaPnCxId, xPagopaPnCxGroups, dto.getReplacedStreamId(),true,  false).
                flatMap(replacedStream -> replaceStreamEntity(streamEntity, replacedStream))
                .doOnSuccess(newEntity -> generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateSuccess().log())
                .doOnError(err -> generateAuditLog(PnAuditLogEventType.AUD_WH_DISABLE, msg, args).generateFailure(ERROR_CREATING_STREAM, err).log());
    }

    private Mono<StreamEntity> replaceStreamEntity(StreamEntity entity, StreamEntity replacedStream) {
        if (replacedStream.getDisabledDate() != null) {
            return Mono.error(new PnStreamForbiddenException("Not supported operation, stream already disabled"));
        } else {
            entity.setEventAtomicCounter(replacedStream.getEventAtomicCounter() + pnStreamConfigs.getDeltaCounter());
            return streamEntityDao.replaceEntity(replacedStream, entity);
        }

    }

    private List<String> getGroups(StreamEntity streamEntity) {
        return streamEntity.getGroups() == null ? Collections.emptyList() : streamEntity.getGroups();
    }
}
