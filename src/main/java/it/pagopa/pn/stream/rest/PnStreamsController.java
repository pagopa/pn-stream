package it.pagopa.pn.stream.rest;

import it.pagopa.pn.commons.utils.MDCUtils;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.stream.service.StreamsService;
import it.pagopa.pn.stream.utils.MdcKey;
import it.pagopa.pn.stream.generated.openapi.server.v1.api.StreamsApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PnStreamsController implements StreamsApi {

    private final StreamsService streamsService;

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV26>> createEventStreamV27(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<StreamCreationRequestV27> streamCreationRequest, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
        log.info("[enter] createEventStream xPagopaPnCxId={} xPagopaPnCxGroups={}", xPagopaPnCxId, xPagopaPnCxGroups);

        return MDCUtils.addMDCToContextAndExecute(
            streamsService.createEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamCreationRequest)
                        .map(ResponseEntity::ok));
    }


    @Override
    public Mono<ResponseEntity<Void>> deleteEventStreamV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
        log.info("[enter] deleteEventStream xPagopaPnCxId={} uuid={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
                streamsService.deleteEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups,xPagopaPnApiVersion, streamId)
                        .then(Mono.just(ResponseEntity.noContent().build()))
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV26>> getEventStreamV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={}", xPagopaPnCxId, streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            streamsService.getEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<Flux<StreamListElement>>> listEventStreamsV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, final ServerWebExchange exchange) {
        log.info("[enter] listEventStreams xPagopaPnCxId={}", xPagopaPnCxId);
        return Mono.fromSupplier(() -> ResponseEntity.ok(streamsService.listEventStream(xPagopaPnUid, xPagopaPnCxId,xPagopaPnCxGroups, xPagopaPnApiVersion)));
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV26>> updateEventStreamV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, Mono<StreamRequestV26> streamRequest, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
        log.info("[enter] updateEventStream xPagopaPnCxId={} xPagopaPnCxType={} xPagopaPnCxGroups={} streamId={}", xPagopaPnCxId, xPagopaPnCxType,xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups), streamId.toString());

        return MDCUtils.addMDCToContextAndExecute(
            streamsService.updateEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId, streamRequest)
                        .map(ResponseEntity::ok)
        );
    }

    @Override
    public Mono<ResponseEntity<StreamMetadataResponseV26>> disableEventStreamV26(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion,  final ServerWebExchange exchange) {
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);
        log.info("[enter] disableEventStream xPagopaPnCxId={} uuid={} xPagopaPnCxGroups={}", xPagopaPnCxId, streamId.toString(), xPagopaPnCxGroups==null?"":String.join(",", xPagopaPnCxGroups));

        return MDCUtils.addMDCToContextAndExecute(
            streamsService.disableEventStream(xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId)
                .map(ResponseEntity::ok)
        );
    }


}
