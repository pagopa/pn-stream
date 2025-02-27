package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.time.Duration;

public interface StreamStatsDao {
    Mono<StreamStatsEntity> getItem(Key key);

    Mono<StreamStatsEntity> updateAtomicCounterStats(StreamStatsEntity entity);

    Mono<UpdateItemResponse> updateCustomCounterStats(String pk, String sk, Integer increment, Duration ttlDuration);

}
