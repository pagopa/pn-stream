package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;

public interface StreamStatsService {
    void updateStreamStats(StreamStatsEnum streamStatsEnum, String paId, String streamId);
}
