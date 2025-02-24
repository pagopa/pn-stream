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
        NotificationUnlockedEntity entity = new NotificationUnlockedEntity("streamId", "iun");
        unlockedNotificationEntityDao.putItem(entity).block();

        NotificationUnlockedEntity result = unlockedNotificationEntityDao.findByPk("streamId_iun").block();

        assert result != null;
        Assertions.assertEquals("streamId_iun", result.getPk());
    }
}