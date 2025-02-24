package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventEntityBatch;
import it.pagopa.pn.stream.middleware.dao.dynamo.EventEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

class EventEntityDaoIT extends BaseTest.WithLocalStack{

    @Autowired
    EventEntityDao eventEntityDao;

    @Mock
    StreamEntityDao streamEntityDao;

    @Test
    void saveEventWithCOnditionKO(){
        EventEntity event = new EventEntity(1L, "streamId-1");
        event.setEventDescription("prova");
        eventEntityDao.save(event).block();
        EventEntity response = eventEntityDao.saveWithCondition(event).block();
        Assertions.assertNull(response);
    }

    @Test
    void saveEventWithConditionOK(){
        EventEntity event = new EventEntity(1L, "streamId-2");
        event.setEventDescription("prova");
        eventEntityDao.save(event).block();
        EventEntity event2 = new EventEntity(2L, "streamId-2");
        event2.setEventDescription("prova2");

        EventEntity resp = eventEntityDao.save(event2).block();
        Assertions.assertEquals("00000000000000000000000000000000000002", resp.getEventId());
        Assertions.assertEquals("streamId-2", resp.getStreamId());
    }

    @Test
    void saveEventWithConditionOK2(){
        EventEntity event = new EventEntity(1L, "streamId-3");
        event.setEventDescription("prova");
        eventEntityDao.save(event).block();
        EventEntity event2 = new EventEntity(2L, "streamId-4");
        event2.setEventDescription("prova");

        EventEntity resp = eventEntityDao.save(event2).block();
        Assertions.assertEquals("00000000000000000000000000000000000002", resp.getEventId());
        Assertions.assertEquals("streamId-4", resp.getStreamId());
    }


    @Test
    void saveEvent(){
        EventEntity event = eventEntityDao.save(new EventEntity(1L, "streamId")).block();
        Assertions.assertEquals("00000000000000000000000000000000000001", event.getEventId());
        Assertions.assertEquals("streamId", event.getStreamId());

        EventEntity event2 = eventEntityDao.save(new EventEntity(2L, "streamId")).block();
        Assertions.assertEquals("00000000000000000000000000000000000002", event2.getEventId());
        Assertions.assertEquals("streamId", event2.getStreamId());
    }

    @Test
    void findByStreamIdWithoutLastEventFound() {
        eventEntityDao.save(new EventEntity(1L, "streamIdx")).block();
        eventEntityDao.save(new EventEntity(2L, "streamIdx")).block();
        EventEntityBatch eventEntityBatch = eventEntityDao.findByStreamId("streamIdx",null).block();
        Assertions.assertEquals(2, eventEntityBatch.getEvents().size());
        Assertions.assertEquals("streamIdx", eventEntityBatch.getStreamId());
    }

    @Test
    void findByStreamIdWithLastEventFound() {
        eventEntityDao.save(new EventEntity(2L, "streamId3")).block();
        eventEntityDao.save(new EventEntity(3L, "streamId3")).block();
        EventEntityBatch eventEntityBatch = eventEntityDao.findByStreamId("streamId3","00000000000000000000000000000000000002").block();
        Assertions.assertEquals(1, eventEntityBatch.getEvents().size());
    }

    @Test
    void findByStreamIdNotFound() {
        eventEntityDao.save(new EventEntity(3L, "streamId4")).block();
        EventEntityBatch eventEntityBatch = eventEntityDao.findByStreamId("streamId4","00000000000000000000000000000000000003").block();
        Assertions.assertEquals(0, eventEntityBatch.getEvents().size());
    }

    @Test
    void deleteEventOlderThan() {
        eventEntityDao.save(new EventEntity(2L, "streamId6")).block();
        eventEntityDao.save(new EventEntity(3L, "streamId6")).block();
        eventEntityDao.save(new EventEntity(4L, "streamId6")).block();
        Boolean res = eventEntityDao.delete("streamId6", "00000000000000000000000000000000000003", true).block();
        Assertions.assertFalse(res);
    }

    @Test
    void deleteEventNotOlderThan() {
        eventEntityDao.save(new EventEntity(2L, "streamId5")).block();
        eventEntityDao.save(new EventEntity(3L, "streamId5")).block();
        eventEntityDao.save(new EventEntity(4L, "streamId5")).block();
        Boolean res = eventEntityDao.delete("streamId5", "00000000000000000000000000000000000003", false).block();
        Assertions.assertFalse(res);
    }

}
