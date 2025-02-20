package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationUnlockedEntityDaoImplTest extends BaseTest.WithLocalStack {

    @Autowired
    NotificationUnlockedEntityDao notificationUnlockedEntityDao;

    @Test
    void putAndGetItem() {
        NotificationUnlockedEntity entity = new NotificationUnlockedEntity();
        entity.setPk("pkTest");
        notificationUnlockedEntityDao.putItem(entity).block();

        NotificationUnlockedEntity result = notificationUnlockedEntityDao.findByPk("pkTest").block();

        assert result != null;
        Assertions.assertEquals("pkTest", result.getPk());
    }
}