package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.dto.stats.WebhookStatsDto;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import reactor.core.publisher.Mono;

public interface WebhookStatsDao {
    Mono<Void> putItemIfAbsent(WebhookStatsDto webhookStats);

    Mono<WebhookStatsEntity> getItem(WebhookStatsEntity webhookStats);
}
