package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamStatsDaoImpl;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamStatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Duration;

class StreamStatsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    StreamStatsDaoImpl webhookStatsDao;

    @Mock
    PnStreamConfigs.Stats stats;

    @Test
    void getItem() {
        StreamStatsEntity entity = new StreamStatsEntity("paId", "streamId", StreamStatsEnum.NUMBER_OF_REQUESTS);
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        StreamStatsEntity retrievedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("paId#streamId#NUMBER_OF_REQUESTS").sortValue("sk1").build()).block();
        assert retrievedEntity != null;
        Assertions.assertEquals("paId#streamId#NUMBER_OF_REQUESTS", retrievedEntity.getPk());
        Assertions.assertNotNull(retrievedEntity.getCounter());
        Assertions.assertEquals(1L, retrievedEntity.getCounter());
    }

    @Test
    void updateAtomicCounterStats() {
        stats.setTtl(Duration.ofDays(3));
        StreamStatsEntity entity = new StreamStatsEntity("paId", "streamId", StreamStatsEnum.NUMBER_OF_REQUESTS);
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        webhookStatsDao.updateAtomicCounterStats(entity).block();
        StreamStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("paId#streamId#NUMBER_OF_REQUESTS").sortValue("sk2").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("paId#streamId#NUMBER_OF_REQUESTS", updatedEntity.getPk());
        Assertions.assertEquals("sk2", updatedEntity.getSk());
        Assertions.assertEquals(2L, updatedEntity.getCounter());
    }

    @Test
    void updateCustomCounterStats() {
        stats.setTtl(Duration.ofDays(3));
        webhookStatsDao.updateCustomCounterStats("paId#streamId#NUMBER_OF_READINGS", "sk3", 5).block();
        StreamStatsEntity updatedEntity = webhookStatsDao.getItem(Key.builder().partitionValue("paId#streamId#NUMBER_OF_READINGS").sortValue("sk3").build()).block();
        assert updatedEntity != null;
        Assertions.assertEquals("paId#streamId#NUMBER_OF_READINGS", updatedEntity.getPk());
        Assertions.assertEquals("sk3", updatedEntity.getSk());
        Assertions.assertEquals(5L, updatedEntity.getCounter());
    }
}