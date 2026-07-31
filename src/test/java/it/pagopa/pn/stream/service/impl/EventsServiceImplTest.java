package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV25;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.CustomRetryAfterParameter;
import it.pagopa.pn.stream.dto.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.ProgressResponseElementDto;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.exceptions.PnTooManyRequestException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.TimelineService;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV28.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventsServiceImplTest {
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
    private StreamUtils webhookUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private ConfidentialInformationService confidentialInformationService;
    @Mock
    private StreamNotificationDao streamNotificationDao;
    @Mock
    private PnDeliveryClientReactive pnDeliveryClientReactive;
    @Mock
    private UnlockedNotificationEntityDao notificationUnlockedEntityDao;
    @Mock
    private EventsQuarantineEntityDao eventsQuarantineEntityDao;

    Duration d = Duration.ofSeconds(3);

    private static final int CURRENT_VERSION = 26;


    @BeforeEach
    void setup() {
        when(pnStreamConfigs.getScheduleInterval()).thenReturn(1000L);
        when(pnStreamConfigs.getMaxLength()).thenReturn(10);
        when(pnStreamConfigs.getPurgeDeletionWaittime()).thenReturn(1000);
        when(pnStreamConfigs.getReadBufferDelay()).thenReturn(1000);
        when(pnStreamConfigs.getTtl()).thenReturn(Duration.ofDays(30));
        when(pnStreamConfigs.getFirstVersion()).thenReturn("v10");
        when(pnStreamConfigs.getListCategoriesPa()).thenReturn(List.of("AAR_GENERATION","REQUEST_ACCEPTED","SEND_DIGITAL_DOMICILE"));
        when(pnStreamConfigs.getUnlockedEventTtl()).thenReturn(Duration.ofDays(1));
        when(pnStreamConfigs.getNotificationSla()).thenReturn(Duration.ofDays(2));
        when(pnStreamConfigs.getMaxTtl()).thenReturn(Duration.ofDays(2));
        when(pnStreamConfigs.getSaveEventMaxConcurrency()).thenReturn(1);
    }

    private List<TimelineElementInternal> generateTimeline(String iun, String paId){
        List<TimelineElementInternal> res = new ArrayList<>();
        Instant t0 = Instant.now();

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.REQUEST_ACCEPTED )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(t0)
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("REFUSED").statusChanged(true).build())
                .timestamp(t0.plusMillis(1000))
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(t0.plusMillis(1000))
                .paId(paId)
                .build());

        return res;
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");

        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimelineElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails("{\"recIndex\":0,\"digitalAddressSource\":\"GENERAL\",\"isAvailable\":true,\"attemptDate\":\"2025-01-21T15:12:28.172984718Z\",\"nextSourceAttemptsMade\":0}");
        timelineElementInternal.setCategory(AAR_GENERATION.name());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactId(new ArrayList<>());
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

        when(webhookUtils.getVersion("v10")).thenReturn(10);
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(uuid, "00000000000000000000000000000000000001")).thenReturn(Mono.just(eventEntityBatch));
        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, "00000000000000000000000000000000000001").block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).getWithRetryAfter(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void consumeEventStreamWithLastEventId() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");

        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimelineElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails("{\"recIndex\":0,\"digitalAddressSource\":\"GENERAL\",\"isAvailable\":true,\"attemptDate\":\"2025-01-21T15:12:28.172984718Z\",\"nextSourceAttemptsMade\":0}");
        timelineElementInternal.setCategory(AAR_GENERATION.name());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactId(new ArrayList<>());
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

        when(webhookUtils.getVersion("v10")).thenReturn(10);
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(eventEntityDao.findByStreamId(uuid, "00000000000000000000000000000000000001")).thenReturn(Mono.just(eventEntityBatch));
        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, "00000000000000000000000000000000000001").block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).getWithRetryAfter(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void consumeEventStreamV10WithGroups() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = List.of("gruppo1");
        String xPagopaPnApiVersion = "v10";


        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);


        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimelineElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails("{\"recIndex\":0,\"digitalAddressSource\":\"GENERAL\",\"isAvailable\":true,\"attemptDate\":\"2025-01-21T15:12:28.172984718Z\",\"nextSourceAttemptsMade\":0}");
        timelineElementInternal.setCategory(AAR_GENERATION.name());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactId(new ArrayList<>());
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

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        when(webhookUtils.getVersion("v10")).thenReturn(10);
        when(webhookUtils.getTimelineInternalFromEvent(any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(uuid, "00000000000000000000000000000000000001")).thenReturn(Mono.just(eventEntityBatch));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, "00000000000000000000000000000000000001").block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).getWithRetryAfter(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    void consumeEventStream2Forbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xPagopaPnApiVersion = "v23";

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v23");
        entity.setGroups(Collections.emptyList());


        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, List.of("gruppo1"), xPagopaPnApiVersion, uuidd, null);
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, never()).findByStreamId(Mockito.anyString(), Mockito.any());
        Mockito.verify(schedulerService, never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);


        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimelineElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("Iun");
        timelineElementInternal.setDetails("{\"recIndex\":0,\"digitalAddressSource\":\"GENERAL\",\"isAvailable\":true,\"attemptDate\":\"2025-01-21T15:12:28.172984718Z\",\"nextSourceAttemptsMade\":0}");
        timelineElementInternal.setCategory(AAR_GENERATION.name());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactId(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(2, res.getProgressResponseElementList().size());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }


    @Test
    void consumeEventStreamNoEvents() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);


        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(Collections.emptyList());
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        CustomRetryAfterParameter retryAfterParameter = new CustomRetryAfterParameter();
        retryAfterParameter.setRetryAfter(1000L);


        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);
        when(streamEntityDao.updateStreamRetryAfter(any())).thenReturn(Mono.empty());
        when(webhookUtils.retrieveRetryAfter(any())).thenReturn(Long.parseLong("1000"));

        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(0, res.getProgressResponseElementList().size());
        Assertions.assertEquals(1000, res.getRetryAfter());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void consumeEventStreamRetryAfterViolationNoException() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);


        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        eventEntity.setEventDescription("2025-01-17T15:51:42.217434925Z_SEND_DIGITAL_FEEDBACK.IUN_DHZW-LJLR-RKXT-202501-D-1.RECINDEX_0.SOURCE_PLATFORM.REPEAT_false.ATTEMPT_0");
        eventEntity.setElement("{\"timelineElementId\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST.IUN_EWEU-VWQE-DQTL-202501-R-1\",\"iun\":\"EWEU-VWQE-DQTL-202501-R-1\",\"statusInfo\":{\"actual\":\"IN_VALIDATION\",\"statusChangeTimestamp\":\"2025-01-28T10:51:30.521908236Z\",\"statusChanged\":false},\"notificationSentAt\":\"2025-01-28T10:51:30.521908236Z\",\"ingestionTimestamp\":\"2025-01-28T10:52:00.126937484Z\",\"paId\":\"a95dace4-4a47-4149-a814-0e669113ce40\",\"legalFactIds\":[],\"details\":{\"nextSourceAttemptsMade\":0},\"category\":\"VALIDATE_NORMALIZE_ADDRESSES_REQUEST\",\"timestamp\":\"2025-01-28T10:52:00.126937484Z\",\"eventTimestamp\":\"2025-01-28T10:52:00.126937484Z\"}");
        list.add(eventEntity);

        EventEntityBatch eventEntityBatch = new EventEntityBatch();
        eventEntityBatch.setEvents(list);
        eventEntityBatch.setStreamId(uuid);
        eventEntityBatch.setLastEventIdRead(null);

        TimelineElementInternal timelineElementInternal = new TimelineElementInternal();
        timelineElementInternal.setTimelineElementId("id");
        timelineElementInternal.setTimestamp(Instant.now());
        timelineElementInternal.setIun("VNXR-JKEY-NYRW-202501-D-1");
        timelineElementInternal.setDetails("{\"recIndex\":0,\"digitalAddressSource\":\"GENERAL\",\"isAvailable\":true,\"attemptDate\":\"2025-01-21T15:12:28.172984718Z\",\"nextSourceAttemptsMade\":0}");
        timelineElementInternal.setCategory(AAR_GENERATION.name());
        timelineElementInternal.setPaId("PaId");
        timelineElementInternal.setLegalFactId(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        StreamRetryAfter streamRetryAfter = new StreamRetryAfter();
        streamRetryAfter.setPaId(xpagopacxid);
        streamRetryAfter.setStreamId(uuid);
        streamRetryAfter.setRetryAfter(Instant.now().plusMillis(10000));


        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.of(streamRetryAfter))));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(2, res.getProgressResponseElementList().size());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void consumeEventStreamRetryAfterViolationException() {
        when(pnStreamConfigs.getRetryAfterEnabled()).thenReturn(Boolean.TRUE);
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());


        List<EventEntity> list = new ArrayList<>();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        list.add(eventEntity);


        eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now().plusMillis(1) + "_" + "timeline_event_id2");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(AAR_GENERATION.name());
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

        StreamRetryAfter streamRetryAfter = new StreamRetryAfter();
        streamRetryAfter.setPaId(xpagopacxid);
        streamRetryAfter.setStreamId(uuid);
        streamRetryAfter.setRetryAfter(Instant.now().plusMillis(10000));


        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.of(streamRetryAfter))));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);

        //WHEN
        Assertions.assertThrows(PnTooManyRequestException.class, () -> webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d));
    }

    @Test
    void consumeEventStreamForbidden() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String xPagopaPnApiVersion = "v23";

        UUID uuidd = UUID.randomUUID();
        String uuid = uuidd.toString();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(uuid);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setVersion("v10");


        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.empty());


        //WHEN
        Mono<ProgressResponseElementDto> mono = webhookEventsService.consumeEventStream(xpagopacxid, null, xPagopaPnApiVersion, uuidd, null);
        assertThrows(PnStreamForbiddenException.class, () -> mono.block(d));

        //THEN
        Mockito.verify(eventEntityDao, never()).findByStreamId(Mockito.anyString(), Mockito.any());
        Mockito.verify(schedulerService, never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    void addConfidentialInformationAtEventTimelineList() {
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId("eventId");
        eventEntity.setIun("iun");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(REQUEST_ACCEPTED.getValue());
        eventEntity.setEventDescription("eventDescription");
        eventEntity.setNewStatus("newStatus");
        eventEntity.setStreamId("streamId");
        eventEntity.setChannel("channel");
        eventEntity.setElement("element");
        eventEntity.setNotificationRequestId("notificationRequestId");
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .timelineElementId("elementId")
                .category(REQUEST_ACCEPTED.getValue())
                .timestamp(Instant.now())
                .paId("paId")
                .statusInfo(StatusInfoInternal.builder().actual("actual").statusChanged(true).build())
                .legalFactId(List.of(LegalFactsIdV20.builder().key("key").category(LegalFactCategoryV20.DIGITAL_DELIVERY).build()))
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
        when(confidentialInformationService.getTimelineConfidentialInformation(List.of(timelineElementInternal)))
                .thenReturn(flux);

        Flux<EventTimelineInternalDto> fluxDto = webhookEventsService.addConfidentialInformationAtEventTimelineList(List.of(eventTimelineInternalDto));

        Assertions.assertNotNull(fluxDto);

        EventTimelineInternalDto dto = fluxDto.blockFirst();

        assert dto != null;
        Assertions.assertEquals("eventId", dto.getEventEntity().getEventId());
        Assertions.assertEquals("iun", dto.getEventEntity().getIun());
        Assertions.assertEquals("element", dto.getEventEntity().getElement());
        Assertions.assertEquals("newStatus", dto.getEventEntity().getNewStatus());
        Assertions.assertEquals(REQUEST_ACCEPTED.getValue(), dto.getEventEntity().getTimelineEventCategory());
        Assertions.assertEquals("streamId", dto.getEventEntity().getStreamId());
        Assertions.assertEquals("channel", dto.getEventEntity().getChannel());
        Assertions.assertEquals("notificationRequestId", dto.getEventEntity().getNotificationRequestId());

        Assertions.assertEquals("elementId", dto.getTimelineElementInternal().getTimelineElementId());
        Assertions.assertEquals(REQUEST_ACCEPTED.getValue(), dto.getTimelineElementInternal().getCategory());
        Assertions.assertEquals("paId", dto.getTimelineElementInternal().getPaId());
        Assertions.assertEquals("actual", dto.getTimelineElementInternal().getStatusInfo().getActual());
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
                .timelineElementId("elementId")
                .category(REQUEST_ACCEPTED.getValue())
                .timestamp(Instant.now())
                .paId("paId")
                .statusInfo(StatusInfoInternal.builder().actual("actual").statusChanged(true).build())
                .legalFactId(List.of(LegalFactsIdV20.builder().key("key").category(LegalFactCategoryV20.DIGITAL_DELIVERY).build()))
                .build();

        EventTimelineInternalDto eventTimelineInternalDto = EventTimelineInternalDto.builder()
                .eventEntity(eventEntity)
                .timelineElementInternal(timelineElementInternal)
                .build();

        when(confidentialInformationService.getTimelineConfidentialInformation(anyList())).thenReturn(Flux.error(new PnInternalException("error", 500, "error")));

        List<EventTimelineInternalDto> list = List.of(eventTimelineInternalDto);
        var resp = webhookEventsService.addConfidentialInformationAtEventTimelineList(list);

        Assertions.assertThrows(PnInternalException.class, resp::blockFirst);
    }

    @Test
    void checkIfReworkElementAndAddConfidentialInfoToRelated_enrichesRework() {
        ConfidentialTimelineElementDtoInt confidentialTimelineElementDtoInt = ConfidentialTimelineElementDtoInt.builder()
                .timelineElementId("elementId")
                .taxId("taxId")
                .denomination("denomination")
                .digitalAddress("digitalAddress")
                .physicalAddress(PhysicalAddressInt.builder().address("via address").build())
                .build();

        TimelineElementV28 reworkElement = new TimelineElementV28();
        TimelineElementDetailsV28 detail = new TimelineElementDetailsV28();
        TimelineElementV28 timelineElementV28 = new TimelineElementV28();
        timelineElementV28.setElementId("elementId");
        timelineElementV28.setCategory(SEND_ANALOG_FEEDBACK);
        List<TimelineElementV28> relatedTimelineElements = List.of(timelineElementV28);

        NotificationStatusHistoryInvalidatedElement historyElement = new NotificationStatusHistoryInvalidatedElement();
        historyElement.setRelatedTimelineElements(relatedTimelineElements);

        List<NotificationStatusHistoryInvalidatedElement> invalidatedTimelineAndStatusHistory = List.of(historyElement);

        detail.setInvalidatedTimelineAndStatusHistory(invalidatedTimelineAndStatusHistory);

        reworkElement.setCategory(NOTIFICATION_TIMELINE_REWORKED);
        reworkElement.setDetails(detail);
        ProgressResponseElementV29 element = new ProgressResponseElementV29();
        element.setElement(reworkElement);

        List<ProgressResponseElementV29> progressResponseElementsV29 = List.of(element);

        Flux<ConfidentialTimelineElementDtoInt> flux = Flux.just(confidentialTimelineElementDtoInt);

        when(confidentialInformationService.getTimelineConfidentialInformationFromConfidentialElementIds(any()))
                .thenReturn(flux);

        List<ProgressResponseElementV29> resultList = webhookEventsService.checkIfReworkElementAndAddConfidentialInfoToRelated(progressResponseElementsV29).block();

        Assertions.assertNotNull(resultList);

        ProgressResponseElementV29 dto = resultList.stream().findFirst().get();

        assert dto != null;
//        Assertions.assertEquals("eventId", dto.getEventEntity().getEventId());
//        Assertions.assertEquals("iun", dto.getEventEntity().getIun());
//        Assertions.assertEquals("element", dto.getEventEntity().getElement());
//        Assertions.assertEquals("newStatus", dto.getEventEntity().getNewStatus());
//        Assertions.assertEquals(REQUEST_ACCEPTED.getValue(), dto.getEventEntity().getTimelineEventCategory());
//        Assertions.assertEquals("streamId", dto.getEventEntity().getStreamId());
//        Assertions.assertEquals("channel", dto.getEventEntity().getChannel());
//        Assertions.assertEquals("notificationRequestId", dto.getEventEntity().getNotificationRequestId());
//
//        Assertions.assertEquals("elementId", dto.getTimelineElementInternal().getTimelineElementId());
//        Assertions.assertEquals(REQUEST_ACCEPTED.getValue(), dto.getTimelineElementInternal().getCategory());
//        Assertions.assertEquals("paId", dto.getTimelineElementInternal().getPaId());
//        Assertions.assertEquals("actual", dto.getTimelineElementInternal().getStatusInfo().getActual());
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(Set.of(TimelineElementCategoryInt.ANALOG_FAILURE_WORKFLOW.name()));
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        entity.setVersion("V10");
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
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);
        StreamNotificationEntity notificationInt = new StreamNotificationEntity();
        notificationInt.setGroup(authGroup);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED.name());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        when(webhookUtils.getVersion(anyString())).thenReturn(10);
        when(streamEntityDao.updateAndGetAtomicCounter(any())).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationInt));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
    }


    @Test
    void purgeEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";

        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true)).thenReturn(Mono.just(false));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }
    @Test
    void purgeEventsWithRetry() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";

        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true))
                .thenReturn(Mono.just(true));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());


        //WHEN
        webhookEventsService.purgeEvents(xpagopacxid, lasteventid, true).block(d);

        //THEN
        Mockito.verify(eventEntityDao).delete(xpagopacxid, lasteventid, true);
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);


        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);
        StreamNotificationEntity notificationInt = new StreamNotificationEntity();
        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + newtimeline.getTimelineElementId());
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun(iun);
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED.name());


        Mockito.when(webhookUtils.getVersion("V10")).thenReturn(10);
        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationInt));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");
        webhookEventsService.saveEvent(newtimeline).block(d);
        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(2)).save(Mockito.any(EventEntity.class));
        Mockito.verify(schedulerService, never()).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void saveEventDiagnosticElementIsSkipped() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";

        List<StreamEntity> list = new ArrayList<>();
        StreamEntity entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.COURTESY_CHANNEL_FAILED.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.COURTESY_CHANNEL_FAILED)
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .paId(xpagopacxid)
                .build();

        StreamNotificationEntity notificationInt = new StreamNotificationEntity();

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationInt));

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, never()).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(Mockito.any());
    }


    @Test
    void saveEventFiltered() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);
        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline = timeline.get(timeline.size()-1);

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");
        //WHEN
        webhookEventsService.saveEvent(timeline.get(0)).block(d);


        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(4)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void saveEventFilteredTimeline() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(TimelineElementCategoryInt.AAR_GENERATION.name());
        entity.setActivationDate(Instant.now());
        entity.setVersion("V23");
        entity.setEventAtomicCounter(1L);
        entity.setSorting(false);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setSorting(false);
        entity.setVersion("V23");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal newtimeline1 = timeline.get(timeline.size()-1);
        TimelineElementInternal newtimeline2 = timeline.get(timeline.size()-2);
        StreamNotificationEntity streamNotificationEntity = new StreamNotificationEntity();


        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory()).thenReturn(TimelineElementCategoryInt.AAR_GENERATION.name());


        TimelineElementInternal timelineElementInternal2 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal2.getCategory()).thenReturn(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name());

        Mockito.when(webhookUtils.getVersion("V23")).thenReturn(10);

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(streamNotificationEntity));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");


        //WHEN
        webhookEventsService.saveEvent(newtimeline1 ).block(d);

        webhookEventsService.saveEvent(newtimeline2 ).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(3)).save(Mockito.any(EventEntity.class));
        Mockito.verify(schedulerService, never()).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(2L);
        entity.setVersion("V10");
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        timeline.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST )
                .timestamp(Instant.now())
                .paId(xpagopacxid)
                .build());

        timeline.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.NOTIFICATION_CANCELLED.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.NOTIFICATION_CANCELLED )
                .timestamp(Instant.now())
                .paId(xpagopacxid)
                .build());

        timeline.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE )
                .timestamp(Instant.now())
                .paId(xpagopacxid)
                .build());

        StreamNotificationEntity streamNotificationEntity = new StreamNotificationEntity();

        Mockito.when(webhookUtils.getVersion("V10")).thenReturn(10);

        Mockito.doReturn(23)
                .when(webhookUtils)
                .getVersion("V23");

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(1))).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(streamNotificationEntity));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");


        TimelineElementInternal timelineElementInternal3 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal3.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLATION_REQUEST.name());


        TimelineElementInternal timelineElementInternal4 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal4.getCategory()).thenReturn(TimelineElementCategoryInt.NOTIFICATION_CANCELLED.name());

        TimelineElementInternal timelineElementInternal5 = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal5.getCategory()).thenReturn(TimelineElementCategoryInt.PROBABLE_SCHEDULING_ANALOG_DATE.name());

        //WHEN
        timeline.forEach(t -> webhookEventsService.saveEvent(t).block(d));

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(timeline.size())).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(6)).save(Mockito.any(EventEntity.class));
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
        streamEntity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of(TimelineElementCategoryInt.REQUEST_ACCEPTED.name()));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid))
                .thenReturn(Flux.fromIterable(streamEntityList));
        when(streamNotificationDao.findByIun(anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory())
                .thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED.name());

        //WHEN
        webhookEventsService.saveEvent(newtimeline1)
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
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));


        //WHEN
        webhookEventsService.saveEvent(timeline.get(1))
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2))
                .findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(0)).save(Mockito.any());
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
        streamEntity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of("DEFAULT"));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setVersion("V23");
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid))
                .thenReturn(Flux.fromIterable(streamEntityList));

        TimelineElementInternal timelineElementInternal = Mockito.mock(TimelineElementInternal.class);
        Mockito.when(timelineElementInternal.getCategory())
                .thenReturn(TimelineElementCategoryInt.REQUEST_ACCEPTED.name());

        SentNotificationV25 sentNotification = new SentNotificationV25();
        sentNotification.setGroup(authGroup);
        StreamNotificationEntity streamNotification = new StreamNotificationEntity();
        streamNotification.setGroup(authGroup);
        when(streamNotificationDao.findByIun(anyString())).thenReturn(Mono.empty());
        when(pnDeliveryClientReactive.getSentNotification(anyString())).thenReturn(Mono.just(sentNotification));
        when(streamNotificationDao.putItem(any())).thenReturn(Mono.just(streamNotification));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));

        Mockito.when(eventEntityDao.save(Mockito.any())).thenReturn(Mono.just(new EventEntity()));
        when(webhookUtils.getVersion("V23")).thenReturn(23);

        //WHEN
        webhookEventsService.saveEvent(newtimeline1)
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .updateAndGetAtomicCounter(Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(1))
                .save(Mockito.any());
        Mockito.verify(pnDeliveryClientReactive, Mockito.times(1))
                .getSentNotification(anyString());
    }

    @Test
    void sortStream_withSkipSortCategory() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-ABC-FGHI-A-1";
        String streamId = UUID.randomUUID().toString();

        StreamEntity entity = new StreamEntity();
        entity.setStreamId(streamId);
        entity.setTitle("1");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(streamId);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST )
                .statusInfo(StatusInfoInternal.builder().actual("IN_VALIDATION").statusChanged(false).build())
                .build();

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(anyString())).thenReturn(Flux.just(entity));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(entity)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void sortStream_enabledSorting() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(true);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .paId(xpagopacxid)
                .notificationSentAt(Instant.now())
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .build();

        NotificationUnlockedEntity unlockNotification = new NotificationUnlockedEntity();
        unlockNotification.setPk(uuid+"_"+iun);

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(Mockito.any())).thenReturn(Mono.just(unlockNotification));
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void sortStream_enabledSorting_unlockNotificationNotPresent_SLACompliance() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(true);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.AAR_GENERATION.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(false).build())
                .build();

        newtimeline.setNotificationSentAt(Instant.now().minus(Duration.ofHours(48)));

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(webhookUtils.checkIfTtlIsExpired(Mockito.any())).thenReturn(true);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void sortStream_unlockEventReceived() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(true);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.REQUEST_ACCEPTED.name());
        eventEntity.setNewStatus(NotificationStatusInt.ACCEPTED.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.REQUEST_ACCEPTED )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .build();

        newtimeline.setNotificationSentAt(Instant.now());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(uuid+"_"+iun)).thenReturn(Mono.empty());
        Mockito.when(notificationUnlockedEntityDao.putItem(Mockito.any())).thenReturn(Mono.just(new NotificationUnlockedEntity()));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn("test");
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(notificationUnlockedEntityDao, Mockito.times(1)).putItem(Mockito.any());
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void sortStream_notUnlockEventReceived() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(true);
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(false).build())
                .build();

        newtimeline.setNotificationSentAt(Instant.now());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(uuid+"_"+iun)).thenReturn(Mono.empty());
        Mockito.when(eventsQuarantineEntityDao.putItem(Mockito.any())).thenReturn(Mono.just(new EventsQuarantineEntity()));
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).putItem(Mockito.any());

        Mockito.verify(notificationUnlockedEntityDao, Mockito.times(0)).putItem(Mockito.any());
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(0)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void sortStream_notificationSentAt_before_activationDate() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(true);
        entity.setActivationDate(Instant.now().plus(Duration.ofDays(1)));
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(false).build())
                .notificationSentAt(Instant.now())
                .build();

        newtimeline.setNotificationSentAt(Instant.now());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void sortStream_notificationSentAt_before_activationDate_not_sorted() {
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
        entity.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setSorting(false);
        entity.setActivationDate(Instant.now().plus(Duration.ofDays(1)));
        list.add(entity);


        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + "timeline_event_id");
        eventEntity.setTimestamp(Instant.now());
        eventEntity.setTimelineEventCategory(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name());
        eventEntity.setNewStatus(NotificationStatusInt.DELIVERING.getValue());
        eventEntity.setIun("");
        eventEntity.setNotificationRequestId("");
        eventEntity.setStreamId(uuid);

        TimelineElementInternal newtimeline = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .iun(iun)
                .paId(xpagopacxid)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(false).build())
                .notificationSentAt(Instant.now())
                .build();

        newtimeline.setNotificationSentAt(Instant.now());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(list));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(list.get(0))).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));

    }

    @Test
    void saveEvent_informalEventGoesToInformalStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-0001";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");

        StreamEntity standardStream = new StreamEntity();
        standardStream.setStreamId(UUID.randomUUID().toString());
        standardStream.setPaId(xpagopacxid);
        standardStream.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        standardStream.setFilterValues(new HashSet<>());
        standardStream.setActivationDate(Instant.now());
        standardStream.setEventAtomicCounter(2L);
        standardStream.setVersion("V26");

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + informalEvent.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream, standardStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(informalStream)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: saved exactly once — only on the informal stream
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(standardStream);
    }

    @Test
    void saveEvent_standardEventDoesNotGoToInformalStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-STD-0001";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");

        StreamEntity standardStream = new StreamEntity();
        standardStream.setStreamId(UUID.randomUUID().toString());
        standardStream.setPaId(xpagopacxid);
        standardStream.setEventType(StreamMetadataResponseV29.EventTypeEnum.TIMELINE.toString());
        standardStream.setFilterValues(new HashSet<>());
        standardStream.setActivationDate(Instant.now());
        standardStream.setEventAtomicCounter(2L);
        standardStream.setVersion("V26");

        // Standard event: no communicationType
        TimelineElementInternal standardEvent = TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED.name())
                .iun(iun)
                .timelineElementId(iun + "_REQUEST_ACCEPTED")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + standardEvent.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream, standardStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(standardStream)).thenReturn(Mono.just(3L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(standardEvent).block(d);

        //THEN: saved exactly once — only on the standard stream
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(informalStream);
        Mockito.verify(streamEntityDao, Mockito.times(1)).updateAndGetAtomicCounter(standardStream);
    }

    @Test
    void saveEvent_informalStreamWithFilterValuesRejectsUnmatchedCategory() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-0002";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>(Set.of("INFORMAL_ACCEPTED")));
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_SENDING")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_SENDING")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("SENDING").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: category not in filterValues — event is skipped
        Mockito.verify(eventEntityDao, never()).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(Mockito.any());
    }

    @Test
    void saveEvent_informalEventWithNullStatusInfoDoesNotThrowNPE() {        //GIVEN: informal event without statusInfo (not applicable for informal notifications)
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-0003";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");

        TimelineElementInternal informalEventNoStatus = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(null)  // informal events may have no statusInfo
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + informalEventNoStatus.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.isNull(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(informalStream)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(informalEventNoStatus).block(d);

        //THEN: no NPE, event saved once
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void saveEvent_informalEventDoesNotGoToStatusStream() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-0004";

        StreamEntity statusStream = new StreamEntity();
        statusStream.setStreamId(UUID.randomUUID().toString());
        statusStream.setPaId(xpagopacxid);
        statusStream.setEventType(StreamMetadataResponseV29.EventTypeEnum.STATUS.toString());
        statusStream.setFilterValues(new HashSet<>());
        statusStream.setActivationDate(Instant.now());
        statusStream.setEventAtomicCounter(1L);
        statusStream.setVersion("V26");

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(statusStream)));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: informal event must not reach a STATUS stream
        Mockito.verify(eventEntityDao, never()).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(Mockito.any());
    }

    // ── Groups authorization tests for TIMELINE_INFORMAL ────────────────────

    @Test
    void saveEvent_informalStreamWithMatchingGroupAcceptsEvent() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-GRP-001";
        String matchingGroup = "GROUP-A";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");
        informalStream.setGroups(List.of(matchingGroup));

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        StreamNotificationEntity notificationEntity = new StreamNotificationEntity();
        notificationEntity.setGroup(matchingGroup);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + informalEvent.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(informalStream)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationEntity));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: group matches — event saved
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
    }

    @Test
    void saveEvent_informalStreamWithNonMatchingGroupRejectsEvent() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-GRP-002";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");
        informalStream.setGroups(List.of("GROUP-RESTRICTED"));

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        StreamNotificationEntity notificationEntity = new StreamNotificationEntity();
        notificationEntity.setGroup("GROUP-OTHER");  // different group

        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationEntity));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: group mismatch — event skipped
        Mockito.verify(eventEntityDao, never()).save(Mockito.any(EventEntity.class));
        Mockito.verify(streamEntityDao, never()).updateAndGetAtomicCounter(Mockito.any());
    }

    // ── Sorting interaction with TIMELINE_INFORMAL ───────────────────────────

    @Test
    void saveEvent_informalStreamSortingNullBypassesSortFlow() {
        // Informal streams do not set sorting; checkEventToSort must short-circuit
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-SORT-001";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.now());
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");
        // sorting is null — not set

        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(Instant.now())
                .paId(xpagopacxid)
                .build();

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + informalEvent.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(informalStream)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(informalEvent).block(d);

        //THEN: event saved — no quarantine, no unlock-event interaction
        Mockito.verify(eventEntityDao, Mockito.times(1)).save(Mockito.any(EventEntity.class));
        Mockito.verify(notificationUnlockedEntityDao, never()).findByPk(Mockito.any());
        Mockito.verify(eventsQuarantineEntityDao, never()).putItem(Mockito.any());
    }

    @Test
    void saveEvent_informalStreamSortingTrueEntersSortFlow() {
        // Sorting=true is not guarded for TIMELINE_INFORMAL: the sort flow is entered.
        // This test documents current behavior so regressions are visible if the guard is later added.
        String xpagopacxid = "PA-xpagopacxid";
        String iun = "IUN-INF-SORT-002";

        StreamEntity informalStream = new StreamEntity();
        informalStream.setStreamId(UUID.randomUUID().toString());
        informalStream.setPaId(xpagopacxid);
        informalStream.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStream.setFilterValues(new HashSet<>());
        informalStream.setActivationDate(Instant.parse("2025-01-01T00:00:00Z"));
        informalStream.setEventAtomicCounter(1L);
        informalStream.setVersion("V26");
        informalStream.setSorting(true);

        Instant notificationSentAt = Instant.parse("2025-01-02T00:00:00Z"); // after activationDate
        TimelineElementInternal informalEvent = TimelineElementInternal.builder()
                .category("INFORMAL_CATEGORY")
                .iun(iun)
                .timelineElementId(iun + "_INFORMAL_CATEGORY")
                .communicationType("INFORMAL")
                .statusInfo(StatusInfoInternal.builder().actual("ACCEPTED").statusChanged(true).build())
                .timestamp(Instant.now())
                .notificationSentAt(notificationSentAt)
                .paId(xpagopacxid)
                .build();

        EventEntity eventEntity = new EventEntity();
        eventEntity.setEventId(Instant.now() + "_" + informalEvent.getTimelineElementId());

        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(eventEntity);
        Mockito.when(webhookUtils.getVersion(Mockito.any())).thenReturn(CURRENT_VERSION);
        Mockito.when(webhookUtils.checkIfTtlIsExpired(Mockito.any())).thenReturn(false);
        Mockito.when(streamEntityDao.findByPa(xpagopacxid)).thenReturn(Flux.fromIterable(List.of(informalStream)));
        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(informalStream)).thenReturn(Mono.just(2L));
        Mockito.when(eventEntityDao.save(Mockito.any(EventEntity.class))).thenReturn(Mono.just(eventEntity));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        // No notificationUnlockedEntity found → event goes to quarantine
        Mockito.when(notificationUnlockedEntityDao.findByPk(Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(eventsQuarantineEntityDao.putItem(Mockito.any())).thenReturn(Mono.empty());

        webhookEventsService.saveEvent(informalEvent).block(d);

        // sorting=true with no unlock entity → event is quarantined (current behavior, not guarded for informal)
        Mockito.verify(notificationUnlockedEntityDao, Mockito.times(1)).findByPk(Mockito.any());
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).putItem(Mockito.any());
        // Not saved directly: went to quarantine path
        Mockito.verify(eventEntityDao, never()).save(Mockito.any(EventEntity.class));
    }

    // ── consumeEventStream for TIMELINE_INFORMAL ─────────────────────────────

    @Test
    void consumeEventStream_informalStreamReturnsEvents() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = new ArrayList<>();
        String xPagopaPnApiVersion = "v26";

        UUID streamUuid = UUID.randomUUID();
        String streamId = streamUuid.toString();

        StreamEntity informalStreamEntity = new StreamEntity();
        informalStreamEntity.setStreamId(streamId);
        informalStreamEntity.setPaId(xpagopacxid);
        informalStreamEntity.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStreamEntity.setFilterValues(new HashSet<>());
        informalStreamEntity.setActivationDate(Instant.now());
        informalStreamEntity.setVersion("v26");

        EventEntity ev1 = new EventEntity();
        ev1.setEventId("00000000000000000000000000000000000001");
        ev1.setTimestamp(Instant.now());
        ev1.setIun("IUN-INF-READ-001");
        ev1.setNotificationRequestId("cmVxSWQ=");
        ev1.setStreamId(streamId);
        ev1.setEventDescription("2025-01-17T15:51:42Z_AAR_GENERATION.IUN_INF-READ-001");
        // element must be non-empty so getTimelineInternalFromEventEntity delegates to the mock
        ev1.setElement("{\"timelineElementId\":\"AAR_GENERATION.IUN_INF-READ-001\",\"category\":\"AAR_GENERATION\",\"details\":{},\"legalFactIds\":[]}");

        EventEntityBatch batch = new EventEntityBatch();
        batch.setEvents(List.of(ev1));
        batch.setStreamId(streamId);
        batch.setLastEventIdRead(null);

        TimelineElementInternal timelineInternal = new TimelineElementInternal();
        timelineInternal.setTimelineElementId("AAR_GENERATION.IUN_INF-READ-001");
        timelineInternal.setTimestamp(Instant.now());
        timelineInternal.setIun("IUN-INF-READ-001");
        timelineInternal.setDetails("{\"nextSourceAttemptsMade\":0}");
        timelineInternal.setCategory(AAR_GENERATION.name());
        timelineInternal.setPaId(xpagopacxid);
        timelineInternal.setCommunicationType("INFORMAL");
        timelineInternal.setLegalFactId(new ArrayList<>());

        when(webhookUtils.getVersion("v26")).thenReturn(CURRENT_VERSION);
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineInternal);
        // Return non-zero to prove events-present path overrides retryAfter to 0 regardless
        when(webhookUtils.retrieveRetryAfter(xpagopacxid)).thenReturn(5000L);
        when(confidentialInformationService.getTimelineConfidentialInformation(Mockito.any())).thenReturn(Flux.empty());
        when(eventEntityDao.findByStreamId(streamId, null)).thenReturn(Mono.just(batch));
        when(streamEntityDao.getWithRetryAfter(xpagopacxid, streamId)).thenReturn(Mono.just(Tuples.of(informalStreamEntity, Optional.empty())));

        //WHEN
        ProgressResponseElementDto res = webhookEventsService
                .consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, streamUuid, null)
                .block(d);

        //THEN: events returned normally, retryAfter=0 regardless of retrieveRetryAfter value
        assertNotNull(res);
        Assertions.assertEquals(1, res.getProgressResponseElementList().size());
        Assertions.assertEquals(0, res.getRetryAfter()); // events present → always 0
        // lastEventId=null → no purge scheduled
        Mockito.verify(schedulerService, never()).scheduleStreamEvent(anyString(), any(), any(), any());
    }

    @Test
    void consumeEventStream_informalStreamEmptyReturnsRetryAfter() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        List<String> xPagopaPnCxGroups = new ArrayList<>();
        String xPagopaPnApiVersion = "v26";
        long expectedRetryAfter = 1000L;

        UUID streamUuid = UUID.randomUUID();
        String streamId = streamUuid.toString();

        StreamEntity informalStreamEntity = new StreamEntity();
        informalStreamEntity.setStreamId(streamId);
        informalStreamEntity.setPaId(xpagopacxid);
        informalStreamEntity.setEventType(StreamCreationRequestV29.EventTypeEnum.TIMELINE_INFORMAL.toString());
        informalStreamEntity.setFilterValues(new HashSet<>());
        informalStreamEntity.setActivationDate(Instant.now());
        informalStreamEntity.setVersion("v26");

        EventEntityBatch emptyBatch = new EventEntityBatch();
        emptyBatch.setEvents(Collections.emptyList());
        emptyBatch.setStreamId(streamId);

        when(webhookUtils.getVersion("v26")).thenReturn(CURRENT_VERSION);
        when(webhookUtils.retrieveRetryAfter(xpagopacxid)).thenReturn(expectedRetryAfter);
        when(confidentialInformationService.getTimelineConfidentialInformation(Mockito.any())).thenReturn(Flux.empty());
        when(eventEntityDao.findByStreamId(streamId, null)).thenReturn(Mono.just(emptyBatch));
        when(streamEntityDao.getWithRetryAfter(xpagopacxid, streamId)).thenReturn(Mono.just(Tuples.of(informalStreamEntity, Optional.empty())));
        when(streamEntityDao.updateStreamRetryAfter(Mockito.any())).thenReturn(Mono.empty());

        //WHEN
        ProgressResponseElementDto res = webhookEventsService
                .consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, streamUuid, null)
                .block(d);

        //THEN: no events — retryAfter is populated
        assertNotNull(res);
        Assertions.assertEquals(0, res.getProgressResponseElementList().size());
        Assertions.assertEquals((int) expectedRetryAfter, res.getRetryAfter());
    }
}