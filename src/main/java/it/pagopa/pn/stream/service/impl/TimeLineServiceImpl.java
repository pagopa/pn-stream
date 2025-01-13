package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.stream.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.stream.middleware.dao.timelinedao.TimelineDao;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static java.util.Map.entry;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeLineServiceImpl implements TimelineService {

    private final ObjectMapper mapper;
    private final String LOG_MSG_DEANONIMIZATION = "Error during deanonimization";

    private final TimelineDao timelineDao;
    private final ConfidentialInformationService confidentialInformationService;
    private final Map<String, String> DEANONIMIZATION_PROPERTIES = Map.ofEntries(
            entry("taxId", "taxId"),
            entry("denomination", "denomination"),
            entry("normalizedAddress", "newPhysicalAddress"),
            entry("newAddress", "newPhysicalAddress"),
            entry("physicalAddress", "physicalAddress"),
            entry("oldAddress", "physicalAddress"),
            entry("foundAddress", "physicalAddress"),
            entry("digitalAddress/address", "digitalAddress")
    );

    private final HashMap<String, String> confidentialInfo = new HashMap<>();
    @Override
    public <T extends TimelineElementDetailsInt> T enrichTimelineElementWithConfidentialInformation(TimelineElementDetailsInt details, ConfidentialTimelineElementDtoInt confidentialDto) {
//        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
//        JsonNode detailsJson = mapper.convertValue(details, JsonNode.class);
//        JsonNode confidentialJson = mapper.convertValue(confidentialDto, JsonNode.class);

        ObjectNode detailsJson = mapper.valueToTree(details);
        ObjectNode confidentialJson = mapper.valueToTree(confidentialDto);

        DEANONIMIZATION_PROPERTIES.entrySet().forEach(
                entry -> {
                    if (Objects.nonNull(detailsJson.path(entry.getKey())) && Objects.nonNull(confidentialJson.get(entry.getValue()))) {
                        detailsJson.put(entry.getKey(), confidentialJson.get(entry.getValue()));
                    }
                }
        );

        try {
            return (T) mapper.readValue(detailsJson.toString(), details.getClass());
        } catch (JsonProcessingException e) {
            throw new PnInternalException(LOG_MSG_DEANONIMIZATION, ERROR_CODE_PN_GENERIC_ERROR);
        }


//        if (details instanceof CourtesyAddressRelatedTimelineElement courtesyAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {
//            CourtesyDigitalAddressInt address = courtesyAddressRelatedTimelineElement.getDigitalAddress();
//
//            address = getCourtesyDigitalAddress(confidentialDto, address);
//            ((CourtesyAddressRelatedTimelineElement) details).setDigitalAddress(address);
//        }
//
//        if (details instanceof DigitalAddressRelatedTimelineElement digitalAddressRelatedTimelineElement && confidentialDto.getDigitalAddress() != null) {
//
//            LegalDigitalAddressInt address = digitalAddressRelatedTimelineElement.getDigitalAddress();
//
//            address = getDigitalAddress(confidentialDto, address);
//
//            ((DigitalAddressRelatedTimelineElement) details).setDigitalAddress(address);
//        }
//
//        if (details instanceof PhysicalAddressRelatedTimelineElement physicalAddressRelatedTimelineElement && confidentialDto.getPhysicalAddress() != null) {
//            PhysicalAddressInt physicalAddress = physicalAddressRelatedTimelineElement.getPhysicalAddress();
//
//            physicalAddress = getPhysicalAddress(physicalAddress, confidentialDto.getPhysicalAddress());
//
//            ((PhysicalAddressRelatedTimelineElement) details).setPhysicalAddress(physicalAddress);
//        }
//
//        if (details instanceof NewAddressRelatedTimelineElement newAddressRelatedTimelineElement && confidentialDto.getNewPhysicalAddress() != null) {
//
//            PhysicalAddressInt newAddress = newAddressRelatedTimelineElement.getNewAddress();
//
//            newAddress = getPhysicalAddress(newAddress, confidentialDto.getNewPhysicalAddress());
//
//            ((NewAddressRelatedTimelineElement) details).setNewAddress(newAddress);
//        }
//
//        if (details instanceof PersonalInformationRelatedTimelineElement personalInformationRelatedTimelineElement) {
//            personalInformationRelatedTimelineElement.setTaxId(confidentialDto.getTaxId());
//            personalInformationRelatedTimelineElement.setDenomination(confidentialDto.getDenomination());
//        }
    }

    private LegalDigitalAddressInt getDigitalAddress(ConfidentialTimelineElementDtoInt confidentialDto, LegalDigitalAddressInt address) {
        if (address == null) {
            address = LegalDigitalAddressInt.builder().build();
        }

        address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
        return address;
    }

    private CourtesyDigitalAddressInt getCourtesyDigitalAddress(ConfidentialTimelineElementDtoInt confidentialDto, CourtesyDigitalAddressInt address) {
        if (address == null) {
            address = CourtesyDigitalAddressInt.builder().build();
        }

        address = address.toBuilder().address(confidentialDto.getDigitalAddress()).build();
        return address;
    }

    private PhysicalAddressInt getPhysicalAddress(PhysicalAddressInt physicalAddress, PhysicalAddressInt physicalAddress2) {
        if (physicalAddress == null) {
            physicalAddress = PhysicalAddressInt.builder().build();
        }

        return physicalAddress.toBuilder()
                .at(physicalAddress2.getAt())
                .address(physicalAddress2.getAddress())
                .municipality(physicalAddress2.getMunicipality())
                .province(physicalAddress2.getProvince())
                .addressDetails(physicalAddress2.getAddressDetails())
                .zip(physicalAddress2.getZip())
                .municipalityDetails(physicalAddress2.getMunicipalityDetails())
                .foreignState(physicalAddress2.getForeignState())
                .build();
    }

    @Override
    public Set<TimelineElementInternal> getTimelineByIunTimelineId(String iun, String timelineId, boolean confidentialInfoRequired) {
        log.debug("getTimelineByIunTimelineId - iun={} timelineId={}", iun, timelineId);
        Set<TimelineElementInternal> setTimelineElements =  this.timelineDao.getTimelineFilteredByElementId(iun, timelineId);
        setConfidentialInfo(confidentialInfoRequired, iun, setTimelineElements);
        return setTimelineElements;
    }

    private void setConfidentialInfo(boolean confidentialInfoRequired, String iun, Set<TimelineElementInternal> setTimelineElements) {
        if (confidentialInfoRequired) {
            Optional<Map<String, ConfidentialTimelineElementDtoInt>> mapConfOtp;
            mapConfOtp = confidentialInformationService.getTimelineConfidentialInformation(iun);

            if (mapConfOtp.isPresent()) {
                Map<String, ConfidentialTimelineElementDtoInt> mapConf = mapConfOtp.get();

                setTimelineElements.forEach(
                        timelineElementInt -> {
                            ConfidentialTimelineElementDtoInt dtoInt = mapConf.get(timelineElementInt.getElementId());
                            if (dtoInt != null) {
                                timelineElementInt.setDetails(enrichTimelineElementWithConfidentialInformation(timelineElementInt.getDetails(), dtoInt));
                            }
                        }
                );
            }
        }
    }

    @Override
    public Set<TimelineElementInternal> getTimeline(String iun, boolean confidentialInfoRequired) {
        log.debug("GetTimeline - iun={} ", iun);
        Set<TimelineElementInternal> setTimelineElements = this.timelineDao.getTimeline(iun);
        setConfidentialInfo(confidentialInfoRequired, iun, setTimelineElements);
        return setTimelineElements;
    }
}
