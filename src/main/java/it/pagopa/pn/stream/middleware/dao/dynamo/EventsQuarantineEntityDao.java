package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

public interface EventsQuarantineEntityDao {

    Mono<Page<EventsQuarantineEntity>> findByPk(String pk);

    Mono<EventsQuarantineEntity> putItem(EventsQuarantineEntity streamNotification);

    Mono<Void> saveAndClearElement(EventsQuarantineEntity entity, EventEntity eventEntity);

}
