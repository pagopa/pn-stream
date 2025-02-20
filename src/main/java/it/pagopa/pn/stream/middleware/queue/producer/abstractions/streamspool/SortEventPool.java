package it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool;

public interface SortEventPool {
     void scheduleFutureAction(SortEventAction action, SortEventType sortEventType);
}