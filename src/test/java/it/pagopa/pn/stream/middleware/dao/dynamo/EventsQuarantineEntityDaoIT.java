package it.pagopa.pn.stream.middleware.dao.dynamo;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

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
        entity.setEventId("skTest");
        entity.setEvent("elementTest");
        eventsQuarantineEntityDao.putItem(entity).block();

        Mono<Page<EventsQuarantineEntity>> result = eventsQuarantineEntityDao.findByPk("pkTest");
        Page<EventsQuarantineEntity> foundEntity = result.block();

        assert foundEntity != null;
        Assertions.assertEquals("pkTest", foundEntity.items().get(0).getPk());
        Assertions.assertEquals("skTest", foundEntity.items().get(0).getEventId());
        Assertions.assertEquals("elementTest", foundEntity.items().get(0).getEvent());
    }

    @Test
    void deleteItemsByKey() {
        EventsQuarantineEntity entity = new EventsQuarantineEntity();
        entity.setPk("pkTest");
        entity.setEventId("skTest1");
        entity.setEvent("elementTest1");
        eventsQuarantineEntityDao.putItem(entity).block();

        entity.setPk("pkTest");
        entity.setEventId("skTest2");
        entity.setEvent("elementTest2");
        eventsQuarantineEntityDao.putItem(entity).block();

        Page<EventsQuarantineEntity> result = eventsQuarantineEntityDao.findByPk("pkTest").block();

        assert result != null;

        result.items().forEach(resultEntity -> {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setStreamId("streamIdTest");
            eventEntity.setEventId("eventIdTest"+resultEntity.getEventId());
            eventsQuarantineEntityDao.saveAndClearElement(resultEntity, eventEntity).block();
        });

        List<EventsQuarantineEntity> finalResult = Objects.requireNonNull(eventsQuarantineEntityDao.findByPk("pkTest").block()).items();
        EventEntityBatch resultEvent = eventEntityDao.findByStreamId("streamIdTest", null).block();


        assert CollectionUtils.isEmpty(finalResult);
        assert !Objects.isNull(resultEvent) && !CollectionUtils.isEmpty(resultEvent.getEvents());
    }
}