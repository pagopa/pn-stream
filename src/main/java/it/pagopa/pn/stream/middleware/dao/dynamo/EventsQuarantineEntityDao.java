package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public interface EventsQuarantineEntityDao {

    Mono<Page<EventsQuarantineEntity>> findByPk(String pk, Map<String, AttributeValue> lastEvaluateKey, int limit);

    Mono<EventsQuarantineEntity> putItem(EventsQuarantineEntity streamNotification);

    Mono<Void> saveAndClearElement(EventsQuarantineEntity entity, EventEntity eventEntity);

}
