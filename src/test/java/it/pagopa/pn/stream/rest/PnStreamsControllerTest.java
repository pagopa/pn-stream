package it.pagopa.pn.stream.rest;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.exceptions.PnNotFoundException;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.stream.service.StreamsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.UUID;


@WebFluxTest(PnStreamsController.class)
class PnStreamsControllerTest {

    public static final String API_VERSION = "v2.8";
    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private StreamsService service;

    @MockBean
    private PnStreamConfigs pnStreamConfigs;

    @Test
    void createEventStreamOk() {
        Mockito.when(service.createEventStream(Mockito.anyString(),Mockito.anyString(), Mockito.any(),Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new StreamMetadataResponseV28()));
        StreamCreationRequestV28 request = StreamCreationRequestV28.builder()
                .eventType(StreamCreationRequestV28.EventTypeEnum.STATUS)
                .build();

        webTestClient.post()
                .uri("/delivery-progresses-2/" + API_VERSION + "/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json")
                .body(Mono.just(request), StreamCreationRequestV28.class)
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV28.class);

        Mockito.verify(service).createEventStream(Mockito.anyString(),Mockito.anyString(), Mockito.any(),Mockito.any(), Mockito.any());
    }

    @Test
    void createEventStreamKoRuntimeEx() {
        Mockito.when(service.createEventStream(Mockito.anyString(), Mockito.anyString(),Mockito.any(),Mockito.any(), Mockito.any()))
                .thenThrow(new RuntimeException());

        webTestClient.post()
                .uri("/delivery-progresses-2/" + API_VERSION + "/streams")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );
    }

    @Test
    void deleteEventStreamMissing() {
        String streamId = UUID.randomUUID().toString();
        StreamEntityDao streamEntityDao = Mockito.mock( StreamEntityDao.class );
        Mockito.when(streamEntityDao.get(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(service.deleteEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class)))
            .thenReturn(Mono.error(new PnNotFoundException("","","")));

        webTestClient.delete()
            .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
            .header(HttpHeaders.ACCEPT, "application/problem+json")
            .headers(httpHeaders -> {
                httpHeaders.set("x-pagopa-pn-uid","test");
                httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                httpHeaders.set("x-pagopa-pn-cx-id","test");
                httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
            })
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void deleteEventStream() {
        String streamId = UUID.randomUUID().toString();

        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(streamId);

        Mockito.when(service.deleteEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class)))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/problem+json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isNoContent();

        Mockito.verify(service).deleteEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class));
    }

    @Test
    void disableEventStream() {
        String streamId = UUID.randomUUID().toString();

        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(streamId);

        Mockito.when(service.disableEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class)))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}/action/disable").replace("{streamId}", streamId) )
            .header(HttpHeaders.ACCEPT, "application/problem+json")
            .headers(httpHeaders -> {
                httpHeaders.set("x-pagopa-pn-uid","test");
                httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                httpHeaders.set("x-pagopa-pn-cx-id","test");
                httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
            })
            .exchange()
            .expectStatus().isOk();

        Mockito.verify(service).disableEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class));
    }

    @Test
    void deleteEventStreamKoRuntime() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.deleteEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class)))
                .thenThrow(new NullPointerException());

        webTestClient.delete()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), problem.getStatus());
                        }
                );
    }

    @Test
    void getEventStream() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.getEventStream(Mockito.anyString(),Mockito.anyString(), Mockito.any(),Mockito.any(), Mockito.any(UUID.class)))
                .thenReturn(Mono.just(new StreamMetadataResponseV28()));

        webTestClient.get()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV28.class);

        Mockito.verify(service).getEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class));

    }

    @Test
    void getEventStreamKoRuntime() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.getEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class)))
                .thenThrow(new NullPointerException());

        webTestClient.get()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );
    }

    @Test
    void listEventStreams() {
        webTestClient.get()
                .uri("/delivery-progresses-2/" + API_VERSION + "/streams")
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(StreamListElement.class);

        Mockito.verify(service).listEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any());

    }

    @Test
    void updateEventStream() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.updateEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any()))
                .thenReturn(Mono.just(new StreamMetadataResponseV28()));

        webTestClient.put()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(StreamMetadataResponseV28.class);

        Mockito.verify(service).updateEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any());
    }

    @Test
    void updateEventStreamKoRuntimeEx() {
        String streamId = UUID.randomUUID().toString();
        Mockito.when(service.updateEventStream(Mockito.anyString(),Mockito.anyString(),Mockito.any(), Mockito.any(), Mockito.any(UUID.class), Mockito.any()))
                .thenThrow(new NullPointerException());

        webTestClient.put()
                .uri( ("/delivery-progresses-2/" + API_VERSION + "/streams/{streamId}").replace("{streamId}", streamId) )
                .header(HttpHeaders.ACCEPT, "application/json")
                .headers(httpHeaders -> {
                    httpHeaders.set("x-pagopa-pn-uid","test");
                    httpHeaders.set("x-pagopa-pn-cx-type", CxTypeAuthFleet.PA.getValue());
                    httpHeaders.set("x-pagopa-pn-cx-id","test");
                    httpHeaders.set("x-pagopa-pn-cx-groups", Collections.singletonList("test").toString());
                })
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(Problem.class).consumeWith(
                        elem -> {
                            Problem problem = elem.getResponseBody();
                            assert problem != null;
                            Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
                        }
                );

    }
}
