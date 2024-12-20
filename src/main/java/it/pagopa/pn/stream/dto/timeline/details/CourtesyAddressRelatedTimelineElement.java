package it.pagopa.pn.stream.dto.timeline.details;

import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;

public interface CourtesyAddressRelatedTimelineElement extends ConfidentialInformationTimelineElement{
    CourtesyDigitalAddressInt getDigitalAddress();
    void setDigitalAddress(CourtesyDigitalAddressInt digitalAddressInt);
}
