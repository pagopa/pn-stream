package it.pagopa.pn.stream.dto.timeline;

import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementDetailsInt;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TimelineElementInternal implements Comparable<TimelineElementInternal> {
    private String iun;
    private String elementId;
    private Instant timestamp;
    private String paId;
    private List<LegalFactsIdInt> legalFactsIds;
    private TimelineElementCategoryInt category;
    private String details;
    private StatusInfoInternal statusInfo;
    private Instant notificationSentAt;
    private Instant ingestionTimestamp; //Questo campo viene valorizzato solo ed esclusivamente in uscita per api e webhook dal mapper
    private Instant eventTimestamp; //Questo campo viene valorizzato solo ed esclusivamente in uscita per api e webhook dal mapper

    @Override
    public int compareTo(@NotNull TimelineElementInternal o) {
        int order = this.timestamp.compareTo(o.getTimestamp());
        if (order == 0)
            order = this.category.getPriority() - o.getCategory().getPriority();
        return order;
    }
}
