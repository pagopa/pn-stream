package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.stream.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.stream.ProgressResponseElementDto;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.TimelineElementV25;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV25;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV25;
import it.pagopa.pn.stream.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.TimelineService;
import it.pagopa.pn.stream.service.StreamEventsService;
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

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static it.pagopa.pn.stream.service.utils.StreamUtils.checkGroups;


@Service
@Slf4j
public class StreamEventsServiceImpl extends StreamServiceImpl implements StreamEventsService {
    private final EventEntityDao eventEntityDao;
    private final SchedulerService schedulerService;
    private final StreamUtils streamUtils;
    private final TimelineService timelineService;
    private final ConfidentialInformationService confidentialInformationService;
    private final Set<String> defaultNotificationStatuses;
    private static final String DEFAULT_CATEGORIES = "DEFAULT";


    public StreamEventsServiceImpl(StreamEntityDao streamEntityDao, EventEntityDao eventEntityDao,
                                   SchedulerService schedulerService, StreamUtils streamUtils,
                                   PnStreamConfigs pnStreamConfigs, TimelineService timeLineService,
                                   ConfidentialInformationService confidentialInformationService) {
        super(streamEntityDao, pnStreamConfigs);
        this.eventEntityDao = eventEntityDao;
        this.schedulerService = schedulerService;
        this.streamUtils = streamUtils;
        this.timelineService = timeLineService;
        this.confidentialInformationService = confidentialInformationService;
        this.defaultNotificationStatuses = statusByVersion(NotificationStatusInt.VERSION_10);
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
        // grazie al contatore atomico usato in scrittura per generare l'eventId, non serve più gestire la finestra.
        return getStreamEntityToWrite(apiVersion(xPagopaPnApiVersion), xPagopaPnCxId, xPagopaPnCxGroups, streamId)
            .doOnError(error -> generateAuditLog(PnAuditLogEventType.AUD_WH_CONSUME, msg, args).generateFailure("Error in reading stream").log())
            .switchIfEmpty(Mono.error(new PnStreamForbiddenException("Cannot consume stream")))
            .flatMap(stream -> eventEntityDao.findByStreamId(stream.getStreamId(), lastEventId))
                .flatMap(res ->
                    toEventTimelineInternalFromEventEntity(res.getEvents())
                            .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                            //timeline ancora anonimizzato - EventEntity + TimelineElementInternal
                            .collectList()
                            // chiamo timelineService per aggiungere le confidentialInfo
                            .flatMapMany(items -> {
                                if (streamUtils.getVersion(xPagopaPnApiVersion) == 10)
                                    return Flux.fromStream(items.stream());
                                return addConfidentialInformationAtEventTimelineList(removeDuplicatedItems(items));
                            })
                            // converto l'eventTimelineInternalDTO in ProgressResponseElementV25
                            .map(this::getProgressResponseFromEventTimeline)
                            .sort(Comparator.comparing(ProgressResponseElementV25::getEventId))
                            .collectList()
                            .map(eventList -> {
                                var retryAfter = pnStreamConfigs.getWebhook().getScheduleInterval().intValue();
                                int currentRetryAfter = res.getLastEventIdRead() == null ? retryAfter : 0;
                                var purgeDeletionWaittime = pnStreamConfigs.getWebhook().getPurgeDeletionWaittime();
                                log.info("consumeEventStream lastEventId={} streamId={} size={} returnedlastEventId={} retryAfter={}", lastEventId, streamId, eventList.size(), (!eventList.isEmpty()?eventList.get(eventList.size()-1).getEventId():"ND"), currentRetryAfter);
                                // schedulo la pulizia per gli eventi precedenti a quello richiesto
                                schedulerService.scheduleWebhookEvent(res.getStreamId(), lastEventId, purgeDeletionWaittime, StreamEventType.PURGE_STREAM_OLDER_THAN);
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

    private ProgressResponseElementV25 getProgressResponseFromEventTimeline(EventTimelineInternalDto eventTimeline) {
        var response = ProgressResponseElementMapper.internalToExternal(eventTimeline.getEventEntity());
        if (StringUtils.hasText(eventTimeline.getEventEntity().getElement())) {
            TimelineElementV25 timelineElement = TimelineElementStreamMapper.internalToExternal(eventTimeline.getTimelineElementInternal());
            response.setElement(timelineElement);
        }
        return response;
    }

    private Flux<EventTimelineInternalDto> toEventTimelineInternalFromEventEntity(List<EventEntity> events) throws PnInternalException{
        return Flux.fromStream(events.stream())
                .map(item -> {
                    TimelineElementInternal timelineElementInternal = getTimelineInternalFromEventEntity(item);
                    return EventTimelineInternalDto.builder()
                            .eventEntity(item)
                            .timelineElementInternal(timelineElementInternal)
                            .build();
                });
    }

    private TimelineElementInternal getTimelineInternalFromEventEntity(EventEntity entity) throws PnInternalException{
        if (StringUtils.hasText(entity.getElement())) {
            return streamUtils.getTimelineInternalFromEvent(entity);
        }
        return null;
    }

    @Override
    public Mono<Void> saveEvent(TimelineElementInternal timelineElementInternal, StreamEventType streamEventType) {
        return streamEntityDao.findByPa(timelineElementInternal.getPaId())
                .filter(entity -> entity.getDisabledDate() == null)
                .collectList()
            .flatMap(l -> {
                if (l.isEmpty()) {
                    return Mono.empty();    // se non ho stream in ascolto, non c'è motivo di fare le query in dynamo
                }
                else {
                    return Mono.fromSupplier(() -> Tuples.of(l, timelineElementInternal, streamUtils.getNotification(timelineElementInternal.getIun())));
                }
            })
            .flatMapMany(res -> Flux.fromIterable(res.getT1())
                .flatMap(stream -> processEvent(stream, res.getT2(), res.getT3()))).collectList().then();
    }
    private Mono<Void> processEvent(StreamEntity stream, TimelineElementInternal timelineElementInternal, List<String> groups) {

        if (!CollectionUtils.isEmpty(stream.getGroups()) && !checkGroups(groups, stream.getGroups())){
            log.info("skipping saving webhook event for stream={} because stream groups are different", stream.getStreamId());
            return Mono.empty();
        }
        // per ogni stream configurato, devo andare a controllare se lo stato devo salvarlo o meno
        // c'è il caso in cui lo stato non cambia (e se lo stream vuolo solo i cambi di stato, lo ignoro)
        if (!StringUtils.hasText(stream.getEventType()))
        {
            log.warn("skipping saving because webhook stream configuration is not correct stream={}", stream);
            return Mono.empty();
        }

        StreamCreationRequestV25.EventTypeEnum eventType = StreamCreationRequestV25.EventTypeEnum.fromValue(stream.getEventType());
        if (eventType == StreamCreationRequestV25.EventTypeEnum.STATUS
            && !timelineElementInternal.getStatusInfo().isStatusChanged())
        {
            log.info("skipping saving webhook event for stream={} because there was no change in status iun={}", stream.getStreamId(), timelineElementInternal.getIun());
            return Mono.empty();
        }

        String timelineEventCategory = timelineElementInternal.getCategory().getValue();

        if (isDiagnosticElement(timelineEventCategory)){
            log.info("skipping saving webhook event for stream={} because category={} is only diagnostic", stream.getStreamId(), timelineEventCategory);
            return Mono.empty();
        }

        Set<String> filteredValues = new LinkedHashSet<>();
        if (eventType == StreamCreationRequestV25.EventTypeEnum.TIMELINE) {
            filteredValues = categoriesByFilter(stream);
        } else if (eventType == StreamCreationRequestV25.EventTypeEnum.STATUS){
            filteredValues = stream.getFilterValues() == null || stream.getFilterValues().isEmpty()
                ? defaultNotificationStatuses
                : stream.getFilterValues();
        }

        log.info("timelineEventCategory={} for stream={}", stream.getStreamId(), timelineEventCategory);

        // e poi c'è il caso in cui lo stream ha un filtro sugli eventi interessati
        // se è nullo/vuoto o contiene lo stato, vuol dire che devo salvarlo
        if ( (eventType == StreamCreationRequestV25.EventTypeEnum.STATUS && filteredValues.contains(timelineElementInternal.getStatusInfo().getActual()))
            || (eventType == StreamCreationRequestV25.EventTypeEnum.TIMELINE && filteredValues.contains(timelineEventCategory)))
        {
            return saveEventWithAtomicIncrement(stream, timelineElementInternal.getStatusInfo().getActual(), timelineElementInternal);
        }
        else {
            log.info("skipping saving webhook event for stream={} because timelineeventcategory is not in list timelineeventcategory={} iun={}", stream.getStreamId(), timelineEventCategory, timelineElementInternal.getIun());
        }

        return Mono.empty();
    }

    private boolean isDiagnosticElement(String timelineEventCategory) {
        try {
            TimelineElementCategoryInt.DiagnosticTimelineElementCategory.valueOf(timelineEventCategory);
            //Is diagnostic element
            return true;
        }catch (IllegalArgumentException ex){
            //is not diagnostic element
            return false;
        }
    }

    private Mono<Void> saveEventWithAtomicIncrement(StreamEntity streamEntity, String newStatus,
                                                    TimelineElementInternal timelineElementInternal) {
        // recupero un contatore aggiornato
        return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
                .flatMap(atomicCounterUpdated -> {
                    if (atomicCounterUpdated < 0) {
                        log.warn("updateAndGetAtomicCounter counter is -1, skipping saving stream");
                        return Mono.empty();
                    }

                    EventEntity eventEntity = streamUtils.buildEventEntity(atomicCounterUpdated, streamEntity, newStatus, timelineElementInternal);

                    return eventEntityDao.saveWithCondition(eventEntity)
                            .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                            .doOnSuccess(event -> log.info("saved webhookevent={}", event))
                            .then();
                });
    }

    @Override
    public Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan) {
        log.info("purgeEvents streamId={} eventId={} olderThan={}", streamId, eventId, olderThan);
        return eventEntityDao.delete(streamId, eventId, olderThan)
            .map(thereAreMore -> {
                if (Boolean.TRUE.equals(thereAreMore))
                {
                    var purgeDeletionWaittime = pnStreamConfigs.getWebhook().getPurgeDeletionWaittime();
                    log.info("purgeEvents streamId={} eventId={} olderThan={} there are more event to purge", streamId, eventId, olderThan);
                    schedulerService.scheduleWebhookEvent(streamId, eventId, purgeDeletionWaittime, olderThan? StreamEventType.PURGE_STREAM_OLDER_THAN: StreamEventType.PURGE_STREAM);
                }
                else
                    log.info("purgeEvents streamId={} eventId={} olderThan={} no more event to purge", streamId, eventId, olderThan);

                return thereAreMore;
            })
            .then();
    }

    private Set<String> categoriesByVersion(int version) {
        return Arrays.stream(TimelineElementCategoryInt.values())
            .filter( e -> e.getVersion() <= version)
            .map(TimelineElementCategoryInt::getValue)
            .collect(Collectors.toSet());
    }

    private Set<String> statusByVersion(int version) {
        return Arrays.stream(NotificationStatusInt.values())
            .filter( e -> e.getVersion() <= version)
            .map(NotificationStatusInt::getValue)
            .collect(Collectors.toSet());
    }

    private Set<String> categoriesByFilter(StreamEntity stream) {
        Set<String> categoriesSet;
        if (CollectionUtils.isEmpty(stream.getFilterValues())){
            categoriesSet = categoriesByVersion(streamUtils.getVersion(stream.getVersion()));
        } else {
            categoriesSet = stream.getFilterValues().stream()
                    .filter(v -> !v.equalsIgnoreCase(DEFAULT_CATEGORIES))
                    .collect(Collectors.toSet());
            if (stream.getFilterValues().contains(DEFAULT_CATEGORIES)) {
                log.debug("pnStreamConfigs.getListCategoriesPa[0]={}", pnStreamConfigs.getListCategoriesPa().get(0));
                categoriesSet.addAll(pnStreamConfigs.getListCategoriesPa());
            }
        }
        return categoriesSet;
    }
    private List<EventTimelineInternalDto> removeDuplicatedItems(List<EventTimelineInternalDto> eventEntities) {
        return new ArrayList<>(eventEntities.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getTimelineElementInternal().getElementId(),
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
                .map(confidentialInfo -> {
                    // cerco l'elemento in TimelineElementInternals con elementiId
                    TimelineElementInternal internal = timelineElementInternals.stream()
                            .filter(i -> i.getElementId().equals(confidentialInfo.getTimelineElementId()))
                            .findFirst()
                            .get();
                    timelineService.enrichTimelineElementWithConfidentialInformation(internal.getDetails(), confidentialInfo);
                    return internal;
                })
                .collectList()
                .flatMapMany(item -> Flux.fromStream(eventEntities.stream()));
    }
}