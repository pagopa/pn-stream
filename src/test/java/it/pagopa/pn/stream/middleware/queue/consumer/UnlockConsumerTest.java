package it.pagopa.pn.stream.middleware.queue.consumer;

import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamScheduleEventHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnlockConsumerTest {

    @Mock
    private StreamScheduleEventHandler streamScheduleEventHandler;

    @InjectMocks
    private UnlockConsumer unlockConsumer;

    @Test
    void consumeUnlockShouldThrowWhenEventTypeHeaderIsMissing() {
        Message<SortEventAction> message = MessageBuilder.withPayload(mock(SortEventAction.class)).build();

        assertThrows(PnStreamException.class, () -> unlockConsumer.consumeUnlock(message));
    }

    @Test
    void consumeUnlockShouldDelegateToUnlockEvents() {
        SortEventAction payload = mock(SortEventAction.class);
        Message<SortEventAction> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", SortEventType.UNLOCK_EVENTS)
                .build();

        unlockConsumer.consumeUnlock(message);

        verify(streamScheduleEventHandler).handleUnlockEvents(payload);
        verifyNoMoreInteractions(streamScheduleEventHandler);
    }

    @Test
    void consumeUnlockShouldDelegateToUnlockAllEvents() {
        SortEventAction payload = mock(SortEventAction.class);
        Message<SortEventAction> message = MessageBuilder.withPayload(payload)
                .setHeader("eventType", SortEventType.UNLOCK_ALL_EVENTS)
                .build();

        unlockConsumer.consumeUnlock(message);

        verify(streamScheduleEventHandler).handleUnlockAllEvents(payload);
        verifyNoMoreInteractions(streamScheduleEventHandler);
    }
}