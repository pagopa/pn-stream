package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.dto.stats.WebhookStatsDto;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import org.springframework.stereotype.Component;

@Component
public class EntityToDtoWebhookStats {

    public static WebhookStatsDto toDto(WebhookStatsEntity entity) {
        return WebhookStatsDto.builder()
                .paId(entity.getPk().split("_")[0])
                .streamId(entity.getPk().split("_")[1])
                .statsType(entity.getPk().split("_")[2])
                .sk(entity.getSk())
                .timeUnit(entity.getTimeUnit())
                .value(entity.getValue())
                .ttl(entity.getTtl())
                .spanUnit(entity.getSpanUnit())
                .build();
    }
}
