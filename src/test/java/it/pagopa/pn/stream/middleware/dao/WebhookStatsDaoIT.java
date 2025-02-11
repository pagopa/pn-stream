package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.WebhookStatsDaoImpl;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.WebhookStatsEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WebhookStatsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    WebhookStatsDaoImpl webhookStatsDao;

    @Test
    void getItem() {
        WebhookStatsEntity entity = new WebhookStatsEntity("pk1", "sk1");
        webhookStatsDao.putItem(entity).block();
        WebhookStatsEntity retrievedEntity = webhookStatsDao.getItem("pk1","sk1").block();
        assert retrievedEntity != null;
        Assertions.assertEquals("pk1", retrievedEntity.getPk());
        Assertions.assertEquals("sk1", retrievedEntity.getSk());
        Assertions.assertNotNull(retrievedEntity.getValue());
        Assertions.assertEquals(0L, retrievedEntity.getValue());
    }

    @Test
    void updateItem() {
        WebhookStatsEntity entity = new WebhookStatsEntity("pk2", "sk2");
        webhookStatsDao.putItem(entity).block();
        entity.setValue(2L);
        webhookStatsDao.updateItem(entity).block();
        WebhookStatsEntity updatedEntity = webhookStatsDao.getItem("pk2","sk2" ).block();
        assert updatedEntity != null;
        Assertions.assertEquals("pk2", updatedEntity.getPk());
        Assertions.assertEquals("sk2", updatedEntity.getSk());
        Assertions.assertEquals(2L, updatedEntity.getValue());
    }
}