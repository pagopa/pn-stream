package it.pagopa.pn.stream.dto.timeline;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactsIdV20;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TimelineElementInternal{
    private String iun;
    private String timelineElementId;
    private Instant timestamp;
    private String paId;
    private List<LegalFactsIdV20> legalFactId;
    private String category;
    private String details;
    private StatusInfoInternal statusInfo;
    private Instant notificationSentAt;
    private Instant businessTimestamp;
    private Instant ingestionTimestamp; //Questo campo viene valorizzato solo ed esclusivamente in uscita per api e webhook dal mapper
    private Instant eventTimestamp; //Questo campo viene valorizzato solo ed esclusivamente in uscita per api e webhook dal mapper
}
