package it.pagopa.pn.stream.dto.timeline.details;

import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.stream.dto.io.IoSendMessageResultInt;
import it.pagopa.pn.stream.utils.AuditLogUtils;
import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class SendCourtesyMessageDetailsInt implements RecipientRelatedTimelineElementDetails, CourtesyAddressRelatedTimelineElement {
    private int recIndex;
    private CourtesyDigitalAddressInt digitalAddress;
    private Instant sendDate;
    private IoSendMessageResultInt ioSendMessageResult;
    
    public String toLog() {
        return String.format(
                "recIndex=%d addressType=%s digitalAddress=%s",
                recIndex,
                digitalAddress.getType(),
                AuditLogUtils.SENSITIVE
        );
    }
}
