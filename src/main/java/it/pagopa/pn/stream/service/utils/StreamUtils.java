package it.pagopa.pn.stream.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.mapper.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;
import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_EVENT_CONVERSION;


@Slf4j
@Component
public class StreamUtils {
    private final EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper;
    private final WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private final Duration ttl;
    private final PnStreamConfigs pnStreamConfigs;

    private final DtoToEntityWebhookTimelineMapper mapperTimeline;
   public StreamUtils(DtoToEntityWebhookTimelineMapper mapperTimeline, EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper,
                      WebhookTimelineElementJsonConverter timelineElementJsonConverter, PnStreamConfigs pnStreamConfigs) {
        this.entityToDtoTimelineMapper = entityToDtoTimelineMapper;
        this.pnStreamConfigs = pnStreamConfigs;
        this.timelineElementJsonConverter = timelineElementJsonConverter;
        this.ttl = pnStreamConfigs.getTtl();
        this.mapperTimeline = mapperTimeline;
    }
    public EventEntity buildEventEntity(Long atomicCounterUpdated, StreamEntity streamEntity,
                                        String newStatus, TimelineElementInternal timelineElementInternal) throws PnInternalException{

        Instant timestamp = timelineElementInternal.getTimestamp();

        // creo l'evento e lo salvo
        EventEntity eventEntity = new EventEntity(atomicCounterUpdated, streamEntity.getStreamId());

        if (!ttl.isZero())
            eventEntity.setTtl(LocalDateTime.now().plus(ttl).atZone(ZoneId.systemDefault()).toEpochSecond());
        eventEntity.setEventDescription(timestamp.toString() + "_" + timelineElementInternal.getTimelineElementId());

        // Lo iun ci va solo se è stata accettata, quindi escludo gli stati invalidation e refused
        if (StringUtils.hasText(newStatus)
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.IN_VALIDATION
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.REFUSED)
            eventEntity.setIun(timelineElementInternal.getIun());

        eventEntity.setNewStatus(newStatus);

        // il requestId ci va sempre, ed è il base64 dello iun
        eventEntity.setNotificationRequestId(Base64Utils.encodeToString(timelineElementInternal.getIun().getBytes(StandardCharsets.UTF_8)));

        WebhookTimelineElementEntity timelineElementEntity = null;
        try {
            timelineElementEntity = mapperTimeline.dtoToEntity(timelineElementInternal);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(e.getMessage(), ERROR_CODE_PN_GENERIC_ERROR);
        }

        eventEntity.setElement(this.timelineElementJsonConverter.entityToJson(timelineElementEntity));

        return eventEntity;
    }

    public TimelineElementInternal getTimelineInternalFromEvent(EventEntity entity) throws PnInternalException{
        WebhookTimelineElementEntity timelineElementEntity = this.timelineElementJsonConverter.jsonToEntity(entity.getElement());
        try {
            return entityToDtoTimelineMapper.entityToDto(timelineElementEntity);
        } catch (JsonProcessingException e) {
            throw new PnInternalException(e.getMessage(), ERROR_CODE_PN_GENERIC_ERROR);
        }
    }


    public static boolean checkGroups(List<String> toCheckGroups, List<String> allowedGroups){
        List<String> safeToCheck = toCheckGroups != null ? toCheckGroups : Collections.emptyList();
        List<String> safeAllowedGroups = allowedGroups != null ? allowedGroups : Collections.emptyList();

        return safeAllowedGroups.isEmpty() || safeAllowedGroups.containsAll(safeToCheck) ;
    }

    public int getVersion (String version) {

        if (version != null && !version.isEmpty()){
            String versionNumberString = version.toLowerCase().replace("v", "");
            return Integer.parseInt(versionNumberString);
        }
        return Integer.parseInt(pnStreamConfigs.getCurrentVersion().replace("v", ""));
    }

    public EventsQuarantineEntity buildEventQuarantineEntity(StreamEntity stream, TimelineElementInternal timelineElement) {
       try {
           EventsQuarantineEntity eventsQuarantineEntity = new EventsQuarantineEntity(stream.getStreamId(), timelineElement.getIun(), timelineElement.getTimelineElementId());
           eventsQuarantineEntity.setEvent(this.timelineElementJsonConverter.entityToJson(mapperTimeline.dtoToEntity(timelineElement)));
           return eventsQuarantineEntity;
       } catch (JsonProcessingException e) {
           log.warn("Error while converting timeline element into JSON", e);
           throw new PnStreamException("Error while converting timeline element into JSON", 500, ERROR_EVENT_CONVERSION);
       }
    }

    public NotificationUnlockedEntity buildNotificationUnlockedEntity(String streamId, String iun, Instant notificationSentAt) {
        NotificationUnlockedEntity notificationUnlockedEntity = new NotificationUnlockedEntity(streamId, iun);
        notificationUnlockedEntity.setTtl(notificationSentAt.plus(pnStreamConfigs.getMaxTtl()).atZone(ZoneOffset.UTC).toEpochSecond());
        return notificationUnlockedEntity;
    }

    public boolean checkIfTtlIsExpired(Instant notificationSentAt) {
        return notificationSentAt.plus(pnStreamConfigs.getMaxTtl()).isBefore(Instant.now());
    }
}
