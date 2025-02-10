package it.pagopa.pn.stream.middleware.dao.dynamo.mapper;

import it.pagopa.pn.stream.dto.stats.WebhookStatsDto;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import org.springframework.stereotype.Component;

@Component
public class DtoToEntityWebhookStats {
    public static WebhookStatsEntity toEntity(WebhookStatsDto dto) {
        return new WebhookStatsEntity(
                dto.getPaId() + "_" + dto.getStreamId() + "_" + dto.getStatsType(),
                dto.getSk(),
                dto.getTimeUnit(),
                dto.getValue(),
                dto.getTtl(),
                dto.getSpanUnit()
        );
    }
}
