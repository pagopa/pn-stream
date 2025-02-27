package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UnlockedNotificationEntityDaoImplTest extends BaseTest.WithLocalStack {

    @Autowired
    UnlockedNotificationEntityDao unlockedNotificationEntityDao;

    @Test
    void putAndGetItem() {
        NotificationUnlockedEntity entity = new NotificationUnlockedEntity();
        entity.setPk("pkTest");
        unlockedNotificationEntityDao.putItem(entity).block();

        NotificationUnlockedEntity result = unlockedNotificationEntityDao.findByPk("pkTest").block();

        assert result != null;
        Assertions.assertEquals("pkTest", result.getPk());
    }
}