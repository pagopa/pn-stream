package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV29;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.RefusedReason;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToDtoRefusedReasonMapper;

import org.springframework.util.CollectionUtils;

import java.util.List;

public class ProgressResponseElementMapper {
    private ProgressResponseElementMapper() {
    }

    public static ProgressResponseElementV29 internalToExternal(EventEntity ev) {
        ProgressResponseElementV29 progressResponseElement = new ProgressResponseElementV29();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatusV26.valueOf(ev.getNewStatus()) : null);
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
        progressResponseElement.setTimelineEventCategory(ev.getTimelineEventCategory() !=null ? TimelineElementCategoryV28.fromValue(ev.getTimelineEventCategory()) : null);
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
