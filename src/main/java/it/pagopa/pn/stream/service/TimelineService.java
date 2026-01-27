package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementDetailsV28;

public interface TimelineService {

    String enrichTimelineElementWithConfidentialInformation(String category, String details,
                                                          ConfidentialTimelineElementDtoInt confidentialDto);
    void enrichTimelineElementWithConfidentialInformation(String category, TimelineElementDetailsV28 details,
                                                            ConfidentialTimelineElementDtoInt confidentialDto);

}
