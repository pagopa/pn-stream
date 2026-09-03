package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToDtoRefusedReasonMapper;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class ProgressResponseElementMapper {
    private ProgressResponseElementMapper() {
    }

    public static ProgressResponseElementV30 internalToExternal(EventEntity ev) {
        ProgressResponseElementV30 progressResponseElement = baseMapping(ev);
        progressResponseElement.setTimelineEventCategory(ev.getTimelineEventCategory() != null ? TimelineElementCategoryV28.fromValue(ev.getTimelineEventCategory()) : null);
        return progressResponseElement;
    }

    public static ProgressResponseElementV30 internalToInformalExternal(EventEntity ev) {
        ProgressResponseElementV30 progressResponseElement = baseMapping(ev);
        progressResponseElement.setInformalTimelineEventCategory(ev.getTimelineEventCategory() != null ? InformalTimelineElementCategoryV1.fromValue(ev.getTimelineEventCategory()) : null);
        return progressResponseElement;
    }

    private static ProgressResponseElementV30 baseMapping(EventEntity ev) {
        ProgressResponseElementV30 progressResponseElement = new ProgressResponseElementV30();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatusV26.valueOf(ev.getNewStatus()) : null);
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
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
