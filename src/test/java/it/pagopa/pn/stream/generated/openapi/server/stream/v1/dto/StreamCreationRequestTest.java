package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import java.util.Collections;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV29;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamCreationRequestTest {

    private StreamCreationRequestV29 request;

    @BeforeEach
    void setUp() {
        request = new StreamCreationRequestV29();
        request.setEventType(StreamCreationRequestV29.EventTypeEnum.STATUS);
        request.setFilterValues(Collections.singletonList("001"));
        request.setTitle("001");
        request.setWaitForAccepted(false);
    }

    @Test
    void title() {
        StreamCreationRequestV29 expected = StreamCreationRequestV29.builder()
                .title("001")
                .eventType(StreamCreationRequestV29.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .waitForAccepted(false)
                .build();
        Assertions.assertEquals(expected, request.title("001"));
    }

    @Test
    void getTitle() {
        Assertions.assertEquals("001", request.getTitle());
    }

    @Test
    void eventType() {
        StreamCreationRequestV29 expected = StreamCreationRequestV29.builder()
                .title("001")
                .eventType(StreamCreationRequestV29.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .waitForAccepted(false)
                .build();
        Assertions.assertEquals(expected, request.eventType(StreamCreationRequestV29.EventTypeEnum.STATUS));
    }

    @Test
    void getEventType() {
        Assertions.assertEquals(StreamCreationRequestV29.EventTypeEnum.STATUS, request.getEventType());
    }

    @Test
    void filterValues() {
        StreamCreationRequestV29 expected = StreamCreationRequestV29.builder()
                .title("001")
                .eventType(StreamCreationRequestV29.EventTypeEnum.STATUS)
                .filterValues(Collections.singletonList("001"))
                .groups(Collections.emptyList())
                .waitForAccepted(false)
                .build();
        Assertions.assertEquals(expected, request.filterValues(Collections.singletonList("001")));
    }

    @Test
    void getFilterValues() {
        Assertions.assertEquals(Collections.singletonList("001"), request.getFilterValues());
    }


}