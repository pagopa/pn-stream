package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.Key;

public interface WebhookStatsDao {
    Mono<WebhookStatsEntity> getItem(Key key);

    Mono<WebhookStatsEntity> updateAtomicCounterStats(WebhookStatsEntity entity);

    Mono<WebhookStatsEntity> updateCustomCounterStats(String pk, String sk, String increment);

}
