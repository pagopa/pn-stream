package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.stream.dto.ConfidentialInformationEnum;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

import static it.pagopa.pn.stream.dto.ConfidentialInformationEnum.*;
import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_EVENT_CONFIDENTIAL_INFORMATION;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeLineServiceImpl implements TimelineService {

    private final ObjectMapper mapper;
    private static final Map<String, BinaryOperator<ObjectNode>> categoryFunctions = new HashMap<>();

    static {
        categoryFunctions.put(ConfidentialInformationEnum.CustomCategory.NORMALIZED_ADDRESS.name(), TimeLineServiceImpl::applyNormalizedAddress);
        categoryFunctions.put(ConfidentialInformationEnum.CustomCategory.SEND_ANALOG_FEEDBACK.name(), TimeLineServiceImpl::applySendAnalogFeedback);
        categoryFunctions.put(ConfidentialInformationEnum.CustomCategory.PREPARE_ANALOG_DOMICILE_FAILURE.name(), TimeLineServiceImpl::applyPrepareAnalogDomicileFailure);
        categoryFunctions.put("default", TimeLineServiceImpl::applyDefaultAddress);
    }

    @Override
    public String enrichTimelineElementWithConfidentialInformation(String category, String details, ConfidentialTimelineElementDtoInt confidentialDto) {

        try {
            var detailsJson = mapper.readValue(details, ObjectNode.class);
            ObjectNode confidentialJson = mapper.valueToTree(confidentialDto);

            Arrays.stream(ConfidentialInformationEnum.values())
                    .forEach(confEnum -> {
                        ObjectNode targetNode = detailsJson;
                         if (StringUtils.hasText(confEnum.getParent()) && detailsJson.has(confEnum.getParent())) {
                                targetNode = (ObjectNode) detailsJson.get(confEnum.getParent());
                                targetNode.set(confEnum.getTimelineKey(), confidentialJson.get(confEnum.getConfidentialValue()));
                        } else {
                             BinaryOperator<ObjectNode> function = categoryFunctions.getOrDefault(category, categoryFunctions.get("default"));
                             function.apply(targetNode, confidentialJson);
                    }});

            return detailsJson.toString();
        } catch (JsonProcessingException e) {
            throw new PnStreamException(e.getMessage(), 500, ERROR_CODE_EVENT_CONFIDENTIAL_INFORMATION);
        }
    }

    private static ObjectNode applyNormalizedAddress(ObjectNode targetNode, ObjectNode confidentialJson) {
        targetNode.set(OLD_ADDRESS.getEventValue(), confidentialJson.get(OLD_ADDRESS.getConfidentialValue()));
        targetNode.set(NORMALIZED_ADDRESS.getEventValue(), confidentialJson.get(NORMALIZED_ADDRESS.getConfidentialValue()));
        targetNode.remove(OLD_ADDRESS.getTimelineKey());
        targetNode.remove(NORMALIZED_ADDRESS.getTimelineKey());
        return targetNode;
    }

    private static ObjectNode applySendAnalogFeedback(ObjectNode targetNode, ObjectNode confidentialJson) {
        targetNode.set(PHYSICAL_ADDRESS.getEventValue(), confidentialJson.get(PHYSICAL_ADDRESS.getConfidentialValue()));
        targetNode.set(NEW_ADDRESS.getEventValue(), confidentialJson.get(NEW_ADDRESS.getConfidentialValue()));
        return targetNode;
    }

    private static ObjectNode applyPrepareAnalogDomicileFailure(ObjectNode targetNode, ObjectNode confidentialJson) {
        targetNode.set(FOUND_ADDRESS.getEventValue(), confidentialJson.get(FOUND_ADDRESS.getConfidentialValue()));
        targetNode.remove(FOUND_ADDRESS.getTimelineKey());
        return targetNode;
    }

    private static ObjectNode applyDefaultAddress(ObjectNode targetNode, ObjectNode confidentialJson) {
        targetNode.set(PHYSICAL_ADDRESS.getEventValue(), confidentialJson.get(PHYSICAL_ADDRESS.getConfidentialValue()));
        return targetNode;
    }

}
