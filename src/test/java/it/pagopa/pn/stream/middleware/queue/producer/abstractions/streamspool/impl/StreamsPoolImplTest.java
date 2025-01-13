package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;

class StreamsPoolImplTest {

    private MomProducer<StreamEvent> actionsQueue;

    private Clock clock;

    private StreamsPoolImpl web;

    @BeforeEach
    public void setup() {
        actionsQueue = Mockito.mock(MomProducer.class);
        clock = Mockito.mock(Clock.class);
        web = new StreamsPoolImpl(actionsQueue, clock);
    }

    @Test
    void scheduleFutureAction() {

        String uuid = UUID.randomUUID().toString();

        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(clock.instant()).thenReturn(instant);

        web.scheduleFutureAction(buildWebhookAction());

        Mockito.verify(actionsQueue).push(Mockito.argThat(matches((StreamEvent tmp) -> tmp.getHeader().getIun().equalsIgnoreCase("004"))));
        
    }

    private StreamAction buildWebhookAction() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        return StreamAction.builder()
                .streamId("001")
                .eventId("002")
                .iun("004")
                .delay(5)
                .type(StreamEventType.REGISTER_EVENT)
                .build();
    }

    private static <T> ArgumentMatcher<T> matches(Predicate<T> predicate) {
        return new ArgumentMatcher<T>() {

            @SuppressWarnings("unchecked")
            @Override
            public boolean matches(Object argument) {
                return predicate.test((T) argument);
            }
        };
    }
}