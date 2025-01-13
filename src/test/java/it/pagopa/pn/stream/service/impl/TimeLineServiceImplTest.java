package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.stream.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.stream.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.*;
import it.pagopa.pn.stream.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

class TimeLineServiceImplTest {
    private TimelineDao timelineDao;
    private TimeLineServiceImpl timeLineService;
    private ConfidentialInformationService confidentialInformationService;

    @BeforeEach
    void setup() {
        timelineDao = Mockito.mock( TimelineDao.class );
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        confidentialInformationService = Mockito.mock( ConfidentialInformationService.class );
        timeLineService = new TimeLineServiceImpl(mapper, timelineDao, confidentialInformationService);

    }

    @Test
    void getTimeline(){
        //GIVEN
        String iun = "iun";
        
        String timelineId1 = "idTimeline1";
        TimelineElementInternal scheduleAnalogNoConfInf = getScheduleAnalogWorkflowTimelineElement(iun, timelineId1);
        String timelineId2 = "idTimeline2";
        TimelineElementInternal sendDigitalConfInf = getSendDigitalTimelineElement(iun, timelineId2);
        String timelineId3 = "idTimeline3";
        TimelineElementInternal sendPaperFeedbackConfInf = getSendPaperFeedbackTimelineElement(iun, timelineId3, Instant.now());

        List<TimelineElementInternal> timelineElementList = new ArrayList<>();
        timelineElementList.add(scheduleAnalogNoConfInf);
        timelineElementList.add(sendDigitalConfInf);
        timelineElementList.add(sendPaperFeedbackConfInf);

        HashSet<TimelineElementInternal> hashSet = new HashSet<>(timelineElementList);
        Mockito.when(timelineDao.getTimeline(Mockito.anyString()))
                .thenReturn(hashSet);

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

        Mockito.when(confidentialInformationService.getTimelineConfidentialInformation(Mockito.anyString()))
                .thenReturn(Optional.of(mapConfInf));
        
        //WHEN
        Set<TimelineElementInternal> retrievedElements = timeLineService.getTimeline(iun, true);

        //THEN
        Assertions.assertFalse(retrievedElements.isEmpty());

        List<TimelineElementInternal> listElement = new ArrayList<>(retrievedElements);

        TimelineElementInternal retrievedScheduleAnalog = getSpecificElementFromList(listElement, scheduleAnalogNoConfInf.getElementId());
        Assertions.assertEquals(retrievedScheduleAnalog , scheduleAnalogNoConfInf);

        TimelineElementInternal retrievedSendDigital = getSpecificElementFromList(listElement, sendDigitalConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendDigital);
        
        SendDigitalDetailsInt details = (SendDigitalDetailsInt) retrievedSendDigital.getDetails();
        Assertions.assertEquals(details, sendDigitalConfInf.getDetails());
        Assertions.assertEquals(details.getDigitalAddress().getAddress() , confInfDigital.getDigitalAddress());

        TimelineElementInternal retrievedSendPaperFeedback = getSpecificElementFromList(listElement, sendPaperFeedbackConfInf.getElementId());
        Assertions.assertNotNull(retrievedSendPaperFeedback);
        
        SendAnalogFeedbackDetailsInt details1 = (SendAnalogFeedbackDetailsInt) retrievedSendPaperFeedback.getDetails();
        Assertions.assertEquals(details1, sendPaperFeedbackConfInf.getDetails());
        Assertions.assertEquals(details1.getPhysicalAddress() , confInfPhysical.getPhysicalAddress());
    }

    private TimelineElementInternal getSpecificElementFromList(List<TimelineElementInternal> listElement, String timelineId){
        for (TimelineElementInternal element : listElement){
            if(element.getElementId().equals(timelineId)){
                return element;
            }
        }
        return null;
    }
    
    private TimelineElementInternal getSendDigitalTimelineElement(String iun, String timelineId) {
        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                .build()
                )
                .recIndex(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(timelineId)
                .iun(iun)
                .details( details )
                .build();
    }


    private TimelineElementInternal getSendPaperFeedbackTimelineElement(String iun, String elementId, Instant timestamp) {
         SendAnalogFeedbackDetailsInt details =  SendAnalogFeedbackDetailsInt.builder()
                 .notificationDate(timestamp)
                .newAddress(
                        PhysicalAddressInt.builder()
                                .province("province")
                                .municipality("munic")
                                .at("at")
                                .build()
                )
                .recIndex(0)
                .sentAttemptMade(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(elementId)
                .iun(iun)
                .category(TimelineElementCategoryInt.SEND_ANALOG_FEEDBACK)
                .timestamp(timestamp)
                .details( details )
                .build();
    }

    
    private TimelineElementInternal getScheduleAnalogWorkflowTimelineElement(String iun, String timelineId) {
        ScheduleAnalogWorkflowDetailsInt details = ScheduleAnalogWorkflowDetailsInt.builder()
                .recIndex(0)
                .build();
        return TimelineElementInternal.builder()
                .elementId(timelineId)
                .iun(iun)
                .details( details )
                .build();
    }

    @Test
    void enrichTimelineElementWithConfidentialInformationForSendCourtesyMessageDetailsInt() {
        // Given
        SendCourtesyMessageDetailsInt details = SendCourtesyMessageDetailsInt.builder()
                .digitalAddress(CourtesyDigitalAddressInt.builder().build())
                .build();
        ConfidentialTimelineElementDtoInt confidentialDto = ConfidentialTimelineElementDtoInt.builder()
                .digitalAddress("confidential@address.com")
                .build();

        // When
        details = timeLineService.enrichTimelineElementWithConfidentialInformation(details, confidentialDto);

        // Then
        Assertions.assertNotNull(details.getDigitalAddress());
        Assertions.assertEquals("confidential@address.com", details.getDigitalAddress().getAddress());
    }
}