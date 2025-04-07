package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV24;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.*;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV28;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV28;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementV27;
import it.pagopa.pn.stream.middleware.dao.dynamo.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.*;
import it.pagopa.pn.stream.service.mapper.ProgressResponseElementMapper;
import it.pagopa.pn.stream.service.mapper.TimelineElementStreamMapper;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter.RETRY_PREFIX;
import static it.pagopa.pn.stream.service.utils.StreamUtils.checkGroups;


@Service
@Slf4j
public class StreamEventsServiceImpl extends PnStreamServiceImpl implements StreamEventsService {

    private static final String DEFAULT_CATEGORIES = "DEFAULT";
    private final EventEntityDao eventEntityDao;
    private final StreamNotificationDao streamNotificationDao;
    private final EventsQuarantineEntityDao eventsQuarantineEntityDao;
    private final UnlockedNotificationEntityDao notificationUnlockedEntityDao;
    private final PnDeliveryClientReactive pnDeliveryClientReactive;
    private final SchedulerService schedulerService;
    private final TimelineService timelineService;
    private final ConfidentialInformationService confidentialInformationService;

    private static final String LOG_MSG_JSON_COMPRESSION = "Error while compressing timeline elements into JSON for the audit";


    public StreamEventsServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao,
                                   SchedulerService schedulerService, StreamUtils streamUtils,
                                   PnStreamConfigs pnStreamConfigs, TimelineService timeLineService,
                                   ConfidentialInformationService confidentialInformationService,
                                   StreamNotificationDao streamNotificationDao, PnDeliveryClientReactive pnDeliveryClientReactive,
                                   EventsQuarantineEntityDao eventsQuarantineEntityDao, UnlockedNotificationEntityDao notificationUnlockedEntityDao,
                                   StreamStatsService streamStatsService) {
        super(streamEntityDao, pnStreamConfigs, streamStatsService, streamUtils);
        this.eventEntityDao = eventEntityDao;
        this.schedulerService = schedulerService;
        this.timelineService = timeLineService;
        this.confidentialInformationService = confidentialInformationService;
        this.streamNotificationDao = streamNotificationDao;
        this.pnDeliveryClientReactive = pnDeliveryClientReactive;
        this.eventsQuarantineEntityDao = eventsQuarantineEntityDao;
        this.notificationUnlockedEntityDao = notificationUnlockedEntityDao;
    }

    @Override
    public Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId,
                                                               List<String> xPagopaPnCxGroups,
                                                               String xPagopaPnApiVersion,
                                                               UUID streamId,
                                                               String lastEventId) {
        String msg = "consumeEventStream xPagopaPnCxId={}, xPagopaPnCxGroups={}, xPagopaPnApiVersion={}, streamId={} ";
        String[] args = {xPagopaPnCxId, groupString(xPagopaPnCxGroups), xPagopaPnApiVersion, streamId.toString()};
        generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).log();
        CustomStatsConfig statConfig = streamUtils.customStatsConfig();
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId, true)
                .doOnError(error -> generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).generateFailure("Error in reading stream").log())
                .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Cannot consume stream")))
                .flatMap(streamEntity -> {
                    if (Boolean.TRUE.equals(pnStreamConfigs.getEnableStreamStats())) {
                        return streamStatsService.updateStreamStats(statConfig, StreamStatsEnum.NUMBER_OF_REQUESTS, xPagopaPnCxId, streamEntity.getStreamId())
                                .thenReturn(streamEntity);
                    }
                    return Mono.just(streamEntity);
                })
                .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .flatMap(res ->
                        toEventTimelineInternalFromEventEntity(res.getEvents())
                                .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                                //timeline ancora anonimizzato - EventEntity + TimelineElementInternal
                                .collectList()
                                .map(items -> {
                                    generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).generateSuccess("timelineElementIds {}", createAuditLogOfElementsId(items)).log();
                                    return items;
                                })
                                // chiamo timelineService per aggiungere le confidentialInfo
                                .flatMapMany(items -> {
                                    if (streamUtils.getVersion(xPagopaPnApiVersion) == 10)
                                        return Flux.fromStream(items.stream());
                                    return addConfidentialInformationAtEventTimelineList(removeDuplicatedItems(items));
                                })
                                // converto l'eventTimelineInternalDTO in ProgressResponseElementV28
                                .map(this::getProgressResponseFromEventTimeline)
                                .sort(Comparator.comparing(ProgressResponseElementV28::getEventId))
                                .collectList()
                                .flatMap(eventList -> updateStreamRetryAfterAndStats(statConfig, xPagopaPnCxId, streamId, eventList).thenReturn(eventList))
                                .map(eventList -> {
                                    var retryAfter = pnStreamConfigs.getScheduleInterval().intValue();
                                    int currentRetryAfter = res.getLastEventIdRead() == null ? retryAfter : 0;
                                    var purgeDeletionWaittime = pnStreamConfigs.getPurgeDeletionWaittime();
                                    log.info("consumeEventStream lastEventId={} streamId={} size={} returnedlastEventId={} retryAfter={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty() ? eventList.get(eventList.size() - 1).getEventId() : "ND"), currentRetryAfter);
                                    // schedulo la pulizia per gli eventi precedenti a quello richiesto
                                    schedulerService.scheduleStreamEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, StreamEventType.PURGE_STREAM_OLDER_THAN);
                                    // ritorno gli eventi successivi all'evento di buffer, FILTRANDO quello con lastEventId visto che l'ho sicuramente già ritornato
                                    return ProgressResponseElementDto.builder()
                                            .retryAfter(currentRetryAfter)
                                            .progressResponseElementList(eventList)
                                            .build();
                                })
                                .doOnSuccess(progressResponseElementDto -> generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).generateSuccess("ProgressResponseElementDto size={}", progressResponseElementDto.getProgressResponseElementList().size()).log())
                                .doOnError(error -> generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).generateFailure("Error in consumeEventStream").log())
                );
    }

    private Mono<Void> updateStreamRetryAfterAndStats(CustomStatsConfig customStatsConfig, String xPagopaPnCxId, UUID streamId, List<ProgressResponseElementV28> eventList) {
        if(Boolean.TRUE.equals(pnStreamConfigs.getEnableStreamStats())) {
            if (eventList.isEmpty()) {
                return streamStatsService.updateStreamStats(customStatsConfig, StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xPagopaPnCxId, streamId.toString())
                        .flatMap(updateItemResponse -> streamEntityDao.updateStreamRetryAfter(constructNewRetryAfterEntity(xPagopaPnCxId, streamId)));
            }
            return streamStatsService.updateNumberOfReadingStreamStats(customStatsConfig, xPagopaPnCxId, streamId.toString(), eventList.size()).then();
        }
        return streamEntityDao.updateStreamRetryAfter(constructNewRetryAfterEntity(xPagopaPnCxId, streamId));
    }

    private String createAuditLogOfElementsId(List<EventTimelineInternalDto> items) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        HashMap<String, List<String>> iunWithTimelineElementId = new HashMap<>();

        items.forEach(timelineElement -> {
            List<String> elements = iunWithTimelineElementId.get(timelineElement.getTimelineElementInternal().getIun());
            String description = timelineElement.getEventEntity().getEventDescription().replace(".IUN_" + timelineElement.getTimelineElementInternal().getIun(), "");
            if (elements == null) {
                elements = new ArrayList<>(Collections.singletonList(description));
            } else {
                elements.add(description);
            }
            iunWithTimelineElementId.put(timelineElement.getTimelineElementInternal().getIun(), elements);
        });

        iunWithTimelineElementId.keySet().forEach(iun -> rootNode.put(iun, iunWithTimelineElementId.get(iun).toString()));

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            log.error(LOG_MSG_JSON_COMPRESSION, e);
            throw new PnInternalException(LOG_MSG_JSON_COMPRESSION, ERROR_CODE_PN_GENERIC_ERROR);
        }
    }

    private StreamRetryAfter constructNewRetryAfterEntity(String xPagopaPnCxId, UUID streamId) {
        StreamRetryAfter retryAfterEntity = new StreamRetryAfter();
        retryAfterEntity.setPaId(xPagopaPnCxId);
        retryAfterEntity.setStreamId(streamId.toString());
        retryAfterEntity.setRetryAfter(streamUtils.retrieveRetryAfter(xPagopaPnCxId));
        return retryAfterEntity;
    }

    private ProgressResponseElementV28 getProgressResponseFromEventTimeline(EventTimelineInternalDto eventTimeline) {
        var response = ProgressResponseElementMapper.internalToExternal(eventTimeline.getEventEntity());
        if (StringUtils.hasText(eventTimeline.getEventEntity().getElement())) {
            TimelineElementV27 timelineElement = TimelineElementStreamMapper.internalToExternal(eventTimeline.getTimelineElementInternal());
            response.setElement(timelineElement);
        }
        return response;
    }

    private Flux<EventTimelineInternalDto> toEventTimelineInternalFromEventEntity(List<EventEntity> events) throws PnInternalException {
        return Flux.fromStream(events.stream())
                .map(item -> {
                    TimelineElementInternal timelineElementInternal = getTimelineInternalFromEventEntity(item);
                    return EventTimelineInternalDto.builder()
                            .eventEntity(item)
                            .timelineElementInternal(timelineElementInternal)
                            .build();
                });
    }

    private TimelineElementInternal getTimelineInternalFromEventEntity(EventEntity entity) throws PnInternalException {
        if (StringUtils.hasText(entity.getElement())) {
            return streamUtils.getTimelineInternalFromEvent(entity);
        }
        return null;
    }

    @Override
    public Mono<Void> saveEvent(TimelineElementInternal timelineElementInternal) {
        log.info("Received timeline element: {}", timelineElementInternal.getTimelineElementId());
        return streamEntityDao.findByPa(timelineElementInternal.getPaId())
                .filter(entity -> entity.getDisabledDate() == null && !entity.getStreamId().startsWith(RETRY_PREFIX))
                .collectList()
                .flatMap(stream -> {
                    if (stream.isEmpty()) {
                        return Mono.empty();
                    } else {
                        return getNotification(timelineElementInternal.getIun())
                                .map(notification -> Tuples.of(stream, timelineElementInternal, notification));
                    }
                })
                .flatMapMany(res -> Flux.fromIterable(res.getT1())
                        .flatMap(stream -> processEvent(stream, res.getT2(), res.getT3().getGroup()))
                        .flatMap(stream -> checkEventToSort(stream, res.getT2()), pnStreamConfigs.getSaveEventMaxConcurrency())
                        .flatMap(stream -> saveEventWithAtomicIncrement(stream, res.getT2().getStatusInfo().getActual() ,res.getT2()), pnStreamConfigs.getSaveEventMaxConcurrency()))
                .collectList()
                .doOnNext(events -> log.info("Saved event: [{}] on {} streams", timelineElementInternal.getTimelineElementId(), events.size()))
                .then();
    }

    private Mono<StreamEntity> checkEventToSort(StreamEntity streamEntity, TimelineElementInternal timelineElement) {
        log.debug("sortStream streamId={} timelineElementId={} category={}", streamEntity.getStreamId(), timelineElement.getTimelineElementId(), timelineElement.getCategory());

        if (Objects.isNull(streamEntity.getSorting()) || Boolean.FALSE.equals(streamEntity.getSorting())) {
           return Mono.just(streamEntity);
        }

        if (Arrays.stream(TimelineElementCategoryInt.SkipSortCategory.values()).anyMatch(category -> category.name().equals(timelineElement.getCategory()))) {
            log.debug("Event {} in validation, ignoring sorting flow for stream with id={}", timelineElement.getTimelineElementId(), streamEntity.getStreamId());
            return Mono.just(streamEntity);
        }

        return manageUnlockEvent(streamEntity, timelineElement);
    }

    private Mono<StreamEntity> manageUnlockEvent(StreamEntity stream, TimelineElementInternal timelineElement) {
        if (Arrays.stream(TimelineElementCategoryInt.UnlockTimelineElementCategory.values()).anyMatch(category -> category.name().equals(timelineElement.getCategory()))) {
            log.info("Event with id={} is an unlock event, saving unlock item and sending message UNLOCK_EVENTS", timelineElement.getTimelineElementId());
            NotificationUnlockedEntity notificationUnlockedEntity = streamUtils.buildNotificationUnlockedEntity(stream.getStreamId(), timelineElement.getIun(), timelineElement.getNotificationSentAt());
            return notificationUnlockedEntityDao.putItem(notificationUnlockedEntity)
                    .map(entity -> schedulerService.scheduleSortEvent(stream.getStreamId() + "_" + timelineElement.getIun(), null, 0, SortEventType.UNLOCK_EVENTS))
                    .map(eventKey -> stream);
        } else {
            if (streamUtils.checkIfTtlIsExpired(timelineElement.getNotificationSentAt())) {
                log.info("Unlock Event ttl is expired, skipping quarantine for [{}]", timelineElement.getTimelineElementId());
                return Mono.just(stream);
            }

            if (timelineElement.getNotificationSentAt().isBefore(stream.getActivationDate())) {
                log.info("Stream activation date is after notificationSentAt, skipping quarantine for [{}]", timelineElement.getTimelineElementId());
                return Mono.just(stream);
            }

            return notificationUnlockedEntityDao.findByPk(stream.getStreamId() + "_" + timelineElement.getIun())
                    .switchIfEmpty(Mono.defer(() -> {
                        log.info("Unlock event not found for eventId [{}], saving in quarantine", timelineElement.getTimelineElementId());
                        return eventsQuarantineEntityDao.putItem(streamUtils.buildEventQuarantineEntity(stream, timelineElement))
                                .then(Mono.empty());
                    }))
                    .map(notificationUnlockedEntity -> stream);
        }
    }



    public Mono<StreamNotificationEntity> getNotification(String iun) {
        return streamNotificationDao.findByIun(iun)
                .doOnNext(entity -> log.info("founded notification on dynamo for iun={}", iun))
                .switchIfEmpty(Mono.defer(() -> pnDeliveryClientReactive.getSentNotification(iun))
                        .flatMap(this::constructAndSaveNotificationEntity));
    }

    private Mono<StreamNotificationEntity> constructAndSaveNotificationEntity(SentNotificationV24 sentNotificationV24) {
        StreamNotificationEntity streamNotificationEntity = new StreamNotificationEntity();
        streamNotificationEntity.setHashKey(sentNotificationV24.getIun());
        streamNotificationEntity.setGroup(sentNotificationV24.getGroup());
        streamNotificationEntity.setTtl(Instant.now().plusSeconds(pnStreamConfigs.getStreamNotificationTtl()).toEpochMilli());
        streamNotificationEntity.setCreationDate(sentNotificationV24.getSentAt());
        return streamNotificationDao.putItem(streamNotificationEntity)
                .doOnNext(entity -> log.info("saved notification on dynamo for iun={}", sentNotificationV24.getIun()));
    }

    private Mono<StreamEntity> processEvent(StreamEntity stream, TimelineElementInternal timelineElementInternal, String groups) {

        if (!CollectionUtils.isEmpty(stream.getGroups()) && !checkGroups(Collections.singletonList(groups), stream.getGroups())) {
            log.info("skipping saving webhook event for stream={} because stream groups are different", stream.getStreamId());
            return Mono.empty();
        }
        if (!StringUtils.hasText(stream.getEventType())) {
            log.warn("skipping saving because webhook stream configuration is not correct stream={}", stream);
            return Mono.empty();
        }

        StreamCreationRequestV28.EventTypeEnum eventType = StreamCreationRequestV28.EventTypeEnum.fromValue(stream.getEventType());
        if (eventType == StreamCreationRequestV28.EventTypeEnum.STATUS && !timelineElementInternal.getStatusInfo().isStatusChanged()) {
            log.info("skipping saving webhook event for stream={} because there was no change in status iun={}", stream.getStreamId(), timelineElementInternal.getIun());
            return Mono.empty();
        }

        String timelineEventCategory = timelineElementInternal.getCategory();
        if (isDiagnosticElement(timelineElementInternal.getCategory())) {
            log.info("skipping saving webhook event for stream={} because category={} is only diagnostic", stream.getStreamId(), timelineEventCategory);
            return Mono.empty();
        }

        Set<String> filteredValues = retrieveFilteredValues(stream, eventType);

        if ((eventType == StreamCreationRequestV28.EventTypeEnum.STATUS && filteredValues.contains(timelineElementInternal.getStatusInfo().getActual()))
                || (eventType == StreamCreationRequestV28.EventTypeEnum.TIMELINE && filteredValues.contains(timelineEventCategory))) {
            return Mono.just(stream);
        } else {
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, timelineElementInternal.getIun());
        }
        return Mono.empty();
    }

    private Set<String> retrieveFilteredValues(StreamEntity stream, StreamCreationRequestV28.EventTypeEnum eventType) {
        if (eventType == StreamCreationRequestV28.EventTypeEnum.TIMELINE) {
            return categoriesByFilter(stream);
        } else if (eventType == StreamCreationRequestV28.EventTypeEnum.STATUS) {
            return statusByFilter(stream);
        }
        return Collections.emptySet();
    }

    private boolean isDiagnosticElement(String timelineEventCategory) {
        try {
            TimelineElementCategoryInt.DiagnosticTimelineElementCategory.valueOf(timelineEventCategory);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Mono<EventEntity> saveEventWithAtomicIncrement(StreamEntity streamEntity, String newStatus,
                                                    TimelineElementInternal timelineElementInternal) {
        return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
                .flatMap(atomicCounterUpdated -> {
                    if (atomicCounterUpdated < 0) {
                        log.warn("updateAndGetAtomicCounter counter is -1, skipping saving stream");
                        return Mono.empty();
                    }

                    EventEntity eventEntity = streamUtils.buildEventEntity(atomicCounterUpdated, streamEntity, newStatus, timelineElementInternal);

                    return eventEntityDao.save(eventEntity)
                            .doOnNext(entity -> log.debug("saved event for stream: [{}] and timelineElementId: [{}]", streamEntity.getStreamId(), timelineElementInternal.getTimelineElementId()))
                            .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)));
                });
    }

    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan) {
        log.info("purgeEvents streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return eventEntityDao.delete(streamId, eventId, olderThan)
                .map(thereAreMore -> {
                    if (Boolean.TRUE.equals(thereAreMore)) {
                        var purgeDeletionWaittime = pnStreamConfigs.getPurgeDeletionWaittime();
                        log.debug("purgeEvents streamId={} eventId={} olderThan={} there are more event to purge", streamId, eventId, olderThan);
                        schedulerService.scheduleStreamEvent(streamId, eventId, purgeDeletionWaittime, olderThan ? StreamEventType.PURGE_STREAM_OLDER_THAN : StreamEventType.PURGE_STREAM);
                    } else
                        log.debug("purgeEvents streamId={} eventId={} olderThan={} no more event to purge", streamId, eventId, olderThan);

                    return thereAreMore;
                })
                .then();
    }

    private Set<String> categoriesByVersion(int version) {
        return Arrays.stream(TimelineElementCategoryInt.values())
                .filter(e -> e.getVersion() <= TimelineElementCategoryInt.StreamVersions.fromIntValue(version).getTimelineVersion())
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

    private Set<String> statusByVersion(int version) {
        return Arrays.stream(NotificationStatusInt.values())
                .filter(e -> e.getVersion() <= TimelineElementCategoryInt.StreamVersions.fromIntValue(version).getStatusVersion())
                .map(NotificationStatusInt::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> categoriesByFilter(StreamEntity stream) {
        Set<String> versionedCategoriesSet = categoriesByVersion(streamUtils.getVersion(stream.getVersion()));

        if (CollectionUtils.isEmpty(stream.getFilterValues())) {
            return versionedCategoriesSet;
        }

        Set<String> categoriesSet = stream.getFilterValues().stream()
                .filter(v -> !v.equalsIgnoreCase(DEFAULT_CATEGORIES))
                .collect(Collectors.toSet());

        if (stream.getFilterValues().contains(DEFAULT_CATEGORIES)) {
            log.debug("pnDeliveryPushConfigs.getListCategoriesPa[0]={}", pnStreamConfigs.getListCategoriesPa().get(0));
            categoriesSet.addAll(pnStreamConfigs.getListCategoriesPa());
        }

        return categoriesSet.stream()
                .filter(versionedCategoriesSet::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> statusByFilter(StreamEntity stream) {
        Set<String> versionedStatusSet = statusByVersion(streamUtils.getVersion(stream.getVersion()));
        if (CollectionUtils.isEmpty(stream.getFilterValues())) {
            return versionedStatusSet;
        }
        return stream.getFilterValues().stream()
                .filter(versionedStatusSet::contains) // Qualsiasi stato non appartenente alla versione di riferimento dello stream viene scartato
                .collect(Collectors.toSet());
    }

    private List<EventTimelineInternalDto> removeDuplicatedItems(List<EventTimelineInternalDto> eventEntities) {
        return new ArrayList<>(eventEntities.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getTimelineElementInternal().getTimelineElementId(),
                        dto -> dto,
                        (existing, replacement) -> existing
                )).values());
    }

    protected Flux<EventTimelineInternalDto> addConfidentialInformationAtEventTimelineList(List<EventTimelineInternalDto> eventEntities) {
        List<TimelineElementInternal> timelineElementInternals = eventEntities.stream()
                .map(EventTimelineInternalDto::getTimelineElementInternal)
                .filter(Objects::nonNull)
                .toList();

        return confidentialInformationService.getTimelineConfidentialInformation(timelineElementInternals)
                .map(confidentialInfo -> timelineElementInternals.stream()
                        .filter(i -> i.getTimelineElementId().equals(confidentialInfo.getTimelineElementId()))
                        .findFirst()
                        .map(timelineElementInternal -> {
                            timelineElementInternal.setDetails(timelineService.enrichTimelineElementWithConfidentialInformation(timelineElementInternal.getCategory(), timelineElementInternal.getDetails(), confidentialInfo));
                            return timelineElementInternal;
                        })
                        .orElse(null)
                )
                .collectList()
                .flatMapMany(item -> Flux.fromStream(eventEntities.stream()));
    }
}