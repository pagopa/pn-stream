package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventType;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;

public interface SchedulerService {
    void scheduleStreamEvent(String streamId, String eventId, Integer delay, StreamEventType actionType);
    void scheduleSortEvent(String streamId, String iun, Integer delay, Integer writtenCounter, SortEventType sortEventType);
}
