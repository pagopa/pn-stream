package it.pagopa.pn.stream.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.SendingReceipt;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV27;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementDetailsV27;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementV27;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_GENERIC;

public class TimelineElementMapper {
    private TimelineElementMapper() {
    }

    public static TimelineElementV27 internalToExternal(TimelineElementInternal internalDto) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TimelineElementV27.TimelineElementV27Builder builder;
        try {
            TimelineElementDetailsV27 timelineElement = objectMapper.readValue(internalDto.getDetails(), TimelineElementDetailsV27.class);
            //TODO: remove this when the digital feedback and progress will be managed correctly by the new service
            if (!CollectionUtils.isEmpty(timelineElement.getSendingReceipts())) {
                timelineElement.sendingReceipts(timelineElement.getSendingReceipts().stream().map(elem -> SendingReceipt.builder().build()).toList());
            }
            if (Objects.isNull(timelineElement.getNextDigitalAddressSource())){
                timelineElement.setNextSourceAttemptsMade(null);
            }
            builder = TimelineElementV27.builder()
                    .category(internalDto.getCategory() != null ? TimelineElementCategoryV27.fromValue(internalDto.getCategory()) : null)
                    .elementId(internalDto.getTimelineElementId())
                    .timestamp(internalDto.getTimestamp())
                    .notificationSentAt(internalDto.getNotificationSentAt())
                    .ingestionTimestamp(internalDto.getIngestionTimestamp())
                    .eventTimestamp(internalDto.getEventTimestamp())
                    .details(timelineElement)
                    .legalFactsIds(internalDto.getLegalFactId());

        } catch (JsonProcessingException e) {
            throw new PnStreamException(e.getMessage(), 500, ERROR_CODE_GENERIC);
        }


        return builder.build();
    }


}
