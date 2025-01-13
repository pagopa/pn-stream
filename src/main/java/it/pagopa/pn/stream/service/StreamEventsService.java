package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.stream.ProgressResponseElementDto;
import java.util.List;
import java.util.UUID;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import reactor.core.publisher.Mono;

public interface StreamEventsService {

    Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, String lastEventId);

    Mono<Void> saveEvent(TimelineElementInternal timelineElementInternal, StreamEventType streamEventType);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
