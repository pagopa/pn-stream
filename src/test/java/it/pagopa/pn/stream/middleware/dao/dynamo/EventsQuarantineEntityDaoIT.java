package it.pagopa.pn.stream.middleware.dao.dynamo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventsQuarantineEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity.webhook.WebhookTimelineElementEntity;
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
    void findByPk() throws JsonProcessingException {
        EventsQuarantineEntity entity = new EventsQuarantineEntity("streamId", "iun", "eventId");
        WebhookTimelineElementEntity<Object> timelineElementInternal = new WebhookTimelineElementEntity<>();
        timelineElementInternal.setCategory("testCategory");
        timelineElementInternal.setDetails("testDetails");

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> objectHashMap = objectMapper.convertValue(timelineElementInternal, new TypeReference<>() {});

        entity.setEvent(objectMapper.writeValueAsString(objectHashMap));
        eventsQuarantineEntityDao.putItem(entity).block();

        Mono<Page<EventsQuarantineEntity>> result = eventsQuarantineEntityDao.findByPk("streamId_iun");
        Page<EventsQuarantineEntity> foundEntity = result.block();

        assert foundEntity != null;
        Assertions.assertEquals("streamId_iun", foundEntity.items().get(0).getPk());
        Assertions.assertEquals("eventId", foundEntity.items().get(0).getEventId());

        objectMapper.registerModule(new JavaTimeModule());
        WebhookTimelineElementEntity webhookTimelineElementEntity = objectMapper.readValue(foundEntity.items().get(0).getEvent(), WebhookTimelineElementEntity.class);

        Assertions.assertEquals("testCategory", webhookTimelineElementEntity.getCategory());
    }

    @Test
    void deleteItemsByKey() {
        EventsQuarantineEntity entity = new EventsQuarantineEntity("streamId2", "iun2", "eventId2");
        eventsQuarantineEntityDao.putItem(entity).block();

        EventsQuarantineEntity entity2 = new EventsQuarantineEntity("streamId2", "iun2", "eventId3");
        eventsQuarantineEntityDao.putItem(entity2).block();

        Map<String, AttributeValue> lastEvaluateKey = new HashMap<>();

        Page<EventsQuarantineEntity> result = eventsQuarantineEntityDao.findByPk("pkTest", lastEvaluateKey, 100).block();

        assert result != null;

        result.items().forEach(resultEntity -> {
            EventEntity eventEntity = new EventEntity();
            eventEntity.setStreamId("streamId2");
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