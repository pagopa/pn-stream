package it.pagopa.pn.stream.config.stats;

import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Data
public class StatsConfig {

    @Value("${pn.stream.stats.time-unit}")
    private StatsTimeUnit statsTimeUnit;

    @Value("${pn.stream.stats.span-unit}")
    private int statsSpanUnit;

    @Value("${pn.stream.stats.ttl}")
    private Duration ttl;
}
