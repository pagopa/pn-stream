package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventsQuarantineEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.StreamScheduleService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Service
@Slf4j
public class StreamScheduleServiceImpl extends PnStreamServiceImpl implements StreamScheduleService {

    private final EventsQuarantineEntityDao eventsQuarantineEntityDao;
    private final SchedulerService schedulerService;
    private final StreamUtils streamUtils;
    private final EntityToDtoWebhookTimelineMapper entityToDtoWebhookTimelineMapper;
    private final WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    public StreamScheduleServiceImpl(StreamEntityDao streamEntityDao, PnStreamConfigs pnStreamConfigs,
            EventsQuarantineEntityDao eventsQuarantineEntityDao, SchedulerService schedulerService,
            StreamUtils streamUtils, EntityToDtoWebhookTimelineMapper entityToDtoWebhookTimelineMapper,
            WebhookTimelineElementJsonConverter timelineElementJsonConverter) {
        super(streamEntityDao, pnStreamConfigs);
        this.eventsQuarantineEntityDao = eventsQuarantineEntityDao;
        this.schedulerService = schedulerService;
        this.streamUtils = streamUtils;
        this.entityToDtoWebhookTimelineMapper = entityToDtoWebhookTimelineMapper;
        this.timelineElementJsonConverter = timelineElementJsonConverter;
    }

    @Override
    public Mono<Void> unlockEvents(SortEventAction event, boolean resendMessage) {
        log.info("Message received with event={} and resendMessage={}", event, resendMessage);

        checkInitalValues(event);
        if (event.getWrittenCounter() > pnStreamConfigs.getMaxWrittenCounter()) {
            log.warn("Reached limit of retries for eventKey={}", event.getEventKey());
            return Mono.empty();
        }

        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();

        return callToUnlockEvents(event, resendMessage, lastEvaluateKey);
    }

    private Mono<Void> callToUnlockEvents(SortEventAction event, boolean resendMessage, Map<String, AttributeValue> lastEvaluateKey) {
        return eventsQuarantineEntityDao.findByPk(event.getEventKey(), lastEvaluateKey, pnStreamConfigs.getQueryEventQuarantineLimit())
                .flatMap(quarantinedEventsList -> {
                    if (CollectionUtils.isEmpty(quarantinedEventsList.items())) {
                        return Mono.empty();
                    }

                    return Flux.fromStream(quarantinedEventsList.items().stream())
                            .flatMap(quarantinedEvent -> {
                                final TimelineElementInternal timelineElementInternal;
                                try {
                                    timelineElementInternal = entityToDtoWebhookTimelineMapper.entityToDto(timelineElementJsonConverter.jsonToEntity(quarantinedEvent.getEvent()));
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                                StreamEntity streamEntity = getStreamEntity(event, timelineElementInternal);
                                timelineElementInternal.setBusinessTimestamp(timelineElementInternal.getTimestamp());
                                timelineElementInternal.setTimestamp(timelineElementInternal.getIngestionTimestamp());
                                return streamEntityDao.updateAndGetAtomicCounter(streamEntity)
                                        .flatMap(counter -> {
                                            EventEntity eventEntity = streamUtils.buildEventEntity(counter, streamEntity, timelineElementInternal.getStatusInfo().getActual(), timelineElementInternal);
                                            return eventsQuarantineEntityDao.saveAndClearElement(quarantinedEvent, eventEntity).then();
                                        });
                            })
                            .then();
                })
                .doOnSuccess(e -> {
                    if (!CollectionUtils.isEmpty(lastEvaluateKey)) {
                        log.info("Recursive call to findByPk to get all remaining items, lastEvaluateKey={}", lastEvaluateKey);
                        callToUnlockEvents(event, resendMessage, lastEvaluateKey);
                    }
                })
                .doOnSuccess(e -> {
                    if(resendMessage) {
                        log.info("Resend message for eventKey={}", event.getEventKey());
                        computeNewValues(event);
                        schedulerService.scheduleSortEvent(event.getEventKey(), event.getDelaySeconds(), event.getWrittenCounter(), SortEventType.UNLOCK_EVENTS);
                    }
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

    private void computeNewValues(SortEventAction event) {
        event.setWrittenCounter(event.getWrittenCounter() + 1);
        event.setDelaySeconds(event.getDelaySeconds() * 2);
    }

    private StreamEntity getStreamEntity(SortEventAction event, TimelineElementInternal timelineElementInternal) {
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(event.getEventKey().split("_")[0]);
        streamEntity.setPaId(timelineElementInternal.getPaId());
        return streamEntity;
    }
}