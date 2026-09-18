package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ExternalEvent;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ExternalEventsRequest;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.PaymentEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ExternalEventsRequestTest {

    private ExternalEventsRequest eventsRequest;

    @BeforeEach 
    void setUp() {
        eventsRequest = new ExternalEventsRequest();
        eventsRequest.setEvents(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()));
    }

    @Test
    void events() {
        ExternalEventsRequest expected = ExternalEventsRequest.builder()
                .events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()))
                .build();

        Assertions.assertEquals(expected, eventsRequest.events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build())));
    }

    @Test
    void getEvents() {
        Assertions.assertEquals(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()), eventsRequest.getEvents());
    }

    @Test
    void testEquals() {
        ExternalEventsRequest expected = ExternalEventsRequest.builder()
                .events(Collections.singletonList(ExternalEvent.builder().payment(PaymentEvent.builder().iun("001").build()).build()))
                .build();

        Assertions.assertEquals(Boolean.TRUE, expected.equals(eventsRequest));
    }
}