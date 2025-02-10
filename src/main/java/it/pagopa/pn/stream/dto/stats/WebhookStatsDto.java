package it.pagopa.pn.stream.dto.stats;

import lombok.*;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Setter
@Getter
@ToString
public class WebhookStatsDto {
    private String paId;
    private String streamId;
    private String statsType;
    private String sk;
    private String spanUnit;
    private TimeUnitEnum timeUnit;
    private Number value;
    private Number ttl;
}
