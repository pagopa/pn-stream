package it.pagopa.pn.stream.service;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamNotificationEntity;
import reactor.core.publisher.Mono;

public interface NotificationService {

    Mono<StreamNotificationEntity> constructNotificationEntity(String iun, CommunicationType communicationType);
}
