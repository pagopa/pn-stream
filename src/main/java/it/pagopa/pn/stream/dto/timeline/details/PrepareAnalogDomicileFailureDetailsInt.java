package it.pagopa.pn.stream.dto.timeline.details;

import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class PrepareAnalogDomicileFailureDetailsInt implements RecipientRelatedTimelineElementDetails, PhysicalAddressRelatedTimelineElement {

    private int recIndex;
    private PhysicalAddressInt foundAddress;
    private String failureCause;
    private String prepareRequestId;

    public String toLog() {
        return String.format(
            "recIndex=%d failureCause=%s prepareRequestId=%s",
            recIndex,
            failureCause,
            prepareRequestId
        );
    }

    @Override
    public PhysicalAddressInt getPhysicalAddress() {
        return foundAddress;
    }

    @Override
    public void setPhysicalAddress(PhysicalAddressInt physicalAddressInt) {
        this.foundAddress = physicalAddressInt;
    }
}