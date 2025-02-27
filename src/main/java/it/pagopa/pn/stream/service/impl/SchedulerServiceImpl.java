package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.*;
import it.pagopa.pn.stream.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class SchedulerServiceImpl implements SchedulerService {
    private final StreamsPool streamsPool;
    private final SortEventPool sortEventPool;

    @Override
    public void scheduleStreamEvent(String streamId, String eventId, Integer delay, StreamEventType actionType) {
        StreamAction action = StreamAction.builder()
                .streamId(streamId)
                .eventId(eventId)
                .iun("nd")
                .delay(delay)
                .type(actionType)
                .build();

        this.streamsPool.scheduleFutureAction(action);
    }

    @Override
    public String scheduleSortEvent(String eventKey, Integer delay, Integer writtenCounter, SortEventType sortEventType) {
        SortEventAction.SortEventActionBuilder action = SortEventAction.builder()
                .eventKey(eventKey)
                .writtenCounter(writtenCounter);

        if(Objects.nonNull(delay)){
            action = action.delaySeconds(delay);
        }

        SortEventAction sortEventAction = action.build();

        sortEventPool.scheduleFutureAction(sortEventAction, sortEventType);
        return eventKey;
    }
}
