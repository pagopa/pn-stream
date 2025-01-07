package it.pagopa.pn.stream.dto.paperchannel;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode
@Getter
@ToString
public class CategorizedAttachmentsResultInt {

    private List<ResultFilterInt> acceptedAttachments = null;
    private List<ResultFilterInt> discardedAttachments = null;
}
