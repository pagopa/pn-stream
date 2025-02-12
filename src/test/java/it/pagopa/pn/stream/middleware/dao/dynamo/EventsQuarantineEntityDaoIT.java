package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

class EventsQuarantineEntityDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    EventsQuarantineEntityDao eventsQuarantineEntityDao;

    @Autowired
    EventEntityDao eventEntityDao;

    @Test
    void findByPk() {
        EventsQuarantineEntity entity = new EventsQuarantineEntity();
        entity.setPk("pkTest");
        entity.setSk("skTest");
        entity.setElement("elementTest");
        eventsQuarantineEntityDao.putItem(entity).block();

        Flux<EventsQuarantineEntity> result = eventsQuarantineEntityDao.findByPk("pkTest");
        EventsQuarantineEntity foundEntity = result.blockFirst();

        assert foundEntity != null;
        Assertions.assertEquals("pkTest", foundEntity.getPk());
        Assertions.assertEquals("skTest", foundEntity.getSk());
        Assertions.assertEquals("elementTest", foundEntity.getElement());
    }

    @Test
    void deleteItemsByKey() {
        EventsQuarantineEntity entity = new EventsQuarantineEntity();
        entity.setPk("pkTest");
        entity.setSk("skTest1");
        entity.setElement("elementTest1");
        eventsQuarantineEntityDao.putItem(entity).block();

        entity.setPk("pkTest");
        entity.setSk("skTest2");
        entity.setElement("elementTest2");
        eventsQuarantineEntityDao.putItem(entity).block();

        List<EventsQuarantineEntity> result = eventsQuarantineEntityDao.findByPk("pkTest").collectList().block();

        assert result != null;

        result.forEach(resultEntity -> {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setStreamId("streamIdTest");
            eventEntity.setEventId("eventIdTest"+resultEntity.getSk());
            eventsQuarantineEntityDao.saveAndClearElement(resultEntity, eventEntity).block();
        });

        List<EventsQuarantineEntity> finalResult = eventsQuarantineEntityDao.findByPk("pkTest").collectList().block();
        EventEntityBatch resultEvent = eventEntityDao.findByStreamId("streamIdTest", null).block();


        assert CollectionUtils.isEmpty(finalResult);
        assert !Objects.isNull(resultEvent) && !CollectionUtils.isEmpty(resultEvent.getEvents());
    }
}