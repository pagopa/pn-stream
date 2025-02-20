package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventsQuarantineEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.StreamScheduleService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;


@Service
@Slf4j
public class StreamScheduleServiceImpl extends PnStreamServiceImpl implements StreamScheduleService {

    private final EventsQuarantineEntityDao eventsQuarantineEntityDao;
    private final SchedulerService schedulerService;
    private final StreamUtils streamUtils;
    public StreamScheduleServiceImpl(StreamEntityDao streamEntityDao, PnStreamConfigs pnStreamConfigs,
            EventsQuarantineEntityDao eventsQuarantineEntityDao, SchedulerService schedulerService,
            StreamUtils streamUtils) {
        super(streamEntityDao, pnStreamConfigs);
        this.eventsQuarantineEntityDao = eventsQuarantineEntityDao;
        this.schedulerService = schedulerService;
        this.streamUtils = streamUtils;
    }

    @Override
    public Mono<Void> unlockEvents(SortEventAction event, boolean resendMessage) {
        log.info("Message received with event={} and resendMessage={}", event, resendMessage);

        checkInitalValues(event);
        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();
        return callToUnlockEvents(event, lastEvaluateKey)
                .doOnNext(sortEventAction -> {
                    if (resendMessage && sortEventAction.getWrittenCounter() <= pnStreamConfigs.getMaxWrittenCounter()) {
                        log.info("Resend message for eventKey: [{}] to unlock with delay: [{}] and writtenCounter: [{}]", event.getEventKey(), event.getDelaySeconds(), event.getWrittenCounter());
                        schedulerService.scheduleSortEvent(event.getEventKey(), event.getDelaySeconds(), event.getWrittenCounter(), SortEventType.UNLOCK_EVENTS);
                    }
                })
                .then();
    }

    private Mono<SortEventAction> callToUnlockEvents(SortEventAction event, Map<String, AttributeValue> lastEvaluateKey) {
        return eventsQuarantineEntityDao.findByPk(event.getEventKey(), lastEvaluateKey, pnStreamConfigs.getQueryEventQuarantineLimit())
                .flatMap(quarantinedEventsList -> {
                    if (CollectionUtils.isEmpty(quarantinedEventsList.items())) {
                        return computeNewValues(event);
                    }
                    return saveEventAndRemoveFromQuarantine(event, quarantinedEventsList)
                            .then(Mono.defer(() -> {
                                if (!CollectionUtils.isEmpty(quarantinedEventsList.lastEvaluatedKey())) {
                                    log.info("Recursive call to findByPk to get all remaining items, lastEvaluateKey={}", quarantinedEventsList.lastEvaluatedKey());
                                    return callToUnlockEvents(event, new HashMap<>(quarantinedEventsList.lastEvaluatedKey()));
                                }
                                return Mono.just(event);
                            }));
                })
                .flatMap(e -> computeNewValues(event));

    }

    @NotNull
    private Mono<Void> saveEventAndRemoveFromQuarantine(SortEventAction event, Page<EventsQuarantineEntity> quarantinedEventsList) {
        return Flux.fromStream(quarantinedEventsList.items().stream())
                .flatMap(quarantinedEvent -> {
                    TimelineElementInternal timelineElementInternal = streamUtils.getTimelineInternalFromQuarantineAndSetTimestamp(quarantinedEvent);
                    StreamEntity streamEntity = constructStreamEntity(event, timelineElementInternal);
                    return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
                            .flatMap(atomicCounterUpdated -> {
                                if (atomicCounterUpdated < 0) {
                                    log.warn("updateAndGetAtomicCounter counter is -1, skipping saving stream");
                                    return Mono.empty();
                                }
                                EventEntity eventEntity = streamUtils.buildEventEntity(atomicCounterUpdated, streamEntity, timelineElementInternal.getStatusInfo().getActual(), timelineElementInternal);
                                return eventsQuarantineEntityDao.saveAndClearElement(quarantinedEvent, eventEntity)
                                        .onErrorResume(ex -> Mono.error(new PnInternalException("Timeline element entity not converted into JSON", ERROR_CODE_PN_GENERIC_ERROR)))
                                        .doOnNext(entity -> log.info("saved webhookevent={}", entity))
                                        .then();
                            });
                })
                .then();
    }

    private void checkInitalValues(SortEventAction event) {
        if (Objects.isNull(event.getDelaySeconds())) {
            event.setDelaySeconds(pnStreamConfigs.getSortEventDelaySeconds());
        }
        if (Objects.isNull(event.getWrittenCounter())) {
            event.setWrittenCounter(0);
        }
    }

    private Mono<SortEventAction> computeNewValues(SortEventAction event) {
        event.setWrittenCounter(event.getWrittenCounter() + 1);
        event.setDelaySeconds(event.getDelaySeconds() * 2);
        return Mono.just(event);
    }

    private StreamEntity constructStreamEntity(SortEventAction event, TimelineElementInternal timelineElementInternal) {
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(event.getStreamId());
        streamEntity.setPaId(timelineElementInternal.getPaId());
        return streamEntity;
    }
}