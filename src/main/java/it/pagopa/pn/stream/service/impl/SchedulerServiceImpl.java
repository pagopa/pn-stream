package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.*;
import it.pagopa.pn.stream.service.SchedulerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public void scheduleSortEvent(String streamId, String iun, Integer delay, Integer writtenCounter, SortEventType sortEventType) {
        SortEventAction action = SortEventAction.builder()
                .eventKey(streamId + "_" + iun)
                .writtenCounter(writtenCounter)
                .delaySeconds(delay)
                .build();

        this.sortEventPool.scheduleFutureAction(action, sortEventType);
    }
}
