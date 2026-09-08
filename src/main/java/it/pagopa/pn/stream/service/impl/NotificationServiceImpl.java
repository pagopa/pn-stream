package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamNotificationEntity;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.stream.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final PnDeliveryClientReactive pnDeliveryClientReactive;

    @Override
    public Mono<StreamNotificationEntity> constructNotificationEntity(String iun, CommunicationType communicationType) {
        log.info("Construct notification entity for IUN={} and communicationType={}", iun, communicationType);

        if (communicationType == CommunicationType.INFORMAL) {
            return pnDeliveryClientReactive.getSentInformalNotificationPrivateV1(iun, true)
                    .map(informalNotification -> {
                        StreamNotificationEntity streamNotificationEntity = new StreamNotificationEntity();
                        streamNotificationEntity.setHashKey(informalNotification.getIun());
                        streamNotificationEntity.setGroup(informalNotification.getGroup());
                        streamNotificationEntity.setCreationDate(informalNotification.getSentAt());
                        return streamNotificationEntity;
                    });
        }

        return pnDeliveryClientReactive.getSentNotification(iun)
                .map(legalNotification -> {
                    StreamNotificationEntity streamNotificationEntity = new StreamNotificationEntity();
                    streamNotificationEntity.setHashKey(legalNotification.getIun());
                    streamNotificationEntity.setGroup(legalNotification.getGroup());
                    streamNotificationEntity.setCreationDate(legalNotification.getSentAt());
                    return streamNotificationEntity;
                });
    }
}