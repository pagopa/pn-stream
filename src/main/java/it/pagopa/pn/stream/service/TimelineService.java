package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementDetailsInt;

import java.util.Set;

public interface TimelineService {

    <T extends TimelineElementDetailsInt> T enrichTimelineElementWithConfidentialInformation(TimelineElementDetailsInt details,
                                                          ConfidentialTimelineElementDtoInt confidentialDto);

    Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId,
                                                            boolean confidentialInfoRequired);

    Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired);
}
