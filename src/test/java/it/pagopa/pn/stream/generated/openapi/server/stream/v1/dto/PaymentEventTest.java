package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.PaymentEvent;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.RecipientType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class PaymentEventTest {

    private PaymentEvent event;

    @BeforeEach
    public void setup() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        event = PaymentEvent.builder()
                .iun("001")
                .recipientTaxId("002")
                .recipientType(RecipientType.PF)
                .paymentType(PaymentEvent.PaymentTypeEnum.PAGOPA)
                .timestamp(instant)
                .build();
    }

    @Test
    void getIun() {
        String value = event.getIun();
        Assertions.assertEquals("001", value);
    }

    @Test
    void getRecipientTaxId() {
        String value = event.getRecipientTaxId();
        Assertions.assertEquals("002", value);
    }

    @Test
    void getRecipientType() {
        RecipientType value = event.getRecipientType();
        Assertions.assertEquals(RecipientType.PF, value);
    }

    @Test
    void getPaymentType() {
        PaymentEvent.PaymentTypeEnum payment = event.getPaymentType();
        Assertions.assertEquals(PaymentEvent.PaymentTypeEnum.PAGOPA, payment);
    }

    @Test
    void getTimestamp() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        Instant actual = event.getTimestamp();
        Assertions.assertEquals(instant, actual);
    }
    
}