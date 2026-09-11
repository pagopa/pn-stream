package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToDtoRefusedReasonMapper;
import it.pagopa.pn.stream.utils.CommunicationTypeUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

public class ProgressResponseElementMapper {
    private ProgressResponseElementMapper() {
    }

    public static ProgressResponseElementV30 internalToExternal(EventTimelineInternalDto eventTimeline) {
        EventEntity ev = eventTimeline.getEventEntity();
        ProgressResponseElementV30 progressResponseElement = baseMapping(ev);
        if (StringUtils.hasText(ev.getElement())) {
            TimelineElementInternal timelineElementInternal = eventTimeline.getTimelineElementInternal();
            CommunicationType communicationType = CommunicationTypeUtils.getDefaultCommunicationType(timelineElementInternal.getCommunicationType());
            progressResponseElement.setCommunicationType(it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType.fromValue(communicationType.name()));
            switch(communicationType) {
                case LEGAL -> enrichLegalResponse(progressResponseElement, ev, timelineElementInternal);
                case INFORMAL -> enrichInformalResponse(progressResponseElement, ev, timelineElementInternal);
            }
        }
        return progressResponseElement;
    }

    private static ProgressResponseElementV30 baseMapping(EventEntity ev) {
        ProgressResponseElementV30 progressResponseElement = new ProgressResponseElementV30();
        progressResponseElement.setEventId(ev.getEventId());
        progressResponseElement.setTimestamp(ev.getTimestamp());
        progressResponseElement.setIun(ev.getIun());
        progressResponseElement.setNotificationRequestId(ev.getNotificationRequestId());
        progressResponseElement.setChannel(ev.getChannel());
        progressResponseElement.setRecipientIndex(ev.getRecipientIndex());
        progressResponseElement.setAnalogCost(ev.getAnalogCost());
        progressResponseElement.setValidationErrors( !CollectionUtils.isEmpty( ev.getValidationErrors() ) ? mapRefusedReasons( ev.getValidationErrors() ) : null );
        return progressResponseElement;
    }

    private static List<RefusedReason> mapRefusedReasons(List<RefusedReasonEntity> refusedReasonEntityList) {
        return refusedReasonEntityList.stream()
                .map(EntityToDtoRefusedReasonMapper::entityToDto)
                .toList();
    }

    private static void enrichLegalResponse(ProgressResponseElementV30 progressResponseElement, EventEntity ev, TimelineElementInternal timelineElementInternal) {
        TimelineElementV28 timelineElement = TimelineElementMapper.internalToExternal(timelineElementInternal);
        progressResponseElement.setTimelineEventCategory(ev.getTimelineEventCategory() != null ? TimelineElementCategoryV28.fromValue(ev.getTimelineEventCategory()) : null);
        progressResponseElement.setElement(timelineElement);
        progressResponseElement.setLegalfactIds(ev.getLegalfactIds());
        progressResponseElement.setNewStatus(ev.getNewStatus() != null ? NotificationStatusV26.valueOf(ev.getNewStatus()) : null);
    }

    private static void enrichInformalResponse(ProgressResponseElementV30 progressResponseElement, EventEntity ev, TimelineElementInternal timelineElementInternal) {
        InformalTimelineElementV1 informalTimelineElement = TimelineElementMapper.internalToInformalExternal(timelineElementInternal);
        progressResponseElement.informalElement(informalTimelineElement);
        progressResponseElement.setInformalNewStatus(ev.getNewStatus() != null ? InformalNotificationStatusV1.valueOf(ev.getNewStatus()) : null);
    }

}
