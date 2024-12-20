package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnHttpResponseException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.NotificationViewedDetailsInt;
import it.pagopa.pn.stream.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.datavault.PnDataVaultClient;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.AddressDto;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.AnalogDomicile;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClient pnDataVaultClient;
    private PnDataVaultClientReactive pnDataVaultClientReactive;
    
    @BeforeEach
    void setup() {
        pnDataVaultClient = Mockito.mock( PnDataVaultClient.class );
        pnDataVaultClientReactive = Mockito.mock( PnDataVaultClientReactive.class );

        confidentialInformationService = new ConfidentialInformationServiceImpl(
                pnDataVaultClient,
                pnDataVaultClientReactive);

    }
    
    @Test
    void getTimelineConfidentialInformation() {
        //GIVEN
        String iun = "testIun";
        String elementId1 = "elementId1";
        String elementId2 = "elementId2";

        ConfidentialTimelineElementDto elementDto1 = ConfidentialTimelineElementDto.builder()
                .timelineElementId(elementId1)
                .digitalAddress(
                        AddressDto.builder()
                                .value("indirizzo@test.com")
                                .build()
                )
                .build();
        ConfidentialTimelineElementDto elementDto2 = ConfidentialTimelineElementDto.builder()
                .timelineElementId(elementId2)
                .newPhysicalAddress(AnalogDomicile.builder()
                        .cap("80010")
                        .province("NA")
                        .addressDetails("Scala 41")
                        .state("IT")
                        .municipality("MO")
                        .address("Via Vecchia")
                        .build())
                .build();
        List<ConfidentialTimelineElementDto> list = new ArrayList<>();
        list.add(elementDto1);
        list.add(elementDto2);
        

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenReturn(list);

        //WHEN
        Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);
                
        //THEN
        Assertions.assertTrue(mapOtp.isPresent());

        Assertions.assertNotNull(mapOtp.get().get(elementId1));
        Assertions.assertEquals(mapOtp.get().get(elementId1).getDigitalAddress(), elementDto1.getDigitalAddress().getValue());

        Assertions.assertNotNull(mapOtp.get().get(elementId2));
        Assertions.assertEquals(mapOtp.get().get(elementId2).getNewPhysicalAddress().getAddress(), elementDto2.getNewPhysicalAddress().getAddress());
    }
    
    @Test
    void getTimelineConfidentialInformationKo() {
        //GIVEN
        String iun = "testIun";

        Mockito.when(pnDataVaultClient.getNotificationTimelineByIunWithHttpInfo(Mockito.anyString()))
                .thenThrow(PnHttpResponseException.class);

        //WHEN
        assertThrows(PnHttpResponseException.class, () -> {
            confidentialInformationService.getTimelineConfidentialInformation(iun);
        });
    }
    
    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {
         SendAnalogDetailsInt details =  SendAnalogDetailsInt.builder()
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .relatedRequestId("abc")
                .analogCost(100)
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
         
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details( details )
                .build();
    }

    private TimelineElementInternal notificationViewedDetails(String iun, String elementId) {
        NotificationViewedDetailsInt details =  NotificationViewedDetailsInt.builder()
                .notificationCost(100)
                .recIndex(0)
                .raddTransactionId("154")
                .delegateInfo(DelegateInfoInt.builder()
                        .internalId("idInterno")
                        .denomination("test")
                        .taxId("prova")
                        .build())
                .build();

        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .details( details )
                .build();
    }

    @Test
    void getTimelineConfidentialInformationWithTimeline() {
        TimelineElementInternal timelineElementInternal = getSendPaperDetailsTimelineElement("iun", "elementId");
        timelineElementInternal.setCategory(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setStatusInfo(StatusInfoInternal.builder().build());
        timelineElementInternal.setLegalFactsIds(List.of(LegalFactsIdInt.builder().build()));

        ConfidentialTimelineElementDto confidentialTimelineElementDto = new ConfidentialTimelineElementDto();
        confidentialTimelineElementDto.setTaxId("taxId");
        confidentialTimelineElementDto.setDenomination("denomination");
        confidentialTimelineElementDto.setTimelineElementId("timelineElementId");
        confidentialTimelineElementDto.setDigitalAddress(AddressDto.builder().value("via addressDto").build());
        AnalogDomicile analogDomicile = AnalogDomicile.builder()
                .at("at")
                .address("via address")
                .municipality("municipality")
                .build();
        confidentialTimelineElementDto.setPhysicalAddress(analogDomicile);

        Mockito.when(pnDataVaultClientReactive.getNotificationTimelines(Mockito.any()))
                .thenReturn(Flux.just(confidentialTimelineElementDto));

        Flux<ConfidentialTimelineElementDtoInt> fluxDto = confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal));
        Assertions.assertNotNull(fluxDto);

        ConfidentialTimelineElementDtoInt dto = fluxDto.blockFirst();
        Assertions.assertEquals("denomination", dto.getDenomination());
        Assertions.assertEquals("timelineElementId", dto.getTimelineElementId());
        Assertions.assertEquals("taxId", dto.getTaxId());
        Assertions.assertEquals("via addressDto", dto.getDigitalAddress());
        Assertions.assertEquals(analogDomicile.getAddress(), dto.getPhysicalAddress().getAddress());
        Assertions.assertEquals(analogDomicile.getAt(), dto.getPhysicalAddress().getAt());
        Assertions.assertEquals(analogDomicile.getMunicipality(), dto.getPhysicalAddress().getMunicipality());
    }

    @Test
    void getTimelineConfidentialInformationWithTimelineKo() {
        TimelineElementInternal timelineElementInternal = getSendPaperDetailsTimelineElement("iun", "elementId");
        timelineElementInternal.setCategory(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setStatusInfo(StatusInfoInternal.builder().build());
        timelineElementInternal.setLegalFactsIds(List.of(LegalFactsIdInt.builder().build()));

        Mockito.when(pnDataVaultClientReactive.getNotificationTimelines(Mockito.any())).thenThrow(PnInternalException.class);

        assertThrows(PnInternalException.class, () -> confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal)).blockFirst());
    }
}