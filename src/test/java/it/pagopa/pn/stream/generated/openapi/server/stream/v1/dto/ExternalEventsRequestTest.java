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
        eventsRequest.setEvents(Collections.singletonList(new ExternalEvent().payment(new PaymentEvent().iun("001"))));
    }

    @Test
    void events() {
        ExternalEventsRequest expected = new ExternalEventsRequest()
                .events(Collections.singletonList(new ExternalEvent().payment(new PaymentEvent().iun("001"))));

        Assertions.assertEquals(expected, eventsRequest.events(Collections.singletonList(new ExternalEvent().payment(new PaymentEvent().iun("001")))));
    }

    @Test
    void getEvents() {
        Assertions.assertEquals(Collections.singletonList(new ExternalEvent().payment(new PaymentEvent().iun("001"))), eventsRequest.getEvents());
    }

    @Test
    void testEquals() {
        ExternalEventsRequest expected = new ExternalEventsRequest()
                .events(Collections.singletonList(new ExternalEvent().payment(new PaymentEvent().iun("001"))));

        Assertions.assertEquals(Boolean.TRUE, expected.equals(eventsRequest));
    }
}