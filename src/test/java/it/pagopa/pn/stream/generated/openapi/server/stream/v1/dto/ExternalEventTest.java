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
        externalEvent.setPayment(PaymentEvent.builder()
                .iun("001")
                .build());
    }

    @Test
    void payment() {
        ExternalEvent actual = new ExternalEvent();
        actual.payment(PaymentEvent.builder()
                .iun("001")
                .build());
        Assertions.assertEquals(externalEvent, actual);
    }

    @Test
    void getPayment() {
        PaymentEvent expected = PaymentEvent.builder()
                .iun("001")
                .build();
        Assertions.assertEquals(expected, externalEvent.getPayment());
    }

    @Test
    void testEquals() {
        ExternalEvent expected = new ExternalEvent();
        expected.payment(PaymentEvent.builder()
                .iun("001")
                .build());
        Assertions.assertEquals(Boolean.TRUE, expected.equals(externalEvent));
    }
    
}