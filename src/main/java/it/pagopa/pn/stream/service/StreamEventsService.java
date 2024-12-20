package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.stream.ProgressResponseElementDto;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface StreamEventsService {

    Mono<ProgressResponseElementDto> consumeEventStream(String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, String lastEventId);

    Mono<Void> saveEvent(String paId, String eventId, String iun);

    Mono<Void> purgeEvents(String streamId, String eventId, boolean olderThan);
}
