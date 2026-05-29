package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ExternalEvent;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.PaymentEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExternalEventTest {

    private ExternalEvent externalEvent;

    @BeforeEach
    void setUp() {
        externalEvent = new ExternalEvent();
        externalEvent.setPayment(new PaymentEvent()
                .iun("001"));
    }

    @Test
    void payment() {
        ExternalEvent actual = new ExternalEvent();
        actual.payment(new PaymentEvent()
                .iun("001"));
        Assertions.assertEquals(externalEvent, actual);
    }

    @Test
    void getPayment() {
        PaymentEvent expected = new PaymentEvent()
                .iun("001");
        Assertions.assertEquals(expected, externalEvent.getPayment());
    }

    @Test
    void testEquals() {
        ExternalEvent expected = new ExternalEvent();
        expected.payment(new PaymentEvent()
                .iun("001"));
        Assertions.assertEquals(Boolean.TRUE, expected.equals(externalEvent));
    }
    
}