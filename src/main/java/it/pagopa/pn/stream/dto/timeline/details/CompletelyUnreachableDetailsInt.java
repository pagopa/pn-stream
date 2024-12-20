package it.pagopa.pn.stream.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class CompletelyUnreachableDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant legalFactGenerationDate;

    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
