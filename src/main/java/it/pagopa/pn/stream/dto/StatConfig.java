package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import lombok.Data;

@Data
public class StatConfig {
    private StatsTimeUnit timeUnit;
    private Integer spanUnit;
    private String ttl;
}
