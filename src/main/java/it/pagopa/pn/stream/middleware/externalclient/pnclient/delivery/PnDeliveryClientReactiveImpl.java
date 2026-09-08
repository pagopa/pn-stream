package it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.InformalSentNotificationV1;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV26;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery_reactive.api.InternalOnlyApi;
import it.pagopa.pn.stream.exceptions.PnStreamNotFoundException;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_STREAM_NOTIFICATIONFAILED;

@Component
@RequiredArgsConstructor
@CustomLog
public class PnDeliveryClientReactiveImpl extends CommonBaseClient implements PnDeliveryClientReactive{
    private final InternalOnlyApi pnDeliveryApi;
    
    @Override
    public Mono<SentNotificationV26> getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);
        
        return pnDeliveryApi.getSentNotificationPrivate(iun)
                .onErrorResume( error -> {
                    log.error("Get notification error ={} - iun {}", error,  iun);
                    if (error instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode() == HttpStatus.NOT_FOUND)
                        {
                            return Mono.error(new PnStreamNotFoundException("Notification not found"));
                        }
                    return Mono.error(new PnInternalException("Get notification error - iun " + iun, ERROR_CODE_STREAM_NOTIFICATIONFAILED, error));
                })
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_NOTIFICATION));
    }

    @Override
    public Mono<InformalSentNotificationV1> getSentInformalNotificationPrivateV1(String iun, Boolean retrieveMessage) {
        log.logInvokingExternalService(CLIENT_NAME, GET_INFORMAL_NOTIFICATION);

        return pnDeliveryApi.getSentInformalNotificationPrivateV1(iun, retrieveMessage)
                .onErrorResume( error -> {
                    log.error("Get informal notification error ={} - iun {}", error,  iun);
                    if (error instanceof WebClientResponseException webClientResponseException && webClientResponseException.getStatusCode() == HttpStatus.NOT_FOUND)
                    {
                        return Mono.error(new PnStreamNotFoundException("Informal Notification not found"));
                    }
                    return Mono.error(new PnInternalException("Get informal notification error - iun " + iun, ERROR_CODE_STREAM_NOTIFICATIONFAILED, error));
                })
                .doOnSuccess(res -> log.debug("Received sync response from {} for {}", CLIENT_NAME, GET_INFORMAL_NOTIFICATION));
    }

}
