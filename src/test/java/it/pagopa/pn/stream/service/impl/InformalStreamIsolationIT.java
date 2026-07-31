package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV29;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV29;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventEntityBatch;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamNotificationDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamNotificationEntity;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.stream.middleware.queue.consumer.PnEventInboundService;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.SortEvent;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamEvent;
import it.pagopa.pn.stream.service.StreamEventsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for TIMELINE_INFORMAL stream isolation.
 *
 * Validates that:
 * 1. Standard events (communicationType=null) are routed only to TIMELINE streams.
 * 2. Informal events (communicationType=INFORMAL) are routed only to TIMELINE_INFORMAL streams.
 * 3. No cross-contamination occurs when both stream types coexist for the same PA.
 *
 * Infrastructure: real DynamoDB via LocalStack (Testcontainers).
 * External dependencies (HTTP clients, SQS producers): mocked.
 */
@EnableAutoConfiguration
class InformalStreamIsolationIT extends BaseTest.WithLocalStack {

    private static final String PA_ID = "e2e-pa-" + UUID.randomUUID();
    private static final String IUN_STANDARD = "IUN-STD-E2E-001";
    private static final String IUN_INFORMAL = "IUN-INF-E2E-001";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    StreamEventsService streamEventsService;

    @Autowired
    StreamEntityDao streamEntityDao;

    @Autowired
    EventEntityDao eventEntityDao;

    @Autowired
    StreamNotificationDao streamNotificationDao;

    // External HTTP client — mocked to avoid network calls
    @MockBean
    PnDeliveryClientReactive pnDeliveryClientReactive;

    // SQS message producers — mocked; the service calls them for purge/sort scheduling,
    // but those side-effects are out of scope for this isolation test.
    @MockBean(name = "sortEventsActionProducer")
    MomProducer<SortEvent> sortEventsActionProducer;

    @MockBean(name = "streamActionsEventProducer")
    MomProducer<StreamEvent> streamActionsEventProducer;

    @MockBean
    PnEventInboundService pnEventInboundService;

    private StreamEntity standardStream;
    private StreamEntity informalStream;

    @BeforeEach
    void setUp() {
        // Create two streams for the same PA: one standard TIMELINE, one TIMELINE_INFORMAL
        standardStream = buildStreamEntity(PA_ID, StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        informalStream = buildStreamEntity(PA_ID, StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());

        streamEntityDao.save(standardStream).block(TIMEOUT);
        streamEntityDao.save(informalStream).block(TIMEOUT);

        // Pre-populate the StreamNotification cache so saveEvent does not fall back
        // to the external PnDeliveryClientReactive.getSentNotification() HTTP call.
        saveNotification(IUN_STANDARD);
        saveNotification(IUN_INFORMAL);
    }

    // ----------------------------------------------------------------------------------
    // Test 1: standard event → only TIMELINE stream receives it
    // ----------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E: standard event (no communicationType) is routed only to TIMELINE stream")
    void standardEvent_routedToTimelineStreamOnly() {
        TimelineElementInternal standardEvent = buildStandardEvent(IUN_STANDARD);

        streamEventsService.saveEvent(standardEvent).block(TIMEOUT);

        // Standard stream must have received the event
        EventEntityBatch standardEvents = eventEntityDao
                .findByStreamId(standardStream.getStreamId(), null).block(TIMEOUT);
        assertThat(standardEvents).isNotNull();
        assertThat(standardEvents.getEvents()).isNotEmpty();

        // Informal stream must NOT have received any event
        EventEntityBatch informalEvents = eventEntityDao
                .findByStreamId(informalStream.getStreamId(), null).block(TIMEOUT);
        assertThat(informalEvents).isNotNull();
        assertThat(informalEvents.getEvents()).isEmpty();
    }

    // ----------------------------------------------------------------------------------
    // Test 2: informal event → only TIMELINE_INFORMAL stream receives it
    // ----------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E: informal event (communicationType=INFORMAL) is routed only to TIMELINE_INFORMAL stream")
    void informalEvent_routedToInformalStreamOnly() {
        TimelineElementInternal informalEvent = buildInformalEvent(IUN_INFORMAL);

        streamEventsService.saveEvent(informalEvent).block(TIMEOUT);

        // Informal stream must have received the event
        EventEntityBatch informalEvents = eventEntityDao
                .findByStreamId(informalStream.getStreamId(), null).block(TIMEOUT);
        assertThat(informalEvents).isNotNull();
        assertThat(informalEvents.getEvents()).isNotEmpty();

        // Standard stream must NOT have received any event
        EventEntityBatch standardEvents = eventEntityDao
                .findByStreamId(standardStream.getStreamId(), null).block(TIMEOUT);
        assertThat(standardEvents).isNotNull();
        assertThat(standardEvents.getEvents()).isEmpty();
    }

    // ----------------------------------------------------------------------------------
    // Test 3: both event types coexist — perfect isolation in both directions
    // ----------------------------------------------------------------------------------
    @Test
    @DisplayName("E2E: standard and informal events coexist without cross-contamination")
    void mixedEvents_perfectIsolationBetweenStreams() {
        TimelineElementInternal standardEvent = buildStandardEvent(IUN_STANDARD);
        TimelineElementInternal informalEvent = buildInformalEvent(IUN_INFORMAL);

        streamEventsService.saveEvent(standardEvent).block(TIMEOUT);
        streamEventsService.saveEvent(informalEvent).block(TIMEOUT);

        EventEntityBatch standardStreamEvents = eventEntityDao
                .findByStreamId(standardStream.getStreamId(), null).block(TIMEOUT);
        EventEntityBatch informalStreamEvents = eventEntityDao
                .findByStreamId(informalStream.getStreamId(), null).block(TIMEOUT);

        assertThat(standardStreamEvents).isNotNull();
        assertThat(informalStreamEvents).isNotNull();

        // Each stream received exactly one event — no cross-contamination
        assertThat(standardStreamEvents.getEvents()).hasSize(1);
        assertThat(informalStreamEvents.getEvents()).hasSize(1);
    }

    // ----------------------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------------------

    private StreamEntity buildStreamEntity(String paId, String eventType) {
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setPaId(paId);
        entity.setEventType(eventType);
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now().minusSeconds(3600));
        entity.setSorting(false);
        entity.setVersion("V29");
        entity.setTitle("E2E test stream - " + eventType);
        return entity;
    }

    private void saveNotification(String iun) {
        StreamNotificationEntity notification = new StreamNotificationEntity();
        notification.setHashKey(iun);
        notification.setCreationDate(Instant.now().minusSeconds(3600));
        streamNotificationDao.putItem(notification).block(TIMEOUT);
    }

    /**
     * Standard timeline event: communicationType is null (absent), statusChanged=true
     * so it passes the STATUS stream guard. Category is NOTIFICATION_VIEWED which is
     * in the default PA category list and not a diagnostic element.
     */
    private TimelineElementInternal buildStandardEvent(String iun) {
        return TimelineElementInternal.builder()
                .iun(iun)
                .timelineElementId(iun + "_NOTIFICATION_VIEWED")
                .category("NOTIFICATION_VIEWED")
                .communicationType(null)
                .paId(PA_ID)
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now().minusSeconds(3600))
                .statusInfo(StatusInfoInternal.builder()
                        .actual("VIEWED")
                        .statusChanged(true)
                        .build())
                .build();
    }

    /**
     * Informal timeline event: communicationType=INFORMAL, statusInfo may be null
     * (informal events from delivery-push may not carry a notification status change).
     */
    private TimelineElementInternal buildInformalEvent(String iun) {
        return TimelineElementInternal.builder()
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .category("INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .paId(PA_ID)
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now().minusSeconds(3600))
                .statusInfo(null)
                .build();
    }
}
