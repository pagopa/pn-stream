package it.pagopa.pn.stream.service.utils;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes;
import it.pagopa.pn.stream.middleware.dao.notificationdao.NotificationDao;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.service.NotificationService;
import it.pagopa.pn.stream.service.StatusService;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;


@Slf4j
@Component
public class StreamUtils {
    private final EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper;
    private final WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private final NotificationService notificationService;
    private final Duration ttl;
    private final PnStreamConfigs pnStreamConfigs;
    private final NotificationDao notificationDao;

   public StreamUtils(NotificationService notificationService,
                       PnStreamConfigs pnStreamConfigs, DtoToEntityWebhookTimelineMapper mapperTimeline, EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper,
                       WebhookTimelineElementJsonConverter timelineElementJsonConverter, NotificationDao notificationDao) {
        this.notificationService = notificationService;
        this.entityToDtoTimelineMapper = entityToDtoTimelineMapper;
        this.pnStreamConfigs = pnStreamConfigs;
        this.timelineElementJsonConverter = timelineElementJsonConverter;
        this.notificationDao = notificationDao;
    }

    public List<String> getNotification(String iun) {
        return notificationDao.getNotificationEntity(iun)
                .switchIfEmpty(Mono.defer(() -> {
                    try {
                        NotificationInt notificationInt = notificationService.getNotificationByIun(iun);
                        NotificationEntity fallbackEntity = new NotificationEntity();
                        fallbackEntity.setGroups(Collections.singletonList(notificationInt.getGroup()));
                        return Mono.just(fallbackEntity);
                    } catch (Exception ex) {
                        log.error("Error while retrieving notification from notificationService", ex);
                        return Mono.error(new PnInternalException("Notification not found in notificationService", PnStreamExceptionCodes.ERROR_CODE_WEBHOOK_SAVEEVENT));
                    }
                }))
                .map(NotificationEntity::getGroups)
                .block();
    }

    public EventEntity buildEventEntity(Long atomicCounterUpdated, StreamEntity streamEntity,
                                        String newStatus, TimelineElementInternal timelineElementInternal) throws PnInternalException{

        Instant timestamp = timelineElementInternal.getTimestamp();

        // creo l'evento e lo salvo
        EventEntity eventEntity = new EventEntity(atomicCounterUpdated, streamEntity.getStreamId());

        if (!ttl.isZero())
            eventEntity.setTtl(LocalDateTime.now().plus(ttl).atZone(ZoneId.systemDefault()).toEpochSecond());
        eventEntity.setEventDescription(timestamp.toString() + "_" + timelineElementInternal.getElementId());

        // Lo iun ci va solo se è stata accettata, quindi escludo gli stati invalidation e refused
        if (StringUtils.hasText(newStatus)
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.IN_VALIDATION
                && NotificationStatusInt.valueOf(newStatus) != NotificationStatusInt.REFUSED)
            eventEntity.setIun(timelineElementInternal.getIun());

        eventEntity.setNewStatus(newStatus);

        // il requestId ci va sempre, ed è il base64 dello iun
        eventEntity.setNotificationRequestId(Base64Utils.encodeToString(timelineElementInternal.getIun().getBytes(StandardCharsets.UTF_8)));

        WebhookTimelineElementEntity timelineElementEntity = mapperTimeline.dtoToEntity(timelineElementInternal);

        eventEntity.setElement(this.timelineElementJsonConverter.entityToJson(timelineElementEntity));

        return eventEntity;
    }

    public TimelineElementInternal getTimelineInternalFromEvent(EventEntity entity) throws PnInternalException{
        WebhookTimelineElementEntity timelineElementEntity = this.timelineElementJsonConverter.jsonToEntity(entity.getElement());
        return entityToDtoTimelineMapper.entityToDto(timelineElementEntity);
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
}
