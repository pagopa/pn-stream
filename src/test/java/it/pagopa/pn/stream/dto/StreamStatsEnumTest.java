package it.pagopa.pn.stream.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;

class StreamStatsEnumTest {

    @Test
    void testEnumValues() {
        StreamStatsEnum[] values = StreamStatsEnum.values();
        assertNotNull(values);
        assertEquals(5, values.length);
        assertEquals(StreamStatsEnum.NUMBER_OF_REQUESTS, values[0]);
        assertEquals(StreamStatsEnum.RETRY_AFTER_VIOLATION, values[1]);
        assertEquals(StreamStatsEnum.NUMBER_OF_READINGS, values[2]);
        assertEquals(StreamStatsEnum.NUMBER_OF_WRITINGS, values[3]);
        assertEquals(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, values[4]);
    }

    @Test
    void testEnumValueOf() {
        assertEquals(StreamStatsEnum.NUMBER_OF_REQUESTS, StreamStatsEnum.valueOf("NUMBER_OF_REQUESTS"));
        assertEquals(StreamStatsEnum.RETRY_AFTER_VIOLATION, StreamStatsEnum.valueOf("RETRY_AFTER_VIOLATION"));
        assertEquals(StreamStatsEnum.NUMBER_OF_READINGS, StreamStatsEnum.valueOf("NUMBER_OF_READINGS"));
        assertEquals(StreamStatsEnum.NUMBER_OF_WRITINGS, StreamStatsEnum.valueOf("NUMBER_OF_WRITINGS"));
        assertEquals(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, StreamStatsEnum.valueOf("NUMBER_OF_EMPTY_READINGS"));
    }
}
