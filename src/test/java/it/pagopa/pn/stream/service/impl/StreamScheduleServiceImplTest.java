package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventsQuarantineEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import it.pagopa.pn.stream.middleware.dao.mapper.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamScheduleServiceImplTest {

    @InjectMocks
    private StreamScheduleServiceImpl streamScheduleService;
    @Mock
    private StreamEntityDao streamEntityDao;
    @Mock
    private PnStreamConfigs pnStreamConfigs;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private EventsQuarantineEntityDao eventsQuarantineEntityDao;
    @InjectMocks
    private StreamUtils streamUtils;
    private final EntityToDtoWebhookTimelineMapper entityToDtoWebhookTimelineMapper = new EntityToDtoWebhookTimelineMapper();
    private final DtoToEntityWebhookTimelineMapper mapperTimeline = new DtoToEntityWebhookTimelineMapper();
    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        WebhookTimelineElementJsonConverter timelineElementJsonConverter = new WebhookTimelineElementJsonConverter(objectMapper);
        when(pnStreamConfigs.getMaxWrittenCounter()).thenReturn(5);
        when(pnStreamConfigs.getTtl()).thenReturn(java.time.Duration.ofSeconds(30));
        ReflectionTestUtils.setField(streamUtils,"mapperTimeline", mapperTimeline);
        ReflectionTestUtils.setField(streamUtils,"timelineElementJsonConverter", timelineElementJsonConverter);
        ReflectionTestUtils.setField(streamScheduleService,"streamUtils", streamUtils);
        ReflectionTestUtils.setField(streamScheduleService,"entityToDtoWebhookTimelineMapper", entityToDtoWebhookTimelineMapper);
        ReflectionTestUtils.setField(streamScheduleService,"timelineElementJsonConverter", timelineElementJsonConverter);
    }

    @Test
    void noEventsToUnlock() {
        // Given
        String streamId = UUID.randomUUID().toString();
        SortEventAction event = SortEventAction.builder()
                .eventKey(streamId+"_iun")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();

        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleSortEvent(anyString(), any(), any(), any());

        // When
        Mono<Void> result = streamScheduleService.unlockEvents(event, true);
        result.block();

        // Then
        assertNotNull(result);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(event.getEventKey(), 60, 1, SortEventType.UNLOCK_EVENTS);
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).findByPk(anyString(), any(), anyInt());
        Mockito.verify(streamEntityDao, Mockito.never()).updateAndGetAtomicCounter(any());

    }

    @Test
    void successEventToUnlock() {
        // Given
        String streamId = UUID.randomUUID().toString();
        SortEventAction event = SortEventAction.builder()
                .eventKey(streamId+"_iun")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();

        EventsQuarantineEntity quarantinedEvent = new EventsQuarantineEntity();
        quarantinedEvent.setPk(streamId+"_iun");
        quarantinedEvent.setEventId(UUID.randomUUID().toString());
        quarantinedEvent.setEvent("{\"iun\":\"JLPV-MWRV-VEWT-202501-W-1\",\"timelineElementId\":\"SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0\",\"timestamp\":\"2025-01-31T16:15:34Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"category\":\"SEND_DIGITAL_DOMICILE\",\"details\":{\"deliveryDetailCode\":\"RECAG001C\",\"nextSourceAttemptsMade\":0,\"nextDigitalAddressSource\":\"PLATFORM\",\"notificationDate\":\"2025-01-31T17:40:34Z\",\"physicalAddress\":{\"foreignState\":\"ITALIA\",\"zip\":\"87100\"},\"recIndex\":0,\"registeredLetterCode\":\"97d36dd3cc1542888dced4d3e1cbdf9d\",\"responseStatus\":\"OK\",\"sendingReceipts\":[{\"id\":\"mock-d98299ff-7177-4fcb-93a2-f30281faab0c\",\"system\":\"mock-system\"}],\"sendRequestId\":\"SEND_DIGITAL_DOMICILE.IUN_HWHY-EKZD-UAPU-202501-P-1.RECINDEX_0.ATTEMPT_0\",\"sentAttemptMade\":0,\"serviceLevel\":\"REGISTERED_LETTER_890\"},\"statusInfo\":{\"actual\":\"DELIVERED\",\"statusChangeTimestamp\":\"2025-01-31T16:15:39.762656057Z\",\"statusChanged\":true},\"notificationSentAt\":\"2025-02-19T17:57:33.945285668Z\",\"ingestionTimestamp\":\"2025-01-31T16:15:39.713731463Z\",\"eventTimestamp\":\"2025-01-31T16:15:34Z\"}");

        Page<EventsQuarantineEntity> page = Page.create(List.of(quarantinedEvent));

        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleSortEvent(anyString(), any(), any(), any());

        // When
        Mono<Void> result = streamScheduleService.unlockEvents(event, true);
        result.block();

        // Then
        assertNotNull(result);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(event.getEventKey(), 60, 1, SortEventType.UNLOCK_EVENTS);
        Mockito.verify(streamEntityDao, Mockito.times(1)).updateAndGetAtomicCounter(any());
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).saveAndClearElement(any(), any());
    }

    @Test
    void successUnlockAllEvents() {
        // Given
        String streamId = UUID.randomUUID().toString();
        SortEventAction event = SortEventAction.builder()
                .eventKey(streamId+"_iun")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();

        EventsQuarantineEntity quarantinedEvent = new EventsQuarantineEntity();
        quarantinedEvent.setPk(streamId+"_iun");
        quarantinedEvent.setEventId(UUID.randomUUID().toString());
        quarantinedEvent.setEvent("{\"iun\":\"JLPV-MWRV-VEWT-202501-W-1\",\"timelineElementId\":\"SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0\",\"timestamp\":\"2025-01-31T16:15:34Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"category\":\"SEND_DIGITAL_DOMICILE\",\"details\":{\"deliveryDetailCode\":\"RECAG001C\",\"nextSourceAttemptsMade\":0,\"nextDigitalAddressSource\":\"PLATFORM\",\"notificationDate\":\"2025-01-31T17:40:34Z\",\"physicalAddress\":{\"foreignState\":\"ITALIA\",\"zip\":\"87100\"},\"recIndex\":0,\"registeredLetterCode\":\"97d36dd3cc1542888dced4d3e1cbdf9d\",\"responseStatus\":\"OK\",\"sendingReceipts\":[{\"id\":\"mock-d98299ff-7177-4fcb-93a2-f30281faab0c\",\"system\":\"mock-system\"}],\"sendRequestId\":\"SEND_DIGITAL_DOMICILE.IUN_HWHY-EKZD-UAPU-202501-P-1.RECINDEX_0.ATTEMPT_0\",\"sentAttemptMade\":0,\"serviceLevel\":\"REGISTERED_LETTER_890\"},\"statusInfo\":{\"actual\":\"DELIVERED\",\"statusChangeTimestamp\":\"2025-01-31T16:15:39.762656057Z\",\"statusChanged\":true},\"notificationSentAt\":\"2025-02-19T17:57:33.945285668Z\",\"ingestionTimestamp\":\"2025-01-31T16:15:39.713731463Z\",\"eventTimestamp\":\"2025-01-31T16:15:34Z\"}");

        Page<EventsQuarantineEntity> page = Page.create(List.of(quarantinedEvent));

        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleSortEvent(anyString(), any(), any(), any());

        // When
        Mono<Void> result = streamScheduleService.unlockEvents(event, false);
        result.block();

        // Then
        assertNotNull(result);
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleSortEvent(any(), any(), any(), any());
        Mockito.verify(streamEntityDao, Mockito.times(1)).updateAndGetAtomicCounter(any());
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).saveAndClearElement(any(), any());
    }

    @Test
    void writtenCounterExpired() {
        // Given
        String streamId = UUID.randomUUID().toString();
        SortEventAction event = SortEventAction.builder()
                .eventKey(streamId+"_iun")
                .delaySeconds(30)
                .writtenCounter(6)
                .build();

        EventsQuarantineEntity quarantinedEvent = new EventsQuarantineEntity();
        quarantinedEvent.setPk(streamId+"_iun");
        quarantinedEvent.setEventId(UUID.randomUUID().toString());
        quarantinedEvent.setEvent("{\"iun\":\"JLPV-MWRV-VEWT-202501-W-1\",\"timelineElementId\":\"SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0\",\"timestamp\":\"2025-01-31T16:15:34Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"category\":\"SEND_DIGITAL_DOMICILE\",\"details\":{\"deliveryDetailCode\":\"RECAG001C\",\"nextSourceAttemptsMade\":0,\"nextDigitalAddressSource\":\"PLATFORM\",\"notificationDate\":\"2025-01-31T17:40:34Z\",\"physicalAddress\":{\"foreignState\":\"ITALIA\",\"zip\":\"87100\"},\"recIndex\":0,\"registeredLetterCode\":\"97d36dd3cc1542888dced4d3e1cbdf9d\",\"responseStatus\":\"OK\",\"sendingReceipts\":[{\"id\":\"mock-d98299ff-7177-4fcb-93a2-f30281faab0c\",\"system\":\"mock-system\"}],\"sendRequestId\":\"SEND_DIGITAL_DOMICILE.IUN_HWHY-EKZD-UAPU-202501-P-1.RECINDEX_0.ATTEMPT_0\",\"sentAttemptMade\":0,\"serviceLevel\":\"REGISTERED_LETTER_890\"},\"statusInfo\":{\"actual\":\"DELIVERED\",\"statusChangeTimestamp\":\"2025-01-31T16:15:39.762656057Z\",\"statusChanged\":true},\"notificationSentAt\":\"2025-02-19T17:57:33.945285668Z\",\"ingestionTimestamp\":\"2025-01-31T16:15:39.713731463Z\",\"eventTimestamp\":\"2025-01-31T16:15:34Z\"}");

        Page<EventsQuarantineEntity> page = Page.create(List.of(quarantinedEvent));

        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleSortEvent(anyString(), any(), any(), any());

        // When
        Mono<Void> result = streamScheduleService.unlockEvents(event, true);
        result.block();

        // Then
        assertNotNull(result);
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleSortEvent(event.getEventKey(), event.getDelaySeconds()*2, event.getWrittenCounter() + 1, SortEventType.UNLOCK_EVENTS);
        Mockito.verify(streamEntityDao, Mockito.times(0)).updateAndGetAtomicCounter(any());
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(0)).saveAndClearElement(any(), any());
    }

}