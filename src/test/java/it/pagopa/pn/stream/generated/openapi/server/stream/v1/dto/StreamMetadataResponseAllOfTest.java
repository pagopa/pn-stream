package it.pagopa.pn.stream.generated.openapi.server.stream.v1.dto;

import java.time.Instant;
import java.util.UUID;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV28AllOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamMetadataResponseAllOfTest {

    private StreamMetadataResponseV28AllOf response;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        response = new StreamMetadataResponseV28AllOf();
        response.setStreamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"));
        response.setActivationDate(instant);
    }

    @Test
    void getStreamId() {
        Assertions.assertEquals(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"), response.getStreamId());
    }

    @Test
    void getActivationDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        Assertions.assertEquals(instant, response.getActivationDate());
    }

    @Test
    void testEquals() {
        StreamMetadataResponseV28AllOf expected = StreamMetadataResponseV28AllOf.builder()
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .activationDate(Instant.parse("2021-09-16T15:23:00.00Z"))
                .build();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(response));
    }
    @Test
    void testStreamId() {
        StreamMetadataResponseV28AllOf actual = new StreamMetadataResponseV28AllOf();
        actual.streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"));
        StreamMetadataResponseV28AllOf expected = StreamMetadataResponseV28AllOf.builder()
                .streamId(UUID.fromString("f8c3de3d-1fea-4d7c-a8b0-29f63c4c3454"))
                .build();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testActivationDate() {
        StreamMetadataResponseV28AllOf actual = new StreamMetadataResponseV28AllOf();
        actual.activationDate(Instant.parse("2021-09-16T15:23:00.00Z"));
        StreamMetadataResponseV28AllOf expected = StreamMetadataResponseV28AllOf.builder()
                .activationDate(Instant.parse("2021-09-16T15:23:00.00Z"))
                .build();
        Assertions.assertEquals(expected, actual);
    }
}