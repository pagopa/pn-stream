package it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StatusInfoEntity;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoWebhookTimelineMapper {
    
    public TimelineElementInternal entityToDto(WebhookTimelineElementEntity entity ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return TimelineElementInternal.builder()
                .iun(entity.getIun())
                .timelineElementId( entity.getTimelineElementId() )
                .category( entity.getCategory() )
                .details(objectMapper.writeValueAsString(entity.getDetails()))
                .legalFactId(  entity.getLegalFactIds() )
                .statusInfo(entityToStatusInfoInternal(entity.getStatusInfo()))
                .notificationSentAt(entity.getNotificationSentAt())
                .paId(entity.getPaId())
                .timestamp(entity.getTimestamp())
                .ingestionTimestamp(entity.getIngestionTimestamp())
                .eventTimestamp(entity.getEventTimestamp())
                .build();
    }


    private StatusInfoInternal entityToStatusInfoInternal(StatusInfoEntity entity) {
        if(entity == null) return null;

        return StatusInfoInternal.builder()
                .actual(entity.getActual())
                .statusChanged(entity.isStatusChanged())
                .statusChangeTimestamp(entity.getStatusChangeTimestamp())
                .build();
    }
}
