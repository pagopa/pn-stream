package it.pagopa.pn.stream.middleware.queue.consumer.handler;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl.StreamActionsEventHandler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class ActionHandlerTest {
    @InjectMocks
    private ActionHandler actionHandler;

    @Mock
    private StreamActionsEventHandler streamActionsEventHandler;


    @Test
    void pnStreamActionConsumer() {
        //GIVEN
        Message<StreamAction> message = getWebhookActionMessage();
 
        //WHEN
        Consumer<Message<StreamAction>> consumer = actionHandler.pnStreamActionConsumer();
        consumer.accept(message);

        //THEN
        StreamAction action = message.getPayload();
        verify(streamActionsEventHandler).handleEvent(action);
    }

    @NotNull
    private static Message<StreamAction> getWebhookActionMessage() {
        return new Message<>() {
            @Override
            @NotNull
            public StreamAction getPayload() {
                return StreamAction.builder()
                        .iun("test")
                        .timelineElementInternal(new TimelineElementInternal())
                        .build();
            }

            @Override
            @NotNull
            public MessageHeaders getHeaders() {
                return new MessageHeaders(new HashMap<>());
            }
        };
    }

}