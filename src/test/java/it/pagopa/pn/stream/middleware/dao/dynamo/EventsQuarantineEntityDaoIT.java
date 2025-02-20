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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        entity = new EventsQuarantineEntity();
        entity.setPk("pkTest2");
        entity.setEventId("skTest2");
        entity.setEvent("elementTest2");

        eventsQuarantineEntityDao.putItem(entity).block();

        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();

        Mono<Page<EventsQuarantineEntity>> result = eventsQuarantineEntityDao.findByPk("pkTest", lastEvaluateKey, 1);
        Page<EventsQuarantineEntity> foundEntity = result.block();

        assert foundEntity != null;
        Assertions.assertEquals(1, foundEntity.items().size());
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

        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();

        Page<EventsQuarantineEntity> result = eventsQuarantineEntityDao.findByPk("pkTest", lastEvaluateKey, 100).block();

        assert result != null;

        result.items().forEach(resultEntity -> {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setStreamId("streamIdTest");
            eventEntity.setEventId("eventIdTest"+resultEntity.getEventId());
            eventsQuarantineEntityDao.saveAndClearElement(resultEntity, eventEntity).block();
        });

        lastEvaluateKey = new HashMap<>();

        List<EventsQuarantineEntity> finalResult = Objects.requireNonNull(eventsQuarantineEntityDao.findByPk("pkTest", lastEvaluateKey, 100).block()).items();
        EventEntityBatch resultEvent = eventEntityDao.findByStreamId("streamIdTest", null).block();


        assert CollectionUtils.isEmpty(finalResult);
        assert !Objects.isNull(resultEvent) && !CollectionUtils.isEmpty(resultEvent.getEvents());
    }
}