package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDaoImpl;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.Key;

class StreamStatsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    StreamStatsDaoImpl webhookStatsDao;

    @Test
    void getItem() {
        StreamStatsEntity entity = new StreamStatsEntity("pk1", "sk1");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        StreamStatsEntity retrievedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk1").sortValue("sk1").build()).block();
        assert retrievedEntity != null;
        Assertions.assertEquals("pk1", retrievedEntity.getPk());
        Assertions.assertEquals("sk1", retrievedEntity.getSk());
        Assertions.assertNotNull(retrievedEntity.getCounter());
        Assertions.assertEquals(1L, retrievedEntity.getCounter());
    }

    @Test
    void updateAtomicCounterStats() {
        StreamStatsEntity entity = new StreamStatsEntity("pk2", "sk2");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        StreamStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk2").sortValue("sk2").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("pk2", updatedEntity.getPk());
        Assertions.assertEquals("sk2", updatedEntity.getSk());
        Assertions.assertEquals(2L, updatedEntity.getCounter());
    }

    @Test
    void updateCustomCounterStats() {
        StreamStatsEntity entity = new StreamStatsEntity("pk3", "sk3");
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        webhookStatsDao.updateCustomCounterStats("pk3", "sk3", "5").block();
        StreamStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("pk3").sortValue("sk3").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("pk3", updatedEntity.getPk());
        Assertions.assertEquals("sk3", updatedEntity.getSk());
        Assertions.assertEquals(6L, updatedEntity.getCounter());
    }
}