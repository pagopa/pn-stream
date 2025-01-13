package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.details.SendCourtesyMessageDetailsInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeLineServiceImplTest {
    private TimeLineServiceImpl timeLineService;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        timeLineService = new TimeLineServiceImpl(mapper);
    }

    @Test
    void enrichTimelineElementWithConfidentialInformationForSendCourtesyMessageDetailsInt() throws JsonProcessingException {
        // Given
        SendCourtesyMessageDetailsInt details = SendCourtesyMessageDetailsInt.builder()
                .digitalAddress(CourtesyDigitalAddressInt.builder().build())
                .build();
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .digitalAddress("confidential@address.com")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String detailsString = mapper.writeValueAsString(details);

        // When
        detailsString = timeLineService.enrichTimelineElementWithConfidentialInformation(detailsString, confidentialDto);

        details = mapper.readValue(detailsString, SendCourtesyMessageDetailsInt.class);

        // Then
        Assertions.assertNotNull(details.getDigitalAddress());
        Assertions.assertEquals("confidential@address.com", details.getDigitalAddress().getAddress());
    }
}