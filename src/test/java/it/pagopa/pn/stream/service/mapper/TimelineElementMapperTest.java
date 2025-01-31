package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementV26;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class TimelineElementMapperTest {

    @Test
    void internalToExternal_validInput() {
        TimelineElementInternal internalDto = buildTimelineElementInternal();
        TimelineElementV26 result = TimelineElementMapper.internalToExternal(internalDto);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(internalDto.getTimelineElementId(), result.getElementId());
        Assertions.assertEquals(internalDto.getTimestamp(), result.getTimestamp());
    }

    @Test
    void internalToExternal_invalidJson() {
        TimelineElementInternal internalDto = buildInvalidTimelineElementInternal();
        Assertions.assertThrows(PnStreamException.class, () -> {
            TimelineElementMapper.internalToExternal(internalDto);
        });
    }

    private TimelineElementInternal buildTimelineElementInternal() {
        return TimelineElementInternal.builder()
                .timelineElementId("001")
                .timestamp(Instant.parse("2023-10-10T10:00:00Z"))
                .details("{\"normalizedAddress\":{\"address\":\"VIA SENZA NOME\",\"addressDetails\":\"SCALA B\",\"at\":\"Presso\",\"foreignState\":\"ITALIA\",\"municipality\":\"COSENZA\",\"municipalityDetails\":\"COSENZA\",\"province\":\"CS\",\"zip\":\"87100\"},\"oldAddress\":{\"address\":\"Via senza nome\",\"addressDetails\":\"scala b\",\"at\":\"Presso\",\"foreignState\":\"ITALIA\",\"municipality\":\"Cosenza\",\"municipalityDetails\":\"Cosenza\",\"province\":\"CS\",\"zip\":\"87100\"},\"recIndex\":0}")
                .build();
    }

    private TimelineElementInternal buildInvalidTimelineElementInternal() {
        return TimelineElementInternal.builder()
                .timelineElementId("001")
                .timestamp(Instant.parse("2023-10-10T10:00:00Z"))
                .details("{invalidJson}")
                .build();
    }
}