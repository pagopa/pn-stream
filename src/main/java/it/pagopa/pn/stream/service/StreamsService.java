package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV30;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamListElement;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV30;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamRequestV30;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface StreamsService {
    Mono<StreamMetadataResponseV30> createEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, Mono<StreamCreationRequestV30> streamCreationRequest);

    Mono<Void> deleteEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Mono<StreamMetadataResponseV30> getEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);

    Flux<StreamListElement> listEventStream(String xPagopaPnUid, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion);

    Mono<StreamMetadataResponseV30> updateEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId, Mono<StreamRequestV30> streamRequest);

    Mono<StreamMetadataResponseV30> disableEventStream(String xPagopaPnUid,String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, UUID streamId);
}
