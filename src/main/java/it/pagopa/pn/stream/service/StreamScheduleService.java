package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.SortEventAction;
import reactor.core.publisher.Mono;

public interface StreamScheduleService {
    Mono<Void> unlockEvents(SortEventAction event, boolean resendMessage);
}