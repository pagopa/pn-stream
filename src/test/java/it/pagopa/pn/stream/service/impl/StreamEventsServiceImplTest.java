package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.stream.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.stream.ProgressResponseElementDto;
import it.pagopa.pn.stream.exceptions.PnNotFoundException;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.generated.openapi.server.webhook.v1.dto.StreamMetadataResponseV25;
import it.pagopa.pn.stream.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.stream.middleware.dao.webhook.EventEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.StreamEntityDao;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.EventEntityBatch;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.queue.producer.abstractions.streamspool.StreamEventType;
import it.pagopa.pn.stream.service.*;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamEventsServiceImplTest {
    @InjectMocks
    private StreamEventsServiceImpl webhookEventsService;
    @Mock
    private EventEntityDao eventEntityDao;
    @Mock
    private StreamEntityDao streamEntityDao;
    @Mock
    private PnStreamConfigs pnStreamConfigs;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private StreamUtils streamUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ConfidentialInformationService confidentialInformationService;

    Duration d = Duration.ofSeconds(3);

    @BeforeEach
    void setup() {
        PnStreamConfigs.Webhook webhook = new PnStreamConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setTtl(Duration.ofDays(30));
        webhook.setFirstVersion("v10");
        Mockito.when(pnStreamConfigs.getWebhook()).thenReturn(webhook);

        List<String> listCategoriesPa = new ArrayList<>(List.of("REQUEST_REFUSED", "REQUEST_ACCEPTED", "SEND_DIGITAL_DOMICILE", "SEND_DIGITAL_FEEDBACK",
                "DIGITAL_SUCCESS_WORKFLOW", "DIGITAL_FAILURE_WORKFLOW", "SEND_SIMPLE_REGISTERED_LETTER", "SEND_SIMPLE_REGISTERED_LETTER_PROGRESS",
                "SEND_ANALOG_DOMICILE", "SEND_ANALOG_PROGRESS", "SEND_ANALOG_FEEDBACK", "ANALOG_SUCCESS_WORKFLOW", "ANALOG_FAILURE_WORKFLOW",
                "COMPLETELY_UNREACHABLE", "REFINEMENT", "NOTIFICATION_VIEWED", "NOTIFICATION_CANCELLED", "NOTIFICATION_RADD_RETRIEVED"));
        Mockito.when(pnStreamConfigs.getListCategoriesPa()).thenReturn(listCategoriesPa);

        webhookEventsService = new StreamEventsServiceImpl(streamEntityDao, eventEntityDao, schedulerService,
                streamUtils, pnStreamConfigs, timelineService, confidentialInformationService);
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
    void saveEventNothingToDo() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup = "PA-groupID";
        String jsonElement = "{\"timelineElementId\": \"1234\",\"iun\": \"1234\"}";

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup);

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(Set.of(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW.getValue()));
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setElement(jsonElement);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.ACCEPTED;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(statusInfoInternal.isStatusChanged()).thenReturn(true);

        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);
        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);
        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);
        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);
        Mockito.when(streamEntityDao.findByPa(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));



        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);

        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(0)).save(any(EventEntity.class));
    }


    @Test
    void purgeEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true)).thenReturn(Mono.just(false));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());

    }
    @Test
    void purgeEventsWithRetry() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType("");
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true))
            .thenReturn(Mono.just(true)).thenReturn(Mono.just(false));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());

    }
    @Test
    void consumeEventStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = new ArrayList<>();
        String xPagopaPnApiVersion = "v10";


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


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails(null);
        timelineElementInternal.setCategory(TimelineElementCategoryInt.AAR_GENERATION);
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactsIds(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        ConfidentialTimelineElementDtoInt timelineElementDtoInt = new ConfidentialTimelineElementDtoInt();
        timelineElementDtoInt.toBuilder()
                .timelineElementId("id")
                .taxId("")
                .digitalAddress("")
                .physicalAddress(new PhysicalAddressInt())
                .newPhysicalAddress(new PhysicalAddressInt())
                .denomination("")
                .build();

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamUtils.getVersion("v10")).thenReturn(10);
        Mockito.when(streamUtils.getTimelineInternalFromEvent(eventEntity)).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.just(eventEntityBatch));



        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,xPagopaPnCxGroups,xPagopaPnApiVersion, uuidd, null).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
    }

    @Test
    void consumeEventStreamV10WithGroups() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = Arrays.asList("gruppo1");
        String xPagopaPnApiVersion = "v10";


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


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails(null);
        timelineElementInternal.setCategory(TimelineElementCategoryInt.AAR_GENERATION);
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactsIds(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        ConfidentialTimelineElementDtoInt timelineElementDtoInt = new ConfidentialTimelineElementDtoInt();
        timelineElementDtoInt.toBuilder()
            .timelineElementId("id")
            .taxId("")
            .digitalAddress("")
            .physicalAddress(new PhysicalAddressInt())
            .newPhysicalAddress(new PhysicalAddressInt())
            .denomination("")
            .build();

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamUtils.getVersion("v10")).thenReturn(10);
        Mockito.when(streamUtils.getTimelineInternalFromEvent(eventEntity)).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.just(eventEntityBatch));



        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,xPagopaPnCxGroups,xPagopaPnApiVersion, uuidd, null).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
    }

    @Test
    void consumeEventStream2Forbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = null;
        String xPagopaPnApiVersion = "v23";

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Collections.EMPTY_LIST);


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, lasteventid)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, Arrays.asList("gruppo1"), xPagopaPnApiVersion, uuidd, lasteventid);
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());

    }

//    @Test
    void consumeEventStreamV23() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = new ArrayList<>();
        String xPagopaPnApiVersion = "V23";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("V23");


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails(null);
        timelineElementInternal.setCategory(TimelineElementCategoryInt.AAR_GENERATION);
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactsIds(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        ConfidentialTimelineElementDtoInt timelineElementDtoInt = new ConfidentialTimelineElementDtoInt();
        timelineElementDtoInt.toBuilder()
                .timelineElementId("id")
                .taxId("")
                .digitalAddress("")
                .physicalAddress(new PhysicalAddressInt())
                .newPhysicalAddress(new PhysicalAddressInt())
                .denomination("")
                .build();

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.when(streamUtils.getVersion("v10")).thenReturn(10);
        Mockito.when(streamUtils.getTimelineInternalFromEvent(eventEntity)).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.just(eventEntityBatch));



        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,xPagopaPnCxGroups,xPagopaPnApiVersion, uuidd, null).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).get(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
    }



    @Test
    void consumeEventStreamNearEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid;
        List<String> xPagopaPnCxGroups = new ArrayList<>();
        String xPagopaPnApiVersion = "v10";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);



        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();


        lasteventid = list.get(0).getEventId();

        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(streamUtils.getTimelineInternalFromEvent(any())).thenReturn(timelineElementInternal);
        Mockito.when(eventEntityDao.findByStreamId(Mockito.anyString() , Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        Mockito.when(streamUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid,xPagopaPnCxGroups,xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        assertEquals(2, res.getProgressResponseElementList().size());
        Mockito.verify(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
    }


    @Test
    @Disabled("Test fail sometimes")
    void consumeEventStreamNotFound() {
        ConsoleAppenderCustom.initializeLog();
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = null;

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.empty());
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, lasteventid)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, null,null,uuidd, lasteventid);
        assertThrows(PnNotFoundException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        ConsoleAppenderCustom.checkLogs("[{}] {} - Error in reading stream");
        ConsoleAppenderCustom.checkAuditLog("BEFORE");
        ConsoleAppenderCustom.checkAuditLog("FAILURE");
    }

    @Test
    void consumeEventStreamForbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = null;
        String xPagopaPnApiVersion = "v23";

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");


        Mockito.when(streamEntityDao.get(xpagopacxid, uuid)).thenReturn(Mono.just(entity));
        Mockito.doNothing().when(schedulerService).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());
        Mockito.when(eventEntityDao.findByStreamId(uuid, lasteventid)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, null, xPagopaPnApiVersion, uuidd, lasteventid);
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleWebhookEvent(Mockito.anyString(), any(), any(), any());

    }

    @Test
    void saveEvent() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + newtimeline.getElementId());
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun(iun);
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.DELIVERING;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt1);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);

        Mockito.when(streamUtils.getVersion("V10")).thenReturn(10);

        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);

        Mockito.when(streamEntityDao.findByPa(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);
        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
    }


    @Test
    void saveEventFiltered() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        StatusService.NotificationStatusUpdate  statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        NotificationStatusInt notificationStatusInt = NotificationStatusInt.ACCEPTED;
        NotificationStatusInt notificationStatusInt1 = NotificationStatusInt.DELIVERING;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(statusInfoInternal.isStatusChanged()).thenReturn(true);

        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);
        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);
        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);
        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);
        Mockito.when(streamEntityDao.findByPa(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);

        // altro test
        statusUpdate = Mockito.mock(StatusService.NotificationStatusUpdate.class);
        notificationStatusInt = NotificationStatusInt.IN_VALIDATION;
        notificationStatusInt1 = NotificationStatusInt.ACCEPTED;
        Mockito.when(statusUpdate.getNewStatus()).thenReturn(notificationStatusInt1);
        Mockito.when(statusUpdate.getOldStatus()).thenReturn(notificationStatusInt);



        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);

        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
    }

    @Test
    void saveEventFilteredTimeline() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V23");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);


        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);

        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);

        Mockito.when(streamEntityDao.findByPa(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);

        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(4)).saveWithCondition(any(EventEntity.class));
    }


    @Test
    void saveEventFilteredTimelineV1() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.getValue());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        EventEntity eventEntity2 = new EventEntity();
        eventEntity2.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity2.setTimestamp(Instant.now());
        eventEntity2.setTimelineEventCategory(TimelineElementCategoryInt.DIGITAL_SUCCESS_WORKFLOW.getValue());
        eventEntity2.setNewStatus(NotificationStatusInt.DELIVERED.getValue());
        eventEntity2.setIun("");
        eventEntity2.setNotificationRequestId("");
        eventEntity2.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());

        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLED)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLED )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());

        timeline.add(TimelineElementInternal.builder()
            .category(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE)
            .iun(iun)
            .elementId(iun + "_" + TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE )
            .timestamp(Instant.now())
            .paId(xpagopacxid)
            .build());


        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);

        TimelineElementInternal timelineElementInternal2 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal2.getCategory()).thenReturn(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE);

        Mockito.when(streamUtils.getVersion("V10")).thenReturn(10);

        Mockito.doReturn(23)
                .when(streamUtils)
                .getVersion("V23");

        Mockito.when(streamUtils.buildEventEntity(Mockito.anyLong(), any(), Mockito.anyString(), any())).thenReturn(eventEntity);
        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);

        Mockito.when(streamEntityDao.findByPa(any())).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        TimelineElementInternal timelineElementInternal3 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal3.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST);

        TimelineElementInternal timelineElementInternal4 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal4.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLED);
        TimelineElementInternal timelineElementInternal5 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal5.getCategory()).thenReturn(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE);
        //WHEN
        timeline.forEach(t -> {
            webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT).block(d);
        });

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(timeline.size())).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(12)).saveWithCondition(any(EventEntity.class));
    }

    @Test
    void saveEventWhenGroupIsUnauthorizedOrWhenIsAuthorized() {
        //UNAUTHORIZED CASE
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup1 = "PA-1groupID";
        String authGroup2 = "PA-2groupID";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup1);

        List<StreamEntity> streamEntityList = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(uuid);
        streamEntity.setStreamId(uuid);
        streamEntity.setTitle("1");
        streamEntity.setPaId(xpagopacxid);
        streamEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of(TimelineElementCategoryInt.REQUEST_ACCEPTED.getValue()));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(any()))
                .thenReturn(Flux.fromIterable(streamEntityList));

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(3L));
        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);

        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT)
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);



        //AUTHORIZED CASE
        groupsList.clear();
        groupsList.add(authGroup2);
        streamEntity.setGroups(groupsList);

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(any()))
                .thenReturn(Mono.just(new EventEntity()));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT)
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2))
                .findByPa(xpagopacxid);
    }

    @Test
    void saveEventWhenFilteredValueIsDefaultCategoriesPA() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String authGroup = "PA-1groupID";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);

        List<String> groupsList = new ArrayList<>();
        groupsList.add(authGroup);

        List<StreamEntity> streamEntityList = new ArrayList<>();
        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity streamEntity = new StreamEntity();
        streamEntity.setStreamId(uuid);
        streamEntity.setStreamId(uuid);
        streamEntity.setTitle("1");
        streamEntity.setPaId(xpagopacxid);
        streamEntity.setEventType(StreamMetadataResponseV25.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of("DEFAULT"));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(any()))
                .thenReturn(Flux.fromIterable(streamEntityList));

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        StatusInfoInternal statusInfoInternal = Mockito.mock(StatusInfoInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED);
        Mockito.when(timelineElementInternal.getStatusInfo()).thenReturn(statusInfoInternal);
        Mockito.when(statusInfoInternal.getActual()).thenReturn("status");
        Mockito.when(statusInfoInternal.isStatusChanged()).thenReturn(true);

        Mockito.when(timelineElementInternal.getPaId()).thenReturn(xpagopacxid);
        Mockito.when(streamUtils.getVersion("V23")).thenReturn(10);
        Mockito.when(eventEntityDao.saveWithCondition(any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamUtils.getNotification(any())).thenReturn(List.of("group"));

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));

        Mockito.when(eventEntityDao.save(any()))
                .thenReturn(Mono.just(new EventEntity()));

        //WHEN
        webhookEventsService.saveEvent(timelineElementInternal, StreamEventType.REGISTER_EVENT)
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);
    }

    @Test
    void addConfidentialInformationAtEventTimelineList() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId("eventId");
        eventEntity.setIun("iun");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.REQUEST_ACCEPTED.getValue());
        eventEntity.setEventDescription("eventDescription");
        eventEntity.setNewStatus("newStatus");
        eventEntity.setStreamId("streamId");
        eventEntity.setChannel("channel");
        eventEntity.setElement("element");
        eventEntity.setNotificationRequestId("notificationRequestId");
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("elementId")
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .timestamp(Instant.now())
                .paId("paId")
                .statusInfo(StatusInfoInternal.builder().actual("actual").statusChanged(true).build())
                .legalFactsIds(List.of(LegalFactsIdInt.builder().key("key").category(LegalFactCategoryInt.DIGITAL_DELIVERY).build()))
                .build();

        EventTimelineInternalDto eventTimelineInternalDto = EventTimelineInternalDto.builder()
                .eventEntity(eventEntity)
                .timelineElementInternal(timelineElementInternal)
                .build();

        ConfidentialTimelineElementDtoInt confidentialTimelineElementDtoInt = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId("elementId")
                .taxId("taxId")
                .denomination("denomination")
                .digitalAddress("digitalAddress")
                .physicalAddress(PhysicalAddressInt.builder().address("via address").build())
                .build();

        Flux<ConfidentialTimelineElementDtoInt> flux = Flux.just(confidentialTimelineElementDtoInt);
        Mockito.when(confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal)))
                .thenReturn(flux);

        Flux<EventTimelineInternalDto> fluxDto = webhookEventsService.addConfidentialInformationAtEventTimelineList(List.of(eventTimelineInternalDto));

        Assertions.assertNotNull(fluxDto);

        EventTimelineInternalDto dto = fluxDto.blockFirst();

        Assertions.assertEquals("eventId", dto.getEventEntity().getEventId());
        Assertions.assertEquals("iun", dto.getEventEntity().getIun());
        Assertions.assertEquals("element", dto.getEventEntity().getElement());
        Assertions.assertEquals("newStatus", dto.getEventEntity().getNewStatus());
        Assertions.assertEquals(TimelineElementCategoryInt.REQUEST_ACCEPTED.getValue(), dto.getEventEntity().getTimelineEventCategory());
        Assertions.assertEquals("streamId", dto.getEventEntity().getStreamId());
        Assertions.assertEquals("channel", dto.getEventEntity().getChannel());
        Assertions.assertEquals("notificationRequestId", dto.getEventEntity().getNotificationRequestId());

        Assertions.assertEquals("elementId", dto.getTimelineElementInternal().getElementId());
        Assertions.assertEquals(TimelineElementCategoryInt.REQUEST_ACCEPTED, dto.getTimelineElementInternal().getCategory());
        Assertions.assertEquals("paId", dto.getTimelineElementInternal().getPaId());
        Assertions.assertEquals("actual", dto.getTimelineElementInternal().getStatusInfo().getActual());
        Assertions.assertEquals("key", dto.getTimelineElementInternal().getLegalFactsIds().get(0).getKey());
        Assertions.assertEquals(LegalFactCategoryInt.DIGITAL_DELIVERY, dto.getTimelineElementInternal().getLegalFactsIds().get(0).getCategory());
    }

    @Test
    void addConfidentialInformationAtEventTimelineListKo() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId("eventId");
        eventEntity.setIun("iun");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setEventDescription("eventDescription");
        eventEntity.setNewStatus("newStatus");
        eventEntity.setStreamId("streamId");
        eventEntity.setChannel("channel");
        eventEntity.setElement("element");
        eventEntity.setNotificationRequestId("notificationRequestId");

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .elementId("elementId")
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .timestamp(Instant.now())
                .paId("paId")
                .statusInfo(StatusInfoInternal.builder().actual("actual").statusChanged(true).build())
                .legalFactsIds(List.of(LegalFactsIdInt.builder().key("key").category(LegalFactCategoryInt.DIGITAL_DELIVERY).build()))
                .build();

        EventTimelineInternalDto eventTimelineInternalDto = EventTimelineInternalDto.builder()
                .eventEntity(eventEntity)
                .timelineElementInternal(timelineElementInternal)
                .build();

        Mockito.when(confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal))).thenThrow(PnInternalException.class);

        Assertions.assertThrows(PnInternalException.class, () -> webhookEventsService.addConfidentialInformationAtEventTimelineList(List.of(eventTimelineInternalDto)).blockFirst());
    }
}