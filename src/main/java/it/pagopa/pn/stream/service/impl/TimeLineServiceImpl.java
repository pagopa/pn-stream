package it.pagopa.pn.stream.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.pagopa.pn.stream.dto.ConfidentialInformationEnum;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.service.TimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimeLineServiceImpl implements TimelineService {

    private final ObjectMapper mapper;

    @Override
    public String enrichTimelineElementWithConfidentialInformation(String details, ConfidentialTimelineElementDtoInt confidentialDto) {

        ObjectNode detailsJson = mapper.valueToTree(details);
        ObjectNode confidentialJson = mapper.valueToTree(confidentialDto);

        Arrays.stream(ConfidentialInformationEnum.values())
                .forEach(confEnum -> {
                    ObjectNode targetNode = detailsJson;

                    if (StringUtils.hasText(confEnum.getParent()) && detailsJson.has(confEnum.getParent())) {
                        targetNode = (ObjectNode) detailsJson.get(confEnum.getParent());
                    }

                    if (targetNode.has(confEnum.getTimelineKey())) {
                        targetNode.set(confEnum.getTimelineKey(), confidentialJson.get(confEnum.getConfidentialValue()));
                    }
                });

        return detailsJson.toString();
    }
}
