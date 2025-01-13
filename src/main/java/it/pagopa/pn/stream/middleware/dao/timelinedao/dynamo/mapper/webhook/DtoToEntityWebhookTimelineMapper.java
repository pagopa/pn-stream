package it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook;

import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.*;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.stream.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoToEntityWebhookTimelineMapper {
    
    public WebhookTimelineElementEntity dtoToEntity(TimelineElementInternal dto) {
        return WebhookTimelineElementEntity.builder()
                .iun( dto.getIun() )
                .timelineElementId( dto.getElementId() )
                .paId( dto.getPaId() )
                .category( TimelineElementCategoryEntity.valueOf( dto.getCategory().getValue() ) )
               // .details( dtoToDetailsEntity( dto.getDetails() ) )
                .legalFactIds( convertLegalFactsToEntity( dto.getLegalFactsIds() ) )
                .statusInfo(dtoToStatusInfoEntity(dto.getStatusInfo()))
                .notificationSentAt(dto.getNotificationSentAt())
                .timestamp(dto.getTimestamp())
                .ingestionTimestamp(dto.getIngestionTimestamp())
                .eventTimestamp(dto.getEventTimestamp())
                .build();
    }

    private List<LegalFactsIdEntity> convertLegalFactsToEntity(List<LegalFactsIdInt>  dto ) {
        List<LegalFactsIdEntity> legalFactsIds = null;

        if (dto != null){
            legalFactsIds = dto.stream().map( this::mapOneLegalFact ).toList();
        }

        return legalFactsIds;
    }

    private LegalFactsIdEntity mapOneLegalFact(LegalFactsIdInt legalFactsId) {
        LegalFactsIdEntity entity = new LegalFactsIdEntity();
        entity.setKey( legalFactsId.getKey() );
        entity.setCategory(LegalFactCategoryEntity.valueOf( legalFactsId.getCategory().getValue()));
        return entity;
    }
    
    private TimelineElementDetailsEntity dtoToDetailsEntity(TimelineElementDetailsInt details) {
        return SmartMapper.mapToClass(details, TimelineElementDetailsEntity.class );
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
