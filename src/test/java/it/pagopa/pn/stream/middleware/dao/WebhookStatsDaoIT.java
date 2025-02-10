package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.dto.stats.TimeUnitEnum;
import it.pagopa.pn.stream.dto.stats.WebhookStatsDto;
import it.pagopa.pn.stream.middleware.dao.dynamo.WebhookStatsDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookStatsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    WebhookStatsDao webhookStatsDao;

    @Test
    void putItemIfAbsent() {
        WebhookStatsDto stats = WebhookStatsDto.builder()
                .paId("paId-1")
                .streamId("streamId-1")
                .statsType("type-1")
                .sk("sk-1")
                .spanUnit("span-1")
                .timeUnit(TimeUnitEnum.DAYS)
                .value(100)
                .ttl(123456789L)
                .build();

        webhookStatsDao.putItemIfAbsent(stats).block();
        WebhookStatsEntity entity = webhookStatsDao.getItem(new WebhookStatsEntity("paId-1", "sk-1", TimeUnitEnum.DAYS, 100, 123456789L, "span-1")).block();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("paId-1", entity.getPk());
        Assertions.assertEquals("sk-1", entity.getSk());
    }

    @Test
    void getItem() {
        WebhookStatsDto stats = WebhookStatsDto.builder()
                .paId("paId-2")
                .streamId("streamId-2")
                .statsType("type-2")
                .sk("sk-2")
                .spanUnit("span-2")
                .timeUnit(TimeUnitEnum.HOURS)
                .value(200)
                .ttl(987654321L)
                .build();

        webhookStatsDao.putItemIfAbsent(stats).block();
        WebhookStatsEntity entity = webhookStatsDao.getItem(new WebhookStatsEntity("paId-2", "sk-2", TimeUnitEnum.HOURS, 200, 987654321L, "span-2")).block();
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("paId-2", entity.getPk());
        Assertions.assertEquals("sk-2", entity.getSk());
    }
}