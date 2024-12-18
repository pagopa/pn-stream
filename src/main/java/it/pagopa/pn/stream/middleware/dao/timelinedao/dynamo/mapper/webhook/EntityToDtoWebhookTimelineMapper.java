package it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook;

import it.pagopa.pn.stream.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementDetailsInt;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.LegalFactsIdEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.StatusInfoEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.TimelineElementDetailsEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.stream.service.mapper.SmartMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EntityToDtoWebhookTimelineMapper {
    
    public TimelineElementInternal entityToDto(WebhookTimelineElementEntity entity ) {
        TimelineElementCategoryInt category = entity.getCategory() != null ? TimelineElementCategoryInt.valueOf(entity.getCategory().getValue()) : null;

        assert category != null;
        return TimelineElementInternal.builder()
                .iun(entity.getIun())
                .elementId( entity.getTimelineElementId() )
                .category( category )
                .details( parseDetailsFromEntity( entity.getDetails(), category) )
                .legalFactsIds( convertLegalFactsFromEntity( entity.getLegalFactIds() ) )
                .statusInfo(entityToStatusInfoInternal(entity.getStatusInfo()))
                .notificationSentAt(entity.getNotificationSentAt())
                .paId(entity.getPaId())
                .timestamp(entity.getTimestamp())
                .ingestionTimestamp(entity.getIngestionTimestamp())
                .eventTimestamp(entity.getEventTimestamp())
                .build();
    }

    private List<LegalFactsIdInt> convertLegalFactsFromEntity(List<LegalFactsIdEntity>  entity ) {
        List<LegalFactsIdInt> legalFactsIds = null;
        
        if (entity != null){
                    legalFactsIds = entity.stream().map( this::mapOneLegalFact ).toList();
        }
        
        return legalFactsIds;
    }

    private LegalFactsIdInt mapOneLegalFact(LegalFactsIdEntity legalFactsIdEntity) {
        String legalFactCategoryName = legalFactsIdEntity.getCategory().getValue();
        return LegalFactsIdInt.builder()
                .key(legalFactsIdEntity.getKey())
                .category( LegalFactCategoryInt.valueOf( legalFactCategoryName ) )
                .build();
    }

    private TimelineElementDetailsInt parseDetailsFromEntity(TimelineElementDetailsEntity entity, TimelineElementCategoryInt category) {
        return SmartMapper.mapToClass(entity, category.getDetailsJavaClass());
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