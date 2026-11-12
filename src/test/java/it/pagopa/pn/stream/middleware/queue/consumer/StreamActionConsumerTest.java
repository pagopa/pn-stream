package it.pagopa.pn.stream.middleware.queue.consumer;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamActionsEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamActionConsumerTest {

    @Mock
    private StreamActionsEventHandler streamActionsEventHandler;

    @InjectMocks
    private StreamActionConsumer consumer;

    @Test
    void consume_ok_withTimelineElementInternal() {
        // Arrange
        TimelineElementInternal timeline = mock(TimelineElementInternal.class);
        when(timeline.getIun()).thenReturn("TEST_IUN");

        StreamAction payload = mock(StreamAction.class);
        when(payload.getTimelineElementInternal()).thenReturn(timeline);

        Message<StreamAction> message = MessageBuilder
                .withPayload(payload)
                .setHeader("aws_messageId", "msg-id-123")
                .setHeader("X-Amzn-Trace-Id", "trace-id-123")
                .setHeader("iun", "TEST_IUN")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> consumer.consume(message));
        verify(streamActionsEventHandler).handleEvent(payload);
    }

    @Test
    void consume_ok_withoutTimelineElementInternal_usesIunFromAction() {
        // Arrange
        StreamAction payload = mock(StreamAction.class);
        when(payload.getTimelineElementInternal()).thenReturn(null);
        when(payload.getIun()).thenReturn("FALLBACK_IUN");

        Message<StreamAction> message = MessageBuilder
                .withPayload(payload)
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> consumer.consume(message));
        verify(streamActionsEventHandler).handleEvent(payload);
    }

    @Test
    void consume_ok_withoutOptionalHeaders() {
        // Arrange
        StreamAction payload = mock(StreamAction.class);
        when(payload.getTimelineElementInternal()).thenReturn(null);
        when(payload.getIun()).thenReturn("TEST_IUN");

        // No aws_messageId, no X-Amzn-Trace-Id, no iun header
        Message<StreamAction> message = MessageBuilder
                .withPayload(payload)
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> consumer.consume(message));
        verify(streamActionsEventHandler).handleEvent(payload);
    }

    @Test
    void consume_handlerThrows_exceptionPropagated() {
        // Arrange
        StreamAction payload = mock(StreamAction.class);
        when(payload.getTimelineElementInternal()).thenReturn(null);
        when(payload.getIun()).thenReturn("TEST_IUN");

        Message<StreamAction> message = MessageBuilder
                .withPayload(payload)
                .build();

        doThrow(new RuntimeException("handler error"))
                .when(streamActionsEventHandler).handleEvent(payload);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> consumer.consume(message));
        verify(streamActionsEventHandler).handleEvent(payload);
    }
}