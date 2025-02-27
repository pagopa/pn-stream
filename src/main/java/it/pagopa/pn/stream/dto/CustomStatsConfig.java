package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import lombok.Data;

import java.util.Map;

@Data
public class CustomStatsConfig {
    private Map<StreamStatsEnum, StatConfig> config;
}

