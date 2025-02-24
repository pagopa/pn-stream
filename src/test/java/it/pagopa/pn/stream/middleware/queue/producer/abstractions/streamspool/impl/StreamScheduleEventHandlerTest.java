package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import it.pagopa.pn.stream.service.StreamScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamScheduleEventHandlerTest {

    @InjectMocks
    private StreamScheduleEventHandler handler;
    @Mock
    private StreamScheduleService scheduleService;

    @Test
    void handleUnlockEvents() {
        // GIVEN
        SortEventAction action = buildSortEventAction();
        Mockito.when(scheduleService.unlockEvents(any(), anyBoolean())).thenReturn(Mono.empty());

        // WHEN
        handler.handleUnlockEvents(action);

        // THEN
        Mockito.verify(scheduleService, Mockito.times(1))
                .unlockEvents(any(), anyBoolean());
    }

    @Test
    void handleUnlockAllEvents() {
        // GIVEN
        SortEventAction action = buildSortEventAction();
        Mockito.when(scheduleService.unlockEvents(any(), anyBoolean())).thenReturn(Mono.empty());

        // WHEN
        handler.handleUnlockAllEvents(action);

        // THEN
        Mockito.verify(scheduleService, Mockito.times(1))
                .unlockEvents(any(), anyBoolean());
    }

    private SortEventAction buildSortEventAction() {
        return SortEventAction.builder()
                .eventKey("streamId_iun")
                .delaySeconds(30)
                .writtenCounter(0)
                .build();
    }
}