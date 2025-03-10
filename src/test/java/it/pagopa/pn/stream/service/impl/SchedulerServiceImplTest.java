package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;


@ExtendWith(SpringExtension.class)
class SchedulerServiceImplTest {

    private StreamsPool streamsPool;
    private SortEventPool sortEventPool;

    @Mock
    private Clock clock;

    
    private SchedulerServiceImpl schedulerService;
    
    @BeforeEach
    void setup() {
        streamsPool = Mockito.mock(StreamsPool.class);
        sortEventPool = Mockito.mock(SortEventPool.class);
        clock = Mockito.mock(Clock.class);

        schedulerService = new SchedulerServiceImpl(streamsPool, sortEventPool);
    }


    @Test
    void testScheduleWebhookEvent() {
        StreamAction action = StreamAction.builder()
                .streamId("01")
                .eventId("02")
                .iun("nd")
                .delay(4)
                .type(StreamEventType.REGISTER_EVENT)
                .build();

        schedulerService.scheduleStreamEvent("01", "02", 4, StreamEventType.REGISTER_EVENT);

        Mockito.verify(streamsPool, Mockito.times(1)).scheduleFutureAction(action);
    }

    @Test
    void testSortActionEvent() {
        SortEventAction action = SortEventAction.builder()
                .eventKey("streamId_iun")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();

        schedulerService.scheduleSortEvent("streamId_iun", 30, 0, SortEventType.UNLOCK_EVENTS);

        Mockito.verify(sortEventPool, Mockito.times(1)).scheduleFutureAction(action, SortEventType.UNLOCK_EVENTS);
    }

    @Test
    void testSortActionEvent2() {
        SortEventAction action = SortEventAction.builder()
                .eventKey("streamId")
                .delaySeconds(null)
                .writtenCounter(0)
                .build();

        schedulerService.scheduleSortEvent("streamId", null, 0, SortEventType.UNLOCK_ALL_EVENTS);

        Mockito.verify(sortEventPool, Mockito.times(1)).scheduleFutureAction(action, SortEventType.UNLOCK_ALL_EVENTS);
    }
}