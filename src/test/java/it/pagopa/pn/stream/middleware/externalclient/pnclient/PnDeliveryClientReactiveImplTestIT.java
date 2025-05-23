package it.pagopa.pn.stream.middleware.externalclient.pnclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import it.pagopa.pn.stream.MockAWSObjectsTest;
import it.pagopa.pn.stream.exceptions.PnNotFoundException;
import it.pagopa.pn.stream.exceptions.PnStreamNotFoundException;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactiveImpl;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.stream.stream-url=http://localhost:9998",
})
class PnDeliveryClientReactiveImplTestIT extends MockAWSObjectsTest {
    @Mock
    private InternalOnlyApi pnDeliveryApi;
    @MockBean
    private PnDeliveryClientReactiveImpl client;
    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9998);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new PnDeliveryClientReactiveImpl(pnDeliveryApi);
    }

    @Test
    void getSentNotification() throws JsonProcessingException {
        //Given
        String iun ="iunTest";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        when(pnDeliveryApi.getSentNotificationPrivate(anyString())).thenReturn(Mono.just(notification));

        String path = "/delivery-private/notifications/{iun}"
                .replace("{iun}",iun);
        
        ObjectMapper mapper = new ObjectMapper();
        String respjson = mapper.writeValueAsString(notification);

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withBody(respjson)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));
        
        Mono<SentNotificationV25> response = client.getSentNotification(iun);

        SentNotificationV25 notificationResponse = response.block();
        Assertions.assertNotNull(notificationResponse);
        Assertions.assertEquals(notification, notificationResponse);
    }
    
    @Test
    void getSentNotificationError(){
        //Given
        String iun ="iunTest";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        when(pnDeliveryApi.getSentNotificationPrivate(anyString()))
                .thenReturn(Mono.error(new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null)));

        String path = "/delivery-private/notifications/{iun}"
                .replace("{iun}",iun);
        
        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(400));

        client.getSentNotification(iun).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    return Mono.empty();
                }
        );
    }

    @Test
    void getSentNotificationError404(){
        //Given
        String iun ="iunTest";
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun(iun);

        String path = "/delivery-private/notifications/{iun}"
                .replace("{iun}",iun);

        when(pnDeliveryApi.getSentNotificationPrivate(anyString()))
                .thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        new MockServerClient("localhost", 9998)
                .when(request()
                        .withMethod("GET")
                        .withPath(path))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(404));

        client.getSentNotification(iun).onErrorResume(
                ex -> {
                    Assertions.assertNotNull(ex);
                    Assertions.assertEquals(PnNotFoundException.class, ex.getClass());
                    return Mono.empty();
                }
        );
    }

    @Test
    void getSentNotification_returnsNotification() {
        SentNotificationV25 notification = new SentNotificationV25();
        notification.setIun("iunTest");

        when(pnDeliveryApi.getSentNotificationPrivate(anyString())).thenReturn(Mono.just(notification));

        Mono<SentNotificationV25> result = client.getSentNotification("iunTest");

        StepVerifier.create(result)
                .expectNext(notification)
                .verifyComplete();
    }

    @Test
    void getSentNotification_handlesNotFound() {
        when(pnDeliveryApi.getSentNotificationPrivate(anyString()))
                .thenReturn(Mono.error(new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "Not Found", null, null, null)));

        Mono<SentNotificationV25> result = client.getSentNotification("iunTest");

        StepVerifier.create(result)
                .expectError(PnStreamNotFoundException.class)
                .verify();
    }

    @Test
    void getSentNotification_handlesInternalError() {
        when(pnDeliveryApi.getSentNotificationPrivate(anyString()))
                .thenReturn(Mono.error(new WebClientResponseException(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", null, null, null)));

        Mono<SentNotificationV25> result = client.getSentNotification("iunTest");

        StepVerifier.create(result)
                .expectError(PnInternalException.class)
                .verify();
    }
}