package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.InformalSentNotificationV1;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV26;
import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationServiceImplTest {
    private PnDeliveryClientReactive pnDeliveryClientReactive;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        pnDeliveryClientReactive = Mockito.mock(PnDeliveryClientReactive.class);
        notificationService = new NotificationServiceImpl(pnDeliveryClientReactive);
    }

    @Test
    void shouldCallLegalApiWhenCommunicationTypeIsNull() {
        String iun = "IUN-NULL";
        Instant sentAt = Instant.now();
        SentNotificationV26 legal = new SentNotificationV26();
        legal.setIun(iun);
        legal.setGroup("LEGAL_GROUP");
        legal.setSentAt(sentAt);

        when(pnDeliveryClientReactive.getSentNotification(iun)).thenReturn(Mono.just(legal));

        StepVerifier.create(notificationService.constructNotificationEntity(iun, null))
                .assertNext(entity -> {
                    assertEquals(iun, entity.getHashKey());
                    assertEquals("LEGAL_GROUP", entity.getGroup());
                    assertEquals(sentAt, entity.getCreationDate());
                })
                .verifyComplete();

        verify(pnDeliveryClientReactive, times(1)).getSentNotification(iun);
        verify(pnDeliveryClientReactive, never()).getSentInformalNotificationPrivateV1(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    void shouldCallLegalApiWhenCommunicationTypeIsLegal() {
        String iun = "IUN-LEGAL";
        Instant sentAt = Instant.now();
        SentNotificationV26 legal = new SentNotificationV26();
        legal.setIun(iun);
        legal.setGroup("GROUP_LEGAL");
        legal.setSentAt(sentAt);

        when(pnDeliveryClientReactive.getSentNotification(iun)).thenReturn(Mono.just(legal));

        StepVerifier.create(notificationService.constructNotificationEntity(iun, CommunicationType.LEGAL))
                .assertNext(entity -> {
                    assertEquals(iun, entity.getHashKey());
                    assertEquals("GROUP_LEGAL", entity.getGroup());
                    assertEquals(sentAt, entity.getCreationDate());
                })
                .verifyComplete();

        verify(pnDeliveryClientReactive, times(1)).getSentNotification(iun);
        verify(pnDeliveryClientReactive, never()).getSentInformalNotificationPrivateV1(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    void shouldCallInformalApiWhenCommunicationTypeIsInformal() {
        String iun = "IUN-INFORMAL";
        Instant sentAt = Instant.now();
        InformalSentNotificationV1 informal = new InformalSentNotificationV1();
        informal.setIun(iun);
        informal.setGroup("GROUP_INFORMAL");
        informal.setSentAt(sentAt);

        when(pnDeliveryClientReactive.getSentInformalNotificationPrivateV1(iun, true)).thenReturn(Mono.just(informal));

        StepVerifier.create(notificationService.constructNotificationEntity(iun, CommunicationType.INFORMAL))
                .assertNext(entity -> {
                    assertEquals(iun, entity.getHashKey());
                    assertEquals("GROUP_INFORMAL", entity.getGroup());
                    assertEquals(sentAt, entity.getCreationDate());
                })
                .verifyComplete();

        verify(pnDeliveryClientReactive, times(1)).getSentInformalNotificationPrivateV1(iun, true);
        verify(pnDeliveryClientReactive, never()).getSentNotification(iun);
    }

}