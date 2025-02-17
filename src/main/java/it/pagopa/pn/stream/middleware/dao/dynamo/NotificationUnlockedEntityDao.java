package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import reactor.core.publisher.Mono;

public interface NotificationUnlockedEntityDao {

    Mono<NotificationUnlockedEntity> findByPk(String pk);

    Mono<NotificationUnlockedEntity> putItem(NotificationUnlockedEntity entity);

}
