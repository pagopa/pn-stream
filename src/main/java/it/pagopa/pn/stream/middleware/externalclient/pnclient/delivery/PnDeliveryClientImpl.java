package it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery;

import it.pagopa.pn.stream.generated.openapi.msclient.delivery.api.InternalOnlyApi;
import it.pagopa.pn.stream.generated.openapi.msclient.delivery.model.RequestUpdateStatusDto;
import it.pagopa.pn.stream.generated.openapi.msclient.delivery.model.SentNotificationV24;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@CustomLog
@RequiredArgsConstructor
@Component
public class PnDeliveryClientImpl implements PnDeliveryClient{
    private final InternalOnlyApi pnDeliveryApi;
    
    @Override
    public void updateStatus(RequestUpdateStatusDto dto) {
        log.logInvokingExternalService(CLIENT_NAME, UPDATE_STATUS);
        pnDeliveryApi.updateStatusWithHttpInfo(dto);
    }

    @Override
    public SentNotificationV24 getSentNotification(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_NOTIFICATION);

        ResponseEntity<SentNotificationV24> res = pnDeliveryApi.getSentNotificationPrivateWithHttpInfo(iun);
        
        return res.getBody();
    }
    
    @Override
    public Map<String, String>  getQuickAccessLinkTokensPrivate(String iun) {
        log.logInvokingExternalService(CLIENT_NAME, GET_QUICK_ACCESS_TOKEN);
        ResponseEntity<Map<String, String>> res = pnDeliveryApi.getQuickAccessLinkTokensPrivateWithHttpInfo(iun);

        return res.getBody();
    }
}