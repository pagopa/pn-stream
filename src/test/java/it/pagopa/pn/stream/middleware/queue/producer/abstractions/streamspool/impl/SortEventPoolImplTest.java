package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.api.dto.events.MomProducer;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Predicate;

class SortEventPoolImplTest {

    private MomProducer<SortEvent> sortActionsQueue;

    private Clock clock;

    private SortEventPoolImpl sortEventPool;

    @BeforeEach
    public void setup() {
        sortActionsQueue = Mockito.mock(MomProducer.class);
        clock = Mockito.mock(Clock.class);
        sortEventPool = new SortEventPoolImpl(sortActionsQueue, clock);
    }

    @Test
    void scheduleFutureAction() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(clock.instant()).thenReturn(instant);

        sortEventPool.scheduleFutureAction(buildSortEventAction());

        Mockito.verify(sortActionsQueue).push(Mockito.argThat(matches((SortEvent tmp) -> tmp.getHeader().getIun().equalsIgnoreCase("streamId_IUN"))));
    }

    private SortEventAction buildSortEventAction() {
        return SortEventAction.builder()
                .eventKey("streamId_IUN")
                .delaySeconds(30)
                .writtenCounter(0)
                .type(SortEventType.UNLOCK_EVENTS)
                .build();
    }

    private static <T> ArgumentMatcher<T> matches(Predicate<T> predicate) {
        return predicate::test;
    }
}