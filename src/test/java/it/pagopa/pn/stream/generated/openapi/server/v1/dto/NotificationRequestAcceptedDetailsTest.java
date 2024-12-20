package it.pagopa.pn.stream.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationRequestAcceptedDetailsTest {

    private NotificationRequestAcceptedDetails details;

    @BeforeEach
    void setUp() {
        details = new NotificationRequestAcceptedDetails();
        details.setRecIndex(1);
    }

    @Test
    void recIndex() {
        NotificationRequestAcceptedDetails tmp = NotificationRequestAcceptedDetails.builder()
                .recIndex(1)
                .build();
        Assertions.assertEquals(tmp, details.recIndex(1));
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void testEquals() {
        NotificationRequestAcceptedDetails tmp = NotificationRequestAcceptedDetails.builder()
                .recIndex(1)
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }

}