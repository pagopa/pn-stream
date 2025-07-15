package it.pagopa.pn.stream.rest;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ExternalEventsRequest;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV29;
import it.pagopa.pn.stream.service.StreamEventsService;
import it.pagopa.pn.stream.utils.MdcKey;
import it.pagopa.pn.stream.generated.openapi.server.v1.api.EventsApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
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
public class PnEventsController implements EventsApi {

    public static final String HEADER_RETRY_AFTER = "retry-after";
    private final StreamEventsService streamEventsService;

    @Override
    public Mono<ResponseEntity<Flux<ProgressResponseElementV29>>> consumeEventStreamV29(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, UUID streamId, List<String> xPagopaPnCxGroups, String xPagopaPnApiVersion, String lastEventId, final ServerWebExchange exchange) {
        log.info("[enter] getEventStream xPagopaPnCxId={} streamId={} lastEventID={}", xPagopaPnCxId, streamId.toString(), lastEventId);
        MDC.put(MDCUtils.MDC_PN_CTX_TOPIC, MdcKey.STREAM_KEY);

        return MDCUtils.addMDCToContextAndExecute(
                streamEventsService.consumeEventStream(xPagopaPnCxId, xPagopaPnCxGroups, xPagopaPnApiVersion, streamId, lastEventId)
                        .map(r -> {
                            HttpHeaders responseHeaders = new HttpHeaders();
                            responseHeaders.set(HEADER_RETRY_AFTER,
                                    ""+r.getRetryAfter());

                            return ResponseEntity
                                    .ok()
                                    .headers(responseHeaders)
                                    .body(Flux.fromIterable(r.getProgressResponseElementList()));
                        })
        );

    }

    @Override
    public Mono<ResponseEntity<Void>> informOnExternalEvent(String xPagopaPnUid, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnCxId, Mono<ExternalEventsRequest> externalEventsRequest, List<String> xPagopaPnCxGroups, final ServerWebExchange exchange) {
        log.error("[enter] informOnExternalEvent not implemented yet");
        return Mono.just(ResponseEntity.internalServerError().build());
    }
}
