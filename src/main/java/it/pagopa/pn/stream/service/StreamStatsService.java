package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.CustomStatsConfig;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

public interface StreamStatsService {
    Mono<StreamStatsEntity> updateStreamStats(CustomStatsConfig customStatsConfig, StreamStatsEnum streamStatsEnum, String paId, String streamId);

    Mono<UpdateItemResponse> updateNumberOfReadingStreamStats(CustomStatsConfig customStatsConfig, String paId, String streamId, Integer increment);
}
