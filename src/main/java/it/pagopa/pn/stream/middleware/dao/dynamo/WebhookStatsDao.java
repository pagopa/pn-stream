package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import reactor.core.publisher.Mono;

public interface WebhookStatsDao {
    Mono<WebhookStatsEntity> getItem(String pk, String sk);

    Mono<WebhookStatsEntity> updateItem(WebhookStatsEntity entity);

    Mono<WebhookStatsEntity> putItem (WebhookStatsEntity entity);
}
