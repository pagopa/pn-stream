package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementId;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ConfidentialInformationService {

        Flux<ConfidentialTimelineElementDtoInt> getTimelineConfidentialInformation(List<TimelineElementInternal> timelineElementInternal);

        Flux<ConfidentialTimelineElementDtoInt> getTimelineConfidentialInformationFromConfidentialElementIds(List<ConfidentialTimelineElementId> request);
}
