package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.stream.dto.ConfidentialInformationEnum;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class TimeLineServiceImplTest {
    private TimeLineServiceImpl timeLineService;
    private ObjectMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new ObjectMapper();
        timeLineService = new TimeLineServiceImpl( mapper);
    }

    @Test
    void getTimeline() {

        String timelineId2 = "idTimeline2";
        String timelineId3 = "idTimeline3";

        Map<String, ConfidentialTimelineElementDtoInt> mapConfInf = new HashMap<>();
        ConfidentialTimelineElementDtoInt confInfDigital = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineId2)
                .digitalAddress("prova@prova.com")
                .build();
        ConfidentialTimelineElementDtoInt confInfPhysical = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId(timelineId3)
                .physicalAddress(
                        PhysicalAddressInt.builder()
                                .at("at")
                                .municipality("muni")
                                .province("NA")
                                .addressDetails("details")
                                .build()
                )
                .build();
        mapConfInf.put(confInfDigital.getTimelineElementId(), confInfDigital);
        mapConfInf.put(confInfPhysical.getTimelineElementId(), confInfPhysical);

        String physical = timeLineService.enrichTimelineElementWithConfidentialInformation("","{\"recIndex\":0,\"physicalAddress\":{\"zip\":\"87100\",\"foreignState\":\"ITALIA\"},\"nextSourceAttemptsMade\":0}", confInfPhysical);
        String digital = timeLineService.enrichTimelineElementWithConfidentialInformation("","{\"recIndex\":0,\"digitalAddress\":{\"type\":\"SERCQ\"}}", confInfDigital);

        Assertions.assertTrue(digital.contains("\"address\":\"prova@prova.com\""));
        Assertions.assertTrue(physical.contains("\"at\":\"at\""));
        Assertions.assertTrue(physical.contains("\"municipality\":\"muni\""));
        Assertions.assertTrue(physical.contains("\"province\":\"NA\""));
        Assertions.assertTrue(physical.contains("\"addressDetails\":\"details\""));
    }

    @Test
    void enrichTimelineElementWithConfidentialInformation_validDigitalAddress() {
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .digitalAddress("confidential@address.com")
                .build();
        String details = "{\"recIndex\":0,\"digitalAddress\":{\"type\":\"SERCQ\"}}";

        String result = timeLineService.enrichTimelineElementWithConfidentialInformation(null, details, confidentialDto);

        Assertions.assertTrue(result.contains("\"address\":\"confidential@address.com\""));
    }

    @Test
    void enrichTimelineElementWithConfidentialInformation_NORMALIZED_ADDRESS() {
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .newPhysicalAddress(PhysicalAddressInt.builder()
                        .at("at")
                        .municipality("muni")
                        .province("NA")
                        .addressDetails("details")
                        .build())
                .physicalAddress(PhysicalAddressInt.builder()
                        .at("at")
                        .municipality("muni")
                        .province("NA")
                        .addressDetails("details")
                        .build())
                .build();
        String details = "{\"normalizedAddress\":{\"address\":\"VIA SENZA NOME\",\"addressDetails\":\"SCALA B\",\"at\":\"Presso\",\"foreignState\":\"ITALIA\",\"municipality\":\"COSENZA\",\"municipalityDetails\":\"COSENZA\",\"province\":\"CS\",\"zip\":\"87100\"},\"oldAddress\":{\"address\":\"Via senza nome\",\"addressDetails\":\"scala b\",\"at\":\"Presso\",\"foreignState\":\"ITALIA\",\"municipality\":\"Cosenza\",\"municipalityDetails\":\"Cosenza\",\"province\":\"CS\",\"zip\":\"87100\"},\"recIndex\":0,\"newAddress\":{}}";

        String result = timeLineService.enrichTimelineElementWithConfidentialInformation(String.valueOf(ConfidentialInformationEnum.CustomCategory.NORMALIZED_ADDRESS), details, confidentialDto);

        Assertions.assertTrue(result.contains("\"normalizedAddress\":{\"fullname\":null,\"at\":\"at\",\"address\":null,\"addressDetails\":\"details\",\"zip\":null,\"municipality\":\"muni\",\"municipalityDetails\":null,\"province\":\"NA\",\"foreignState\":null}"));
        Assertions.assertTrue(result.contains("\"oldAddress\":{\"fullname\":null,\"at\":\"at\",\"address\":null,\"addressDetails\":\"details\",\"zip\":null,\"municipality\":\"muni\",\"municipalityDetails\":null,\"province\":\"NA\",\"foreignState\":null}"));
    }

    @Test
    void enrichTimelineElementWithConfidentialInformation_PREPARE_ANALOG_DOMICILE_FAILURE() {
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .physicalAddress(PhysicalAddressInt.builder()
                        .at("at")
                        .municipality("muni")
                        .province("NA")
                        .addressDetails("details")
                        .build())
                .build();
        String details = "{\"failureCause\":\"D02\",\"foundAddress\":{\"address\":\"VIA @FAIL-IRREPERIBILE_AR 55\",\"municipality\":\"Cosenza\",\"province\":\"CS\",\"zip\":\"87100\"},\"nextSourceAttemptsMade\":0,\"physicalAddress\":{\"zip\":\"87100\"},\"prepareRequestId\":\"PREPARE_ANALOG_DOMICILE.IUN_XPAL-RXRD-ADKT-202501-Y-1.RECINDEX_0.ATTEMPT_1\",\"recIndex\":0}";

        String result = timeLineService.enrichTimelineElementWithConfidentialInformation(String.valueOf(ConfidentialInformationEnum.CustomCategory.PREPARE_ANALOG_DOMICILE_FAILURE), details, confidentialDto);

        Assertions.assertTrue(result.contains("\"foundAddress\":{\"fullname\":null,\"at\":\"at\",\"address\":null,\"addressDetails\":\"details\",\"zip\":null,\"municipality\":\"muni\",\"municipalityDetails\":null,\"province\":\"NA\",\"foreignState\":null}"));
    }

    @Test
    void enrichTimelineElementWithConfidentialInformation_SEND_ANALOG_FEEDBACK() {
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .physicalAddress(PhysicalAddressInt.builder()
                        .at("at")
                        .municipality("muni")
                        .province("NA")
                        .addressDetails("details")
                        .build())
                .newPhysicalAddress(PhysicalAddressInt.builder()
                        .at("at")
                        .municipality("muni")
                        .province("NA")
                        .addressDetails("details")
                        .build())
                .build();
        String details = "{\"deliveryDetailCode\":\"RECAG001C\",\"notificationDate\":\"2025-01-23T11:35:27Z\",\"physicalAddress\":{\"address\":\"VIA SENZA NOME\",\"addressDetails\":\"SCALA B\",\"at\":\"Presso\",\"foreignState\":\"ITALIA\",\"municipality\":\"COSENZA\",\"municipalityDetails\":\"COSENZA\",\"province\":\"CS\",\"zip\":\"87100\"},\"recIndex\":0,\"registeredLetterCode\":\"346d3404cc6642889162d87775d4e9ad\",\"responseStatus\":\"OK\",\"sendRequestId\":\"SEND_ANALOG_DOMICILE.IUN_DHYG-XQHR-UJLQ-202501-N-1.RECINDEX_0.ATTEMPT_0\",\"sentAttemptMade\":0,\"serviceLevel\":\"REGISTERED_LETTER_890\"}";

        String result = timeLineService.enrichTimelineElementWithConfidentialInformation(String.valueOf(ConfidentialInformationEnum.CustomCategory.SEND_ANALOG_FEEDBACK), details, confidentialDto);

        Assertions.assertTrue(result.contains("\"physicalAddress\":{\"fullname\":null,\"at\":\"at\",\"address\":null,\"addressDetails\":\"details\",\"zip\":null,\"municipality\":\"muni\",\"municipalityDetails\":null,\"province\":\"NA\",\"foreignState\":null}"));
        Assertions.assertTrue(result.contains("\"newAddress\":{\"fullname\":null,\"at\":\"at\",\"address\":null,\"addressDetails\":\"details\",\"zip\":null,\"municipality\":\"muni\",\"municipalityDetails\":null,\"province\":\"NA\",\"foreignState\":null}"));
    }

    @Test
    void enrichTimelineElementWithConfidentialInformation_invalidJson() {
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .digitalAddress("confidential@address.com")
                .build();
        String details = "{invalidJson}";

        Assertions.assertThrows(PnStreamException.class, () -> {
            timeLineService.enrichTimelineElementWithConfidentialInformation(null, details, confidentialDto);
        });
    }

}
