package it.pagopa.pn.stream.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.config.springbootcfg.AbstractCachedSsmParameterConsumerActivation;
import it.pagopa.pn.stream.dto.CustomRetryAfterParameter;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import it.pagopa.pn.stream.middleware.dao.mapper.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_ERROR;


@Slf4j
@Component
public class StreamUtils {
    private static final long SECONDS_IN_HOUR = 3600L;
    private static final long SECONDS_IN_MINUTE = 60L;
    private static final long SECONDS_IN_DAY = 86400L;

    private final EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper;
    private final WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private final Duration ttl;
    private final PnStreamConfigs pnStreamConfigs;

    private final AbstractCachedSsmParameterConsumerActivation ssmParameterConsumerActivation;

    private final DtoToEntityWebhookTimelineMapper mapperTimeline;
   public StreamUtils(DtoToEntityWebhookTimelineMapper mapperTimeline, EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper,
                      WebhookTimelineElementJsonConverter timelineElementJsonConverter, PnStreamConfigs pnStreamConfigs,
                      AbstractCachedSsmParameterConsumerActivation ssmParameterConsumerActivation) {
        this.entityToDtoTimelineMapper = entityToDtoTimelineMapper;
        this.pnStreamConfigs = pnStreamConfigs;
        this.timelineElementJsonConverter = timelineElementJsonConverter;
        this.ttl = pnStreamConfigs.getTtl();
        this.mapperTimeline = mapperTimeline;
       this.ssmParameterConsumerActivation = ssmParameterConsumerActivation;
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
    public Instant retrieveCurrentInterval() {
        Instant startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        long spanInSeconds = convertToSeconds(pnStreamConfigs.getStats().getSpanUnit(), pnStreamConfigs.getStats().getTimeUnit());
        long elapsedTimeInSeconds = Duration.between(startOfYear, Instant.now()).getSeconds();
        long currentIntervalIndex = elapsedTimeInSeconds / spanInSeconds;

        return startOfYear.plusSeconds(currentIntervalIndex * spanInSeconds);
    }


    private static long convertToSeconds(int spanUnit, StatsTimeUnit timeUnit) {
        return switch (timeUnit) {
            case HOURS -> spanUnit * SECONDS_IN_HOUR;
            case MINUTES -> spanUnit * SECONDS_IN_MINUTE;
            case DAYS -> spanUnit * SECONDS_IN_DAY;
        };
    }

    public StreamStatsEntity buildEntity(StreamStatsEnum streamStatsEnum, String paId, String streamId) {
        log.info("Build entity for stream stats: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
        StreamStatsEntity streamStatsEntity = new StreamStatsEntity(paId, streamId, streamStatsEnum);
        streamStatsEntity.setSk(buildSk());
        streamStatsEntity.setTtl(LocalDateTime.now().plus(retrieveStatsTtl(streamStatsEnum)).atZone(ZoneOffset.UTC).toEpochSecond());
        log.info("Entity built for stream stats: {} for paId: {} and streamId: {}", streamStatsEnum, paId, streamId);
        return streamStatsEntity;
    }

    public Duration retrieveStatsTtl(StreamStatsEnum streamStatsEnum) {
        return ssmParameterConsumerActivation.getParameterValue(pnStreamConfigs.getStats().getCustomTtlParameterName(), Map.class)
                .map(map -> (String) map.get(streamStatsEnum.name()))
                .map(DurationStyle.SIMPLE::parse)
                .orElse(pnStreamConfigs.getStats().getTtl());
    }

    public Instant retrieveRetryAfter(String xPagopaPnCxId) {
        return ssmParameterConsumerActivation.getParameterValue(pnStreamConfigs.getRetryParameterPrefix() + xPagopaPnCxId, CustomRetryAfterParameter.class)
                .map(customRetryAfterParameter -> Instant.now().plusMillis(customRetryAfterParameter.getRetryAfter()))
                .orElse(Instant.now().plusMillis(pnStreamConfigs.getScheduleInterval()));
    }

    public String buildSk() {
        return retrieveCurrentInterval()+ "#" + pnStreamConfigs.getStats().getTimeUnit() + "#" + pnStreamConfigs.getStats().getSpanUnit();
    }
}
