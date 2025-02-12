package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventsQuarantineEntityDao {

    Flux<EventsQuarantineEntity> findByPk(String pk);

    Mono<EventsQuarantineEntity> putItem(EventsQuarantineEntity streamNotification);

    Mono<Void> saveAndClearElement(EventsQuarantineEntity entity, EventEntity eventEntity);

}
