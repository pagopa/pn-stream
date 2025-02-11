package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.WebhookStatsDaoImpl;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.Key;

class WebhookStatsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    WebhookStatsDaoImpl webhookStatsDao;

    @Test
    void getItem() {
        WebhookStatsEntity entity = new WebhookStatsEntity("pk1", "sk1");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        WebhookStatsEntity retrievedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk1").sortValue("sk1").build()).block();
        assert retrievedEntity != null;
        Assertions.assertEquals("pk1", retrievedEntity.getPk());
        Assertions.assertEquals("sk1", retrievedEntity.getSk());
        Assertions.assertNotNull(retrievedEntity.getValueCounter());
        Assertions.assertEquals(0L, retrievedEntity.getValueCounter());
    }

    @Test
    void updateAtomicCounterStats() {
        WebhookStatsEntity entity = new WebhookStatsEntity("pk2", "sk2");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        entity.setValueCounter(2L);
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        WebhookStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk2").sortValue("sk2").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("pk2", updatedEntity.getPk());
        Assertions.assertEquals("sk2", updatedEntity.getSk());
        Assertions.assertEquals(2L, updatedEntity.getValueCounter());
    }

    @Test
    void updateCustomCounterStats() {
        WebhookStatsEntity entity = new WebhookStatsEntity("pk3", "sk3");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        webhookStatsDao.updateCustomCounterStats("pk3", "sk3", "5").block();
        WebhookStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk3").sortValue("sk3").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("pk3", updatedEntity.getPk());
        Assertions.assertEquals("sk3", updatedEntity.getSk());
        Assertions.assertEquals(5L, updatedEntity.getValueCounter());
    }
}