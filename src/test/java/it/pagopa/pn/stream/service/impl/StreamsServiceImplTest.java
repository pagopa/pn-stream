package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.exceptions.PnStreamMaxStreamsCountReachedException;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamCreationRequestV25;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamListElement;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV25;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamRequestV25;
import it.pagopa.pn.stream.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.WebhookStreamRetryAfter;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.mapper.DtoToEntityStreamMapper;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.externalregistry.PnExternalRegistryClient;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.StreamsService;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class StreamsServiceImplTest {
    Duration d = Duration.ofMillis(3000);

    private StreamEntityDao streamEntityDao;
    private PnStreamConfigs pnStreamConfigs;
    private SchedulerService schedulerService;
    private StreamsService webhookService;
    private PnExternalRegistryClient pnExternalRegistryClient;

    private final int MAX_STREAMS = 5;

    @BeforeEach
    void setup() {
        streamEntityDao = Mockito.mock( StreamEntityDao.class );
        pnStreamConfigs = Mockito.mock( PnStreamConfigs.class );
        schedulerService = Mockito.mock(SchedulerService.class);
        pnExternalRegistryClient = Mockito.mock(PnExternalRegistryClient.class);

        MockitoAnnotations.initMocks(this);

        PnStreamConfigs.Webhook webhook = new PnStreamConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(MAX_STREAMS);
        webhook.setTtl(Duration.ofDays(30));
        webhook.setFirstVersion("v10");
        webhook.setCurrentVersion("v23");
        webhook.setDeltaCounter(1000);
        Mockito.when(pnStreamConfigs.getWebhook()).thenReturn(webhook);

        webhookService = new StreamsServiceImpl(streamEntityDao, schedulerService, pnStreamConfigs
            ,pnExternalRegistryClient);

        DtoToEntityStreamMapper mapper = new DtoToEntityStreamMapper(pnStreamConfigs);
    }

    @Test
    void createEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        StreamCreationRequestV25 req = new StreamCreationRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        StreamEntity pentity = new StreamEntity();
        pentity.setStreamId(uuid);
        pentity.setTitle(req.getTitle());
        pentity.setPaId(xpagopacxid);
        pentity.setEventType(req.getEventType().toString());
        pentity.setFilterValues(new HashSet<>());
        pentity.setActivationDate(Instant.now());


        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(List.of(pentity)));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }



    @Test
    void createEventStreamMaxReached() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamCreationRequestV25 req = new StreamCreationRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> sss = new ArrayList<>();
        for(int i = 0;i<MAX_STREAMS;i++) {
            StreamEntity pentity = new StreamEntity();
            pentity.setStreamId(UUID.randomUUID().toString());
            pentity.setTitle(req.getTitle());
            pentity.setPaId(xpagopacxid);
            pentity.setEventType(req.getEventType().toString());
            pentity.setFilterValues(new HashSet<>());
            pentity.setActivationDate(Instant.now());
            sss.add(pentity);
        }

        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(sss));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));

        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.createEventStream(xpagopapnuid, xpagopacxid,null,null, Mono.just(req));
        assertThrows(PnStreamMaxStreamsCountReachedException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).save(Mockito.any());
    }

    @Test
    void createEventStreamMaxReachedSkipDisabled() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamCreationRequestV25 req = new StreamCreationRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> sss = new ArrayList<>();
        for(int i = 0;i<MAX_STREAMS;i++) {
            StreamEntity pentity = new StreamEntity();
            pentity.setStreamId(UUID.randomUUID().toString());
            pentity.setTitle(req.getTitle());
            pentity.setPaId(xpagopacxid);
            pentity.setEventType(req.getEventType().toString());
            pentity.setFilterValues(new HashSet<>());
            pentity.setActivationDate(Instant.now());
            pentity.setDisabledDate(Instant.now());
            sss.add(pentity);
        }

        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(sss));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));

        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.createEventStream(xpagopapnuid, xpagopacxid,null,null, Mono.just(req));
        assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, times(1)).save(Mockito.any());
    }

    @Test
    void createEventStreamWithoutReplaceStreamIdSameGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo1"));

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.singletonList("gruppo1"));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }

    @Test
    void createEventStreamWithoutReplaceStreamIdNoGroupBodyGroupHeader() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Collections.EMPTY_LIST);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.EMPTY_LIST);


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void createEventStreamNoReplaceIdNoGroupBodyNoGroupHeader() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Collections.EMPTY_LIST);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.EMPTY_LIST);


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.EMPTY_LIST,null, Mono.just(req));

        //THEN
        assertDoesNotThrow(() -> res.block(d));
    }

    @Test
    void createEventStreamWithoutReplaceStreamIdNoGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Collections.EMPTY_LIST);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.EMPTY_LIST);


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }

    @Test
    void createEventStreamWithoutReplaceStreamIdSubGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Arrays.asList("gruppo1", "gruppo2"));

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Arrays.asList("gruppo1", "gruppo2","gruppo3"));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).save(Mockito.any());
    }

    @Test
    void createEventStreamWithoutReplaceStreamIdMoreGroups() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Arrays.asList("gruppo1", "gruppo2", "gruppo3", "gruppo4"));

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Arrays.asList("gruppo1", "gruppo2","gruppo3"));


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req));
        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }
    @Test
    void createEventStreamWithReplaceStreamIdSameGroupV10() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo1"), replacedStreamId);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.singletonList("gruppo1"));

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v10");
        replacedEntity.setEventAtomicCounter(3L);

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamRequestV25.EventTypeEnum.STATUS.name());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(replacedEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+replacedEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(Tuples.of(replacedEntity, retryEntity)));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).replaceEntity(Mockito.any(), Mockito.any());
    }

    @Test
    void createEventStreamWithReplaceIdSameGroupV23WithHeaderGroups() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo1"), replacedStreamId);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.singletonList("gruppo1"));

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v23");
        replacedEntity.setEventAtomicCounter(3L);
        replacedEntity.setGroups(Collections.singletonList("gruppo1"));

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamRequestV25.EventTypeEnum.STATUS.name());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(replacedEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+replacedEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(Tuples.of(replacedEntity, retryEntity)));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).replaceEntity(Mockito.any(), Mockito.any());
    }

    @Test
    void replaceStreamWithNoGroupV23WithHeaderGroups() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo1"), replacedStreamId);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.singletonList("gruppo1"));

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v23");
        replacedEntity.setEventAtomicCounter(3L);

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(replacedEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+replacedEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(Tuples.of(replacedEntity, retryEntity)));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
        Mockito.verify(streamEntityDao, never()).replaceEntity(Mockito.any(), Mockito.any());
    }

    @Test
    void createEventStreamNoGroupWithReplaceByHeaderWithGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo2"), replacedStreamId);

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v23");
        replacedEntity.setGroups(Collections.EMPTY_LIST);
        replacedEntity.setEventAtomicCounter(1L);

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamRequestV25.EventTypeEnum.STATUS.name());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(replacedEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+replacedEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(Tuples.of(replacedEntity, retryEntity)));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo2"),null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void createEventStreamWithReplaceStreamIdDifferentGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo2"), replacedStreamId);

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v10");

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());

        Mockito.when(streamEntityDao.get(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(replacedEntity));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void createEventStreamWithReplaceStreamIdViaExtReg() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo2"), replacedStreamId);

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v10");

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());

        Mockito.when(streamEntityDao.get(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(replacedEntity));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));
        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Collections.singletonList("gruppo1"));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));

        Mockito.verify(pnExternalRegistryClient).getGroups(Mockito.anyString(), Mockito.anyString());
    }
    @Test
    void createEventStreamWithReplaceStreamIdDisabled() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID replacedStreamId = UUID.randomUUID();
        StreamCreationRequestV25 req = createEventStreamRequest(Collections.singletonList("gruppo1"), replacedStreamId);

        StreamEntity replacedEntity = new StreamEntity();
        replacedEntity.setStreamId(replacedStreamId.toString());
        replacedEntity.setPaId(xpagopacxid);
        replacedEntity.setVersion("v10");
        replacedEntity.setDisabledDate(Instant.now());

        StreamEntity newEntity = new StreamEntity();
        newEntity.setPaId(xpagopacxid);
        newEntity.setStreamId(UUID.randomUUID().toString());
        newEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(replacedEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+replacedEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(Tuples.of(replacedEntity, retryEntity)));
        Mockito.when(streamEntityDao.replaceEntity(Mockito.any(), Mockito.any() )).thenReturn(Mono.just(newEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Collections.singletonList("gruppo1"),null, Mono.just(req));

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void createEventStreamDefaultV23() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamCreationRequestV25 req = createEventStreamRequest(Arrays.asList("gruppo1", "gruppo2"));

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Arrays.asList("gruppo1", "gruppo2","gruppo3"));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, null,null, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        ArgumentCaptor<StreamEntity> argument = ArgumentCaptor.forClass(StreamEntity.class);
        Mockito.verify(streamEntityDao).save(argument.capture());

        Assert.assertEquals(pnStreamConfigs.getWebhook().getCurrentVersion(), argument.getValue().getVersion());
    }

    @Test
    void createEventStreamOldVersion() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion = "v10";

        StreamCreationRequestV25 req = createEventStreamRequest(Collections.EMPTY_LIST);

        Mockito.when(pnExternalRegistryClient.getGroups(xpagopapnuid, xpagopacxid)).thenReturn(Arrays.asList("gruppo1", "gruppo2","gruppo3"));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.createEventStream(xpagopapnuid,xpagopacxid, Arrays.asList("gruppo1"),xPagopaPnApiVersion, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        ArgumentCaptor<StreamEntity> argument = ArgumentCaptor.forClass(StreamEntity.class);
        Mockito.verify(streamEntityDao).save(argument.capture());

        Assert.assertEquals(xPagopaPnApiVersion, argument.getValue().getVersion());
    }

    private StreamCreationRequestV25 createEventStreamRequest(List<String> requestGroups) {
        return createEventStreamRequest(requestGroups, null);
    }
    private StreamCreationRequestV25 createEventStreamRequest(List<String> requestGroups, UUID replacedStreamId) {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        StreamCreationRequestV25 req = new StreamCreationRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamCreationRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(requestGroups);
        req.setReplacedStreamId(replacedStreamId);

        String uuid = UUID.randomUUID().toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        StreamEntity pentity = new StreamEntity();
        pentity.setStreamId(uuid);
        pentity.setTitle(req.getTitle());
        pentity.setPaId(xpagopacxid);
        pentity.setEventType(req.getEventType().toString());
        pentity.setFilterValues(new HashSet<>());
        pentity.setActivationDate(Instant.now());

        Mockito.when(streamEntityDao.findByPa(Mockito.anyString())).thenReturn(Flux.fromIterable(List.of(pentity)));
        Mockito.when(streamEntityDao.save(Mockito.any())).thenReturn(Mono.just(entity));

        return req;
    }

    @Test
    void disableEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,null,null, toBeDisabledStreamId);
        res.block(d);
        //THEN
        Mockito.verify(streamEntityDao).disable(Mockito.any());
    }
    @Test
    void disableEventStream2() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");
        disabledEntity.setGroups(Collections.EMPTY_LIST);

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo1"),null, toBeDisabledStreamId);
        //THEN
        Assert.assertThrows(PnStreamForbiddenException.class, ()->res.block(d));
        Mockito.verify(streamEntityDao, never()).disable(Mockito.any());
    }

    @Test
    void disableEventStreamAlreadyDisabled() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");
        disabledEntity.setDisabledDate(Instant.now());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,null,null, toBeDisabledStreamId);
        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void disableEventStreamVersionMismatch() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion = "v23";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v10");
        disabledEntity.setDisabledDate(Instant.now());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,null,xPagopaPnApiVersion, toBeDisabledStreamId);
        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void disableEventStreamNotOwner() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion = "v23";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");
        disabledEntity.setDisabledDate(Instant.now());
        disabledEntity.setGroups(Arrays.asList("gruppo1","gruppo2"));

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo3"),xPagopaPnApiVersion, toBeDisabledStreamId);
        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }
    @Test
    void disableEventStreamOwner() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion = "v23";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamRequestV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");
        disabledEntity.setGroups(Arrays.asList("gruppo1","gruppo2"));

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo1","gruppo2"),xPagopaPnApiVersion, toBeDisabledStreamId);
        res.block(d);
        //THEN
        Mockito.verify(streamEntityDao).disable(Mockito.any());
    }

    @Test
    void disableEventStreamPartialOwner() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion = "v23";

        UUID toBeDisabledStreamId = UUID.randomUUID();

        StreamEntity disabledEntity = new StreamEntity();
        disabledEntity.setPaId(xpagopacxid);
        disabledEntity.setStreamId(toBeDisabledStreamId.toString());
        disabledEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.name());
        disabledEntity.setVersion("v23");
        disabledEntity.setGroups(Arrays.asList("gruppo1","gruppo2"));

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(disabledEntity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+disabledEntity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(Mockito.any(), Mockito.any())).thenReturn(Mono.just(Tuples.of(disabledEntity, retryEntity)));
        Mockito.when(streamEntityDao.disable(Mockito.any())).thenReturn(Mono.just(disabledEntity));

        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.disableEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo2"),xPagopaPnApiVersion, toBeDisabledStreamId);
        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
    }

    @Test
    void deleteEventStreamV10() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        //WHEN
        webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,"v10",uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void deleteEventStreamV10ByStdKey() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        //WHEN
        webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, Arrays.asList("gruppo1"),"v10",uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void deleteEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        //WHEN
        webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,null,uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void deleteEventStreamWithGroupByNoRequestGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Arrays.asList("gruppo1","gruppo2"));

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        //WHEN
        webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,null,uuidd).block(d);

        //THEN
        Mockito.verify(streamEntityDao).delete(xpagopacxid, uuid);
    }

    @Test
    void deleteEventStreamWithNoGroupByRequestGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Collections.emptyList());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());

        //WHEN
        Mono mono = webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, Arrays.asList("gruppo1"),null,uuidd);

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));
    }

    @Test
    void deleteEventStreamNotAllowed(){
        deleteEventStreamException(null, null);
        deleteEventStreamException("v23", null);
        deleteEventStreamException("v23", "v10");
    }

    void deleteEventStreamException(String xPagopaPnApiVersion, String entityVersion) {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion(entityVersion);

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());


        //WHEN
        var mono = webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, null,xPagopaPnApiVersion,uuidd);
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).delete(Mockito.any(), Mockito.any());
    }

    @Test
    void deleteEventStreamWithGroupsByNoGroups() {
        //GIVEN
        String xPagopaPnApiVersion="v23";
        String entityVersion = "v23";
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion(entityVersion);
        entity.setGroups(Arrays.asList("gruppo1"));

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.delete(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any());


        //WHEN
        var mono = webhookService.deleteEventStream(xpagopapnuid,xpagopacxid, Collections.EMPTY_LIST,xPagopaPnApiVersion,uuidd);
//        assertThrows(PnWebhookForbiddenException.class, () -> mono.block(d));
        mono.block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).delete(Mockito.any(), Mockito.any());
    }

    @Test
    void updateEventStreamNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,null,null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
    }

    @Test
    void updateEventStreamWithNoGroupByRequestHeaderWithGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Collections.emptyList());

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo1"),null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
    }

    @Test
    void updateEventStreamWithNoGroupByRequestWithNoGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Collections.emptyList());
        entity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Collections.EMPTY_LIST,null, uuidd, Mono.just(req));
        assertDoesNotThrow( () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).update(Mockito.any());
    }

    @Test
    void updateEventStreamWithNoGroupByRequestWithGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Arrays.asList("gruppo1"));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Collections.emptyList());
        entity.setVersion("v23");

        Mockito.when(pnExternalRegistryClient.getGroups(Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList("gruppo1","gruppo2"));
        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Collections.EMPTY_LIST,null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
        Mockito.verify(pnExternalRegistryClient, Mockito.never()).getGroups(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void updateEventStreamWithGroupByRequestWithGroupAddGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Arrays.asList("gruppo1","gruppo2"));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Arrays.asList("gruppo1"));
        entity.setVersion("v23");

        Mockito.when(pnExternalRegistryClient.getGroups(Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList("gruppo1","gruppo2"));
        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Collections.EMPTY_LIST,null, uuidd, Mono.just(req));
        assertDoesNotThrow( () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).update(Mockito.any());
        Mockito.verify(pnExternalRegistryClient, Mockito.times(1)).getGroups(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void updateEventStreamChangeGroupNotAllowed() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Arrays.asList("gruppo2"));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Arrays.asList("gruppo1"));
        entity.setVersion("v23");

        Mockito.when(pnExternalRegistryClient.getGroups(Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList("gruppo1","gruppo2"));
        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Collections.EMPTY_LIST,null, uuidd, Mono.just(req));
        assertThrows( PnStreamForbiddenException.class,() -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());
        Mockito.verify(pnExternalRegistryClient, Mockito.times(0)).getGroups(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void updateEventStreamWithGroupAddGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Arrays.asList("gruppo1","gruppo2"));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Arrays.asList("gruppo1"));
        entity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo1","gruppo2"),null, uuidd, Mono.just(req));
        assertDoesNotThrow( () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).update(Mockito.any());
    }

    @Test
    void updateEventStreamWithGroupDelGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Arrays.asList("gruppo1"));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Arrays.asList("gruppo1","gruppo2"));
        entity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Arrays.asList("gruppo1","gruppo2"),null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.never()).update(Mockito.any());

    }

    @Test
    void getEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.getEventStream(xpagopapnuid,xpagopacxid,null,null, uuidd).block(d);

        //THEN
        assertNotNull(res);
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }
    @Test
    void getEventStreamEmptyGroupByRequestWithGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(new ArrayList<>());


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        List<String> reqGroups = new ArrayList<>();
        reqGroups.add("gruppo1");
        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.getEventStream(xpagopapnuid,xpagopacxid,reqGroups,null, uuidd);

        //THEN
        assertDoesNotThrow(() -> mono.block(d));
    }
    @Test
    void getEventStreamWithRequestGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(new ArrayList<>());

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));

        List<String> reqGroups = new ArrayList<>();
        reqGroups.add("gruppo1");
        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.getEventStream(xpagopapnuid,xpagopacxid,reqGroups,null, uuidd);

        //THEN
        assertDoesNotThrow(() -> mono.block(d));
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }
    @Test
    void getEventStreamWrongVersion() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");
        entity.setGroups(Arrays.asList("gruppo1"));

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));

        List<String> reqGroups = new ArrayList<>();
        reqGroups.add("gruppo1");
        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.getEventStream(xpagopapnuid,xpagopacxid,reqGroups,null, uuidd);

        //THEN
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }
    @Test
    void getEventStreamByOtherGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Arrays.asList("gruppo2"));

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));

        List<String> reqGroups = new ArrayList<>();
        reqGroups.add("gruppo1");
        //WHEN
        Mono<StreamMetadataResponseV25> mono = webhookService.getEventStream(xpagopapnuid,xpagopacxid,reqGroups,null, uuidd);

        //THEN
        assertDoesNotThrow(() -> mono.block(d));
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }

    @Test
    void getEventStreamWithGroupByRequestNoGroup() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Arrays.asList("gruppo1"));


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.getEventStream(xpagopapnuid,xpagopacxid,Collections.emptyList(),null, uuidd);

        //THEN
        assertDoesNotThrow(() -> res.block(d));
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }
    @Test
    void getEventStreamByMaster() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Arrays.asList(new String[]{"gruppo1","gruppo2"}));
        entity.setVersion("v23");


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.getEventStream(xpagopapnuid,xpagopacxid,null,null, uuidd).block(d);

        //THEN
        assertNotNull(res);
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
    }
    @Test
    void listEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());

        List<StreamEntity> list = new ArrayList<>();
        list.add(entity);


        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));


        //WHEN
        List<StreamListElement> res = webhookService.listEventStream(xpagopapnuid, xpagopacxid,null,null).collectList().block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.size());
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
    }

    @Test
    void updateEventStreamV10() {

        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Collections.emptyList());

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");
        entity.setGroups(Collections.EMPTY_LIST);


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));

        //WHEN
        StreamMetadataResponseV25 res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, Arrays.asList("gruppo1"),"v10", uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).update(Mockito.any());
    }

    @Test
    void updateEventStreamV23() {
        updateEventStream("v23","v23");
    }
    @Test
    void updateEventStreamDefault() {
        updateEventStream(null,"v23");
    }
    void updateEventStream(String xPagopaPnApiVersion, String entityVersion) {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";

        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion(entityVersion);


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        StreamMetadataResponseV25 res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, null,xPagopaPnApiVersion, uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).update(Mockito.any());
    }


    @Test
    void updateEventStreamMaster() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        String xPagopaPnApiVersion="v23";
        String entityVersion="v23";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(Arrays.asList("CCCC","DDDD"));
        req.setGroups(Arrays.asList(new String[]{"gruppo1","gruppo2"}));

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setActivationDate(Instant.now());
        entity.setVersion(entityVersion);
        entity.setGroups(Arrays.asList(new String[]{"gruppo1","gruppo2"}));
        entity.setFilterValues(new HashSet(Arrays.asList("AAAA","BBBB")));


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));
        Mockito.when(pnExternalRegistryClient.getGroups(Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList("gruppo1","gruppo2"));


        //WHEN
        List<String> requestGroups = Collections.emptyList();
        StreamMetadataResponseV25 res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, requestGroups,xPagopaPnApiVersion, uuidd, Mono.just(req)).block(d);

        //THEN
        assertNotNull(res);

        Mockito.verify(streamEntityDao).update(Mockito.any());
    }
    @Test
    void updateEventStreamForbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, Collections.EMPTY_LIST,null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
        //THEN
        assertNotNull(res);

    }

    @Test
    void updateEventStreamNoGroupWithGroupInHeader() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo nuovo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Collections.EMPTY_LIST);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("titolo vecchio");
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(Collections.EMPTY_LIST);
        entity.setVersion("v23");

        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        //WHEN
        Mono<StreamMetadataResponseV25> res = webhookService.updateEventStream(xpagopapnuid,xpagopacxid, Arrays.asList("gruppo1"),null, uuidd, Mono.just(req));
        assertThrows(PnStreamForbiddenException.class, () -> res.block(d));
        //THEN
        assertNotNull(res);

    }

    private List<TimelineElementInternal> generateTimeline(String iun, String paId){
        List<TimelineElementInternal> res = new ArrayList<>();
        Instant t0 = Instant.now();

        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.REQUEST_ACCEPTED )
            .timestamp(t0)
            .paId(paId)
            .build());
        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.AAR_GENERATION)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
            .timestamp(t0.plusMillis(1000))
            .paId(paId)
            .build());
        res.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE )
            .timestamp(t0.plusMillis(1000))
            .paId(paId)
            .build());

        return res;
    }

    @Test
    void updateEventStreamPN11674() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xpagopapnuid = "PA-xpagopapnuid";
        StreamRequestV25 req = new StreamRequestV25();
        req.setTitle("titolo");
        req.setEventType(StreamRequestV25.EventTypeEnum.STATUS);
        req.setFilterValues(null);
        req.setGroups(Collections.EMPTY_LIST);

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle(req.getTitle());
        entity.setPaId(xpagopacxid);
        entity.setEventType(req.getEventType().toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(null);
        entity.setVersion(null);


        WebhookStreamRetryAfter retryEntity = new WebhookStreamRetryAfter();
        retryEntity.setPaId(entity.getPaId());
        retryEntity.setStreamId(WebhookStreamRetryAfter.RETRY_PREFIX+entity.getStreamId());
        retryEntity.setRetryAfter(Instant.now());

        Mockito.when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, retryEntity)));
        Mockito.when(streamEntityDao.update(Mockito.any())).thenReturn(Mono.just(entity));


        Mono<StreamMetadataResponseV25> mono = webhookService.updateEventStream(xpagopapnuid,xpagopacxid,Collections.singletonList("gruppo1"),"v10", uuidd, Mono.just(req));
        assertDoesNotThrow(() -> mono.block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).update(Mockito.any());
    }




}