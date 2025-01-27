package it.pagopa.pn.stream.middleware.dao;

import it.pagopa.pn.stream.BaseTest;
import it.pagopa.pn.stream.middleware.dao.dynamo.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamRetryAfter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.util.function.Tuple2;

import java.time.Instant;
import java.util.Optional;

class StreamEntityDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    StreamEntityDao streamEntityDaoDynamo;

    @Test
    void findByPa() {
        streamEntityDaoDynamo.save(new StreamEntity("paId","streamId")).block();
        StreamEntity streamEntity = streamEntityDaoDynamo.findByPa("paId").blockFirst();
        assert streamEntity != null;
        Assertions.assertEquals("paId", streamEntity.getPaId()) ;
        Assertions.assertEquals("streamId", streamEntity.getStreamId());
        Assertions.assertNotNull(streamEntity.getActivationDate());
        Assertions.assertEquals(0,streamEntity.getEventAtomicCounter());
    }

    @Test
    void getStream() {
        streamEntityDaoDynamo.save(new StreamEntity("paId1","streamId1")).block();
        StreamEntity streamEntity = streamEntityDaoDynamo.get("paId1", "streamId1").block();
        assert streamEntity != null;
        Assertions.assertEquals("paId1", streamEntity.getPaId()) ;
        Assertions.assertEquals("streamId1", streamEntity.getStreamId());
        Assertions.assertNotNull(streamEntity.getActivationDate());
        Assertions.assertEquals(0,streamEntity.getEventAtomicCounter());
    }

    @Test
    void getWithRetryAfter_returnsTupleOfStreamEntityAndOptionalRetryAfterFound() {
        streamEntityDaoDynamo.save(new StreamEntity("paId7","streamId7")).block();
        StreamRetryAfter streamRetryAfter = new StreamRetryAfter();
        streamRetryAfter.setStreamId("streamId7");
        streamRetryAfter.setPaId("paId7");
        streamRetryAfter.setRetryAfter(Instant.now());
        streamEntityDaoDynamo.updateStreamRetryAfter(streamRetryAfter).block();

        Tuple2<StreamEntity, Optional<StreamRetryAfter>> tuple = streamEntityDaoDynamo.getWithRetryAfter("paId7", "streamId7").block();
        assert tuple != null;
        Assertions.assertEquals("paId7", tuple.getT1().getPaId());
        Assertions.assertEquals("streamId7", tuple.getT1().getStreamId());
        Assertions.assertEquals("paId7", tuple.getT2().get().getPaId());
        Assertions.assertEquals("RETRY#streamId7", tuple.getT2().get().getStreamId());
        Assertions.assertNotNull(tuple.getT2().get().getRetryAfter());
    }

    @Test
    void getWithRetryAfter_returnsTupleOfStreamEntityAndOptionalRetryAfterNotFound() {
        streamEntityDaoDynamo.save(new StreamEntity("paId8","streamId8")).block();
        Tuple2<StreamEntity, Optional<StreamRetryAfter>> tuple = streamEntityDaoDynamo.getWithRetryAfter("paId8", "streamId8").block();
        assert tuple != null;
        Assertions.assertEquals("paId8", tuple.getT1().getPaId());
        Assertions.assertEquals("streamId8", tuple.getT1().getStreamId());
        Assertions.assertFalse(tuple.getT2().isPresent());
    }

    @Test
    void delete() {
        streamEntityDaoDynamo.save(new StreamEntity("paId2","streamId2")).block();
        streamEntityDaoDynamo.delete("paId2", "streamId2").block();
        StreamEntity streamEntity = streamEntityDaoDynamo.get("paId2", "streamId2").block();
        Assertions.assertNull(streamEntity);
    }

    @Test
    void save() {
        StreamEntity streamEntity = streamEntityDaoDynamo.save(new StreamEntity("paId4","streamId4")).block();
        assert streamEntity != null;
        Assertions.assertEquals("paId4", streamEntity.getPaId()) ;
        Assertions.assertEquals("streamId4", streamEntity.getStreamId());
        Assertions.assertNotNull(streamEntity.getActivationDate());
        Assertions.assertEquals(0,streamEntity.getEventAtomicCounter());
    }

    @Test
    void update() {
        StreamEntity streamEntity = new StreamEntity("paId5","streamId5");
        streamEntity.setTitle("title");
        StreamEntity toReplace = new StreamEntity("paId5","streamId5");
        toReplace.setTitle("changedTitle");
        streamEntityDaoDynamo.save(new StreamEntity("paId5","streamId5")).block();
        streamEntityDaoDynamo.update(toReplace).block();
        StreamEntity updatedStreamEntity = streamEntityDaoDynamo.get("paId5", "streamId5").block();
        assert updatedStreamEntity != null;
        Assertions.assertEquals("changedTitle", updatedStreamEntity.getTitle());
    }

    @Test
    void updateAndGetAtomicCounter() {
        StreamEntity streamEntity = new StreamEntity("paId5","streamId5");
        streamEntity.setTitle("title");
        Long counter = streamEntityDaoDynamo.updateAndGetAtomicCounter(streamEntity).block();
        Assertions.assertEquals(1, counter);
    }

    @Test
    void replaceEntity() {
        StreamEntity streamEntity = new StreamEntity("paId8","streamId8");
        StreamEntity replaced = new StreamEntity("paId9","streamId9");

        streamEntityDaoDynamo.replaceEntity(streamEntity, replaced).block();
        StreamEntity updatedStreamEntity = streamEntityDaoDynamo.get("paId8", "streamId8").block();
        assert updatedStreamEntity != null;
        Assertions.assertNotNull(updatedStreamEntity.getDisabledDate());
        StreamEntity newStreamEntity = streamEntityDaoDynamo.get("paId9", "streamId9").block();
        assert newStreamEntity != null;
        Assertions.assertNull(newStreamEntity.getDisabledDate());

    }

    @Test
    void disable() {
        StreamEntity streamEntity = new StreamEntity("paId6","streamId6");
        streamEntityDaoDynamo.save(streamEntity).block();
        StreamEntity res = streamEntityDaoDynamo.disable(streamEntity).block();
        assert res != null;
        Assertions.assertNotNull(res.getDisabledDate());
    }
}
