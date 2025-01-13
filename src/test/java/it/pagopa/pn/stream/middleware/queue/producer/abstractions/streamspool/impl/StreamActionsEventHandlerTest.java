package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.StreamEventsService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

class StreamActionsEventHandlerTest {

    @Mock
    private StreamEventsService webhookService;

    private StreamActionsEventHandler handler;

    @BeforeEach
    public void setup() {
        webhookService = Mockito.mock(StreamEventsService.class);
        handler = new StreamActionsEventHandler(webhookService);
    }

    @Test
    void handleEventRegister() {
        // GIVEN
        StreamAction action = buildWebhookAction();
        Mockito.when(webhookService.saveEvent(Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        // WHEN
        handler.handleEvent(action);

        // THEN
        Mockito.verify(webhookService, Mockito.times(1))
                .saveEvent(action.getTimelineElementInternal(), action.getType());
    }


    @Test
    void handleEventPurge() {
        // GIVEN
        StreamAction action = buildWebhookActionPurge();
        Mockito.when(webhookService.purgeEvents(Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mono.empty());

        // WHEN
        handler.handleEvent(action);

        // THEN
        Mockito.verify(webhookService, Mockito.times(1))
                .purgeEvents(action.getStreamId(), action.getEventId(), false);
    }

    private StreamAction buildWebhookAction() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return StreamAction.builder()
                .type(StreamEventType.REGISTER_EVENT)
                .eventId("002")
                .iun("003")
                .build();
    }


    private StreamAction buildWebhookActionPurge() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");

        return StreamAction.builder()
                .type(StreamEventType.PURGE_STREAM)
                .streamId("001")
                .eventId("002")
                .iun("003")
                .build();
    }
}