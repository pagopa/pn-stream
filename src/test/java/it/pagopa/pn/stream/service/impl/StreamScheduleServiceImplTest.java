package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventsQuarantineEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
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
    @Mock
    private StreamUtils streamUtils;

    @BeforeEach
    void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        when(pnStreamConfigs.getMaxWrittenCounter()).thenReturn(5);
        when(pnStreamConfigs.getTtl()).thenReturn(java.time.Duration.ofSeconds(30));
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

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimestamp(java.time.Instant.now());
        timelineElementInternal.setPaId("paIdTest");
        StatusInfoInternal statusInfoInternal = new StatusInfoInternal();
        statusInfoInternal.setActual("DELIVERED");
        timelineElementInternal.setStatusInfo(statusInfoInternal);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setStreamId(streamId);
        eventEntity.setEventId("1");
        eventEntity.setEventDescription("2025-01-31T16:15:34Z_SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0");

        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        when(streamUtils.buildEventEntity(anyLong(), any(), anyString(), any())).thenReturn(eventEntity);
        when(streamUtils.getTimelineInternalFromQuarantineAndSetTimestamp(any())).thenReturn(timelineElementInternal);
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

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimestamp(java.time.Instant.now());
        timelineElementInternal.setPaId("paIdTest");
        StatusInfoInternal statusInfoInternal = new StatusInfoInternal();
        statusInfoInternal.setActual("DELIVERED");
        timelineElementInternal.setStatusInfo(statusInfoInternal);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setStreamId(streamId);
        eventEntity.setEventId("1");
        eventEntity.setEventDescription("2025-01-31T16:15:34Z_SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0");


        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        when(streamUtils.buildEventEntity(anyLong(), any(), anyString(), any())).thenReturn(eventEntity);
        when(streamUtils.getTimelineInternalFromQuarantineAndSetTimestamp(any())).thenReturn(timelineElementInternal);
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
                .writtenCounter(5)
                .build();

        EventsQuarantineEntity quarantinedEvent = new EventsQuarantineEntity();
        quarantinedEvent.setPk(streamId+"_iun");
        quarantinedEvent.setEventId(UUID.randomUUID().toString());
        quarantinedEvent.setEvent("{\"iun\":\"JLPV-MWRV-VEWT-202501-W-1\",\"timelineElementId\":\"SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0\",\"timestamp\":\"2025-01-31T16:15:34Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"category\":\"SEND_DIGITAL_DOMICILE\",\"details\":{\"deliveryDetailCode\":\"RECAG001C\",\"nextSourceAttemptsMade\":0,\"nextDigitalAddressSource\":\"PLATFORM\",\"notificationDate\":\"2025-01-31T17:40:34Z\",\"physicalAddress\":{\"foreignState\":\"ITALIA\",\"zip\":\"87100\"},\"recIndex\":0,\"registeredLetterCode\":\"97d36dd3cc1542888dced4d3e1cbdf9d\",\"responseStatus\":\"OK\",\"sendingReceipts\":[{\"id\":\"mock-d98299ff-7177-4fcb-93a2-f30281faab0c\",\"system\":\"mock-system\"}],\"sendRequestId\":\"SEND_DIGITAL_DOMICILE.IUN_HWHY-EKZD-UAPU-202501-P-1.RECINDEX_0.ATTEMPT_0\",\"sentAttemptMade\":0,\"serviceLevel\":\"REGISTERED_LETTER_890\"},\"statusInfo\":{\"actual\":\"DELIVERED\",\"statusChangeTimestamp\":\"2025-01-31T16:15:39.762656057Z\",\"statusChanged\":true},\"notificationSentAt\":\"2025-02-19T17:57:33.945285668Z\",\"ingestionTimestamp\":\"2025-01-31T16:15:39.713731463Z\",\"eventTimestamp\":\"2025-01-31T16:15:34Z\"}");

        Page<EventsQuarantineEntity> page = Page.create(List.of(quarantinedEvent));

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimestamp(java.time.Instant.now());
        timelineElementInternal.setPaId("paIdTest");
        StatusInfoInternal statusInfoInternal = new StatusInfoInternal();
        statusInfoInternal.setActual("DELIVERED");
        timelineElementInternal.setStatusInfo(statusInfoInternal);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setStreamId(streamId);
        eventEntity.setEventId("1");
        eventEntity.setEventDescription("2025-01-31T16:15:34Z_SEND_DIGITAL_DOMICILE.IUN_JLPV-MWRV-VEWT-202501-W-1.RECINDEX_0");


        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(0L));
        when(eventsQuarantineEntityDao.findByPk(anyString(), any(), anyInt())).thenReturn(Mono.just(page));
        when(eventsQuarantineEntityDao.saveAndClearElement(any(), any())).thenReturn(Mono.empty());
        when(streamUtils.buildEventEntity(anyLong(), any(), anyString(), any())).thenReturn(eventEntity);
        when(streamUtils.getTimelineInternalFromQuarantineAndSetTimestamp(any())).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleSortEvent(anyString(), any(), any(), any());

        // When
        Mono<Void> result = streamScheduleService.unlockEvents(event, true);
        result.block();

        // Then
        assertNotNull(result);
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleSortEvent(any(), anyInt(), anyInt(), any());
        Mockito.verify(streamEntityDao, Mockito.times(1)).updateAndGetAtomicCounter(any());
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).saveAndClearElement(any(), any());
    }

}