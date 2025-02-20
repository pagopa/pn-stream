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
    void scheduleFutureActionUnlockEvents() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(clock.instant()).thenReturn(instant);

        sortEventPool.scheduleFutureAction(buildSortEventAction(), SortEventType.UNLOCK_EVENTS);

        Mockito.verify(sortActionsQueue).push(Mockito.argThat(matches((SortEvent tmp) ->
                tmp.getHeader().getEventType().equalsIgnoreCase("UNLOCK_EVENTS") &&
                tmp.getPayload().getEventKey().equalsIgnoreCase("streamId_IUN"))), Mockito.eq(30));
    }

    @Test
    void scheduleFutureActionUnlockAllEvents() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");

        Mockito.when(clock.instant()).thenReturn(instant);

        sortEventPool.scheduleFutureAction(buildSortEventAction(), SortEventType.UNLOCK_ALL_EVENTS);

        Mockito.verify(sortActionsQueue).push(Mockito.argThat(matches((SortEvent tmp) ->
                tmp.getHeader().getEventType().equalsIgnoreCase("UNLOCK_ALL_EVENTS") &&
                        tmp.getPayload().getEventKey().equalsIgnoreCase("streamId_IUN"))), Mockito.eq(30));
    }


    private SortEventAction buildSortEventAction() {
        return SortEventAction.builder()
                .eventKey("streamId_IUN")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();
    }

    private static <T> ArgumentMatcher<T> matches(Predicate<T> predicate) {
        return predicate::test;
    }
}