package it.pagopa.pn.stream.dto.timeline;

import lombok.*;

import java.time.Instant;

@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class StatusInfoInternal {

    private final String actual;
    private final Instant statusChangeTimestamp;
    private final boolean statusChanged;
}
