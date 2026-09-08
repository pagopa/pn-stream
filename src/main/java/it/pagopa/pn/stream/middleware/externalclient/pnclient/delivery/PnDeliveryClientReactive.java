package it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.InformalSentNotificationV1;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV26;
import reactor.core.publisher.Mono;

public interface PnDeliveryClientReactive {
    String CLIENT_NAME = PnLogger.EXTERNAL_SERVICES.PN_DELIVERY;
    String GET_NOTIFICATION = "GET NOTIFICATION";
    String GET_INFORMAL_NOTIFICATION = "GET INFORMAL NOTIFICATION";

    Mono<SentNotificationV26> getSentNotification(String iun);
    Mono<InformalSentNotificationV1> getSentInformalNotificationPrivateV1(String iun, Boolean retrieveMessage);
}
