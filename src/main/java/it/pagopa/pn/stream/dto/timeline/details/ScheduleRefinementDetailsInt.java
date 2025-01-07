package it.pagopa.pn.stream.dto.timeline.details;

import lombok.*;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode
@Getter
@Setter
@ToString
public class ScheduleRefinementDetailsInt implements RecipientRelatedTimelineElementDetails {
    private int recIndex;
    private Instant schedulingDate;

    public String toLog() {
        return String.format(
                "recIndex=%d",
                recIndex
        );
    }
}
