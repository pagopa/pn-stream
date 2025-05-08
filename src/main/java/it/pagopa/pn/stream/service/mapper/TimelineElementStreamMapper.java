package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementV27;


public class TimelineElementStreamMapper {
    private TimelineElementStreamMapper(){}

    public static TimelineElementV27 internalToExternal(TimelineElementInternal internalDto) {
        // passo da TimelineElementMapper.internalToExternal(internalDto) in modo da replicare gli stessi controlli già presenti per il mapper di delivery push
        return TimelineElementMapper.internalToExternal(internalDto);
    }
}