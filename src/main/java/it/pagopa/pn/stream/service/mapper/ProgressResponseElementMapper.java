package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.mapper.EntityToDtoRefusedReasonMapper;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.NotificationStatus;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.ProgressResponseElementV25;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.RefusedReason;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.TimelineElementCategoryV23;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ProgressResponseElementMapper {
    private ProgressResponseElementMapper() {
    }

    public static ProgressResponseElementV25 internalToExternal(EventEntity ev) {
        ProgressResponseElementV25 progressResponseElement = new ProgressResponseElementV25();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatus.valueOf(ev.getNewStatus()) : null);
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
        progressResponseElement.setTimelineEventCategory(ev.getTimelineEventCategory() !=null ? TimelineElementCategoryV23.fromValue(ev.getTimelineEventCategory()) : null);
        progressResponseElement.setChannel(ev.getChannel());
        progressResponseElement.setRecipientIndex(ev.getRecipientIndex());
        progressResponseElement.setLegalfactIds(ev.getLegalfactIds());
        progressResponseElement.setAnalogCost(ev.getAnalogCost());
        progressResponseElement.setValidationErrors( !CollectionUtils.isEmpty( ev.getValidationErrors() ) ? mapRefusedReasons( ev.getValidationErrors() ) : null );
        return progressResponseElement;
    }
    private static List<RefusedReason> mapRefusedReasons(List<RefusedReasonEntity> refusedReasonEntityList) {
        return refusedReasonEntityList.stream()
                .map(EntityToDtoRefusedReasonMapper::entityToDto)
                .toList();
    }

}
