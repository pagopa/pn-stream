package it.pagopa.pn.stream.rest;

import it.pagopa.pn.stream.dto.stream.ProgressResponseElementDto;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV26;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV26;
import it.pagopa.pn.stream.service.StreamEventsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@WebFluxTest(PnEventsController.class)
class PnEventsControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private StreamEventsService service;

    @Test
    void consumeEventStreamOk() {
        String streamId = UUID.randomUUID().toString();
        List<ProgressResponseElementV26> timelineElements = Collections.singletonList(ProgressResponseElementV26.builder()
                .timestamp( Instant.now() )
                .eventId( "event_id" )
                .iun("")
                .newStatus(NotificationStatusV26.ACCEPTED)
                .timelineEventCategory(TimelineElementCategoryV26.REQUEST_ACCEPTED)
                .build()
        );
        ProgressResponseElementDto dto = ProgressResponseElementDto.builder()
                .retryAfter(0)
                .progressResponseElementList(timelineElements)
                .build();

        Mockito.when(service.consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any()))
                .thenReturn(Mono.just(dto ));


        webTestClient.get()
                .uri( "/delivery-progresses/v2.6/streams/{streamId}/events".replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("retry-after", "0")
                .expectBodyList(ProgressResponseElementV26.class);

        Mockito.verify(service).consumeEventStream(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any());

    }

    @Test
    void informOnExternalEvent() {
        webTestClient.post()
                .uri( "/delivery-progresses/events" )
                .contentType(MediaType.APPLICATION_JSON)
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
