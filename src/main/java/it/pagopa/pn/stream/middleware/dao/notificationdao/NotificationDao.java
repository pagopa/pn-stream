package it.pagopa.pn.stream.middleware.dao.notificationdao;

import it.pagopa.pn.stream.dto.notification.NotificationInternal;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity;
import reactor.core.publisher.Mono;

public interface NotificationDao {
    String IMPLEMENTATION_TYPE_PROPERTY_NAME = "pn.middleware.impl.notification";

    Mono<NotificationEntity> putItem(NotificationInternal notification);

    Mono<NotificationInternal> findByHashkey(String hashkey);

    Mono<NotificationEntity> getNotificationEntity(String hashkey);
}
