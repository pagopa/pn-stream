package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import reactor.core.publisher.Mono;

public interface StreamStatsService {
    Mono<Void> updateStreamStats(StreamStatsEnum streamStatsEnum, String paId, String streamId);
    Mono<Void> updateNumberOfReadingStreamStats(String paId, String streamId, Integer increment);
}
