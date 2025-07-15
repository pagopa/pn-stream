package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.AddressDto;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.AnalogDomicile;
import it.pagopa.pn.stream.generated.openapi.msclient.datavault.model.ConfidentialTimelineElementDto;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactsIdV28;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.datavault.PnDataVaultClientReactive;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidentialInformationServiceImplTest {
    private ConfidentialInformationService confidentialInformationService;
    private PnDataVaultClientReactive pnDataVaultClientReactive;
    
    @BeforeEach
    void setup() {
        pnDataVaultClientReactive = Mockito.mock( PnDataVaultClientReactive.class );
        confidentialInformationService = new ConfidentialInformationServiceImpl(
                pnDataVaultClientReactive);

    }

    private TimelineElementInternal getSendPaperDetailsTimelineElement(String iun, String elementId) {

        return TimelineElementInternal.builder()
                .timelineElementId(elementId)
                .iun(iun)
                .details( "{\"recIndex\":0,\"physicalAddress\":{\"zip\":\"87100\",\"foreignState\":\"ITALIA\"},\"nextSourceAttemptsMade\":0}" )
                .build();
    }

    @Test
    void getTimelineConfidentialInformationWithTimeline()  {
        TimelineElementInternal timelineElementInternal = getSendPaperDetailsTimelineElement("iun", "elementId");
        timelineElementInternal.setCategory("REQUEST_ACCEPTED");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setStatusInfo(StatusInfoInternal.builder().build());
        timelineElementInternal.setLegalFactId(List.of(new LegalFactsIdV28()));

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
        timelineElementInternal.setCategory("REQUEST_ACCEPTED");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setStatusInfo(StatusInfoInternal.builder().build());
        timelineElementInternal.setLegalFactId(List.of(new LegalFactsIdV28()));

        Mockito.when(pnDataVaultClientReactive.getNotificationTimelines(Mockito.any())).thenThrow(PnInternalException.class);

        assertThrows(PnInternalException.class, () -> confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal)).blockFirst());
    }
}