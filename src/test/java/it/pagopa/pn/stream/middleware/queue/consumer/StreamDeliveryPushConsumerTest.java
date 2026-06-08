package it.pagopa.pn.stream.middleware.queue.consumer;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamScheduleEventHandler;
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
class StreamDeliveryPushConsumerTest {

    @Mock
    private StreamScheduleEventHandler streamScheduleEventHandler;

    @InjectMocks
    private StreamDeliveryPushConsumer consumer;

    @Test
    void consumeUnlockAllEvents_ok() {
        // Arrange
        SortEventAction payload = mock(SortEventAction.class);

        Message<SortEventAction> message = MessageBuilder
                .withPayload(payload)
                .setHeader("aws_messageId", "msg-id-789")
                .setHeader("X-Amzn-Trace-Id", "trace-id-789")
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> consumer.consumeUnlockAllEvents(message));
        verify(streamScheduleEventHandler).handleUnlockAllEvents(payload);
    }

    @Test
    void consumeUnlockAllEvents_ok_withoutOptionalHeaders() {
        // Arrange
        SortEventAction payload = mock(SortEventAction.class);

        Message<SortEventAction> message = MessageBuilder
                .withPayload(payload)
                .build();

        // Act & Assert
        assertDoesNotThrow(() -> consumer.consumeUnlockAllEvents(message));
        verify(streamScheduleEventHandler).handleUnlockAllEvents(payload);
    }

    @Test
    void consumeUnlockAllEvents_handlerThrows_exceptionPropagated() {
        // Arrange
        SortEventAction payload = mock(SortEventAction.class);

        Message<SortEventAction> message = MessageBuilder
                .withPayload(payload)
                .build();

        doThrow(new RuntimeException("unlock all error"))
                .when(streamScheduleEventHandler).handleUnlockAllEvents(payload);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> consumer.consumeUnlockAllEvents(message));
        verify(streamScheduleEventHandler).handleUnlockAllEvents(payload);
    }
}