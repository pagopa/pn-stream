package it.pagopa.pn.stream.middleware.dao.mapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.stream.dto.timeline.StatusInfoEntity;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityWebhookTimelineMapper {
    
    public WebhookTimelineElementEntity dtoToEntity(TimelineElementInternal dto) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return WebhookTimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getTimelineElementId() )
                .paId( dto.getPaId() )
                .category(dto.getCategory())
                .details(objectMapper.readValue(dto.getDetails(), new TypeReference<>() {}))
                .legalFactIds(dto.getLegalFactsIds())
                .statusInfo(dtoToStatusInfoEntity(dto.getStatusInfo()))
                .notificationSentAt(dto.getNotificationSentAt())
                .timestamp(dto.getBusinessTimestamp())
                .ingestionTimestamp(dto.getTimestamp())
                .eventTimestamp(dto.getBusinessTimestamp())
                .build();
    }

    private StatusInfoEntity dtoToStatusInfoEntity(StatusInfoInternal statusInfoInternal) {
        if(statusInfoInternal == null) return null;
        return StatusInfoEntity.builder()
                .statusChangeTimestamp(statusInfoInternal.getStatusChangeTimestamp())
                .statusChanged(statusInfoInternal.isStatusChanged())
                .actual(statusInfoInternal.getActual())
                .build();
    }
}
