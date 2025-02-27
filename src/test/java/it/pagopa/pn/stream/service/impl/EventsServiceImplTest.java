package it.pagopa.pn.stream.service.impl;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.delivery.model.SentNotificationV24;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.config.springbootcfg.AbstractCachedSsmParameterConsumerActivation;
import it.pagopa.pn.stream.dto.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.ProgressResponseElementDto;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.address.PhysicalAddressInt;
import it.pagopa.pn.stream.dto.ext.datavault.ConfidentialTimelineElementDtoInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import it.pagopa.pn.stream.dto.stats.StreamStatsEnum;
import it.pagopa.pn.stream.dto.timeline.StatusInfoInternal;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.exceptions.PnTooManyRequestException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactsIdV20;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamMetadataResponseV27;
import it.pagopa.pn.stream.middleware.dao.dynamo.*;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.stream.middleware.externalclient.pnclient.delivery.PnDeliveryClientReactive;
import it.pagopa.pn.stream.service.ConfidentialInformationService;
import it.pagopa.pn.stream.service.SchedulerService;
import it.pagopa.pn.stream.service.StreamStatsService;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV26.AAR_GENERATION;
import static it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV26.REQUEST_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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
    private StreamStatsDao streamStatsDao;
    @Mock
    private PnStreamConfigs pnStreamConfigs;
    @Mock
    private PnStreamConfigs.Stats stats;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private StreamStatsService streamStatsService;
    @Mock
    private AbstractCachedSsmParameterConsumerActivation ssmParameterConsumerActivation;
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
        when(pnStreamConfigs.getStats()).thenReturn(stats);
        when(pnStreamConfigs.getStats().getTtl()).thenReturn(Duration.ofDays(30));
        when(pnStreamConfigs.getStats().getSpanUnit()).thenReturn(1);
        when(pnStreamConfigs.getStats().getTimeUnit()).thenReturn(StatsTimeUnit.DAYS);
        when(pnStreamConfigs.getUnlockedEventTtl()).thenReturn(Duration.ofDays(1));
        when(pnStreamConfigs.getNotificationSla()).thenReturn(Duration.ofDays(2));
        when(pnStreamConfigs.getMaxTtl()).thenReturn(Duration.ofDays(2));
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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

        when(webhookUtils.getVersion("v10")).thenReturn(10);
        when(streamStatsService.updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.just(eventEntityBatch));
        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, null).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).getWithRetryAfter(xpagopacxid, uuid);
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateNumberOfReadingStreamStats(xpagopacxid,uuid, list.size());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        when(webhookUtils.getVersion("v10")).thenReturn(10);
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        when(streamStatsService.updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        when(webhookUtils.getTimelineInternalFromEvent(any())).thenReturn(timelineElementInternal);
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(eventEntityDao.findByStreamId(uuid, null)).thenReturn(Mono.just(eventEntityBatch));


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, null).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(list.size(), res.getProgressResponseElementList().size());
        Mockito.verify(streamEntityDao).getWithRetryAfter(xpagopacxid, uuid);
        Mockito.verify(streamStatsService).updateNumberOfReadingStreamStats(xpagopacxid,uuid, list.size());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid,uuid);
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), Mockito.any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        timelineElementInternal.setLegalFactsIds(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        when(streamStatsService.updateNumberOfReadingStreamStats(Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(2, res.getProgressResponseElementList().size());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateNumberOfReadingStreamStats(xpagopacxid,uuid, list.size());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid,uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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


        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.empty())));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(Mockito.any())).thenReturn(timelineElementInternal);
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);
        when(ssmParameterConsumerActivation.getParameterValue(any(), any())).thenReturn(Optional.empty());
        when(streamEntityDao.updateStreamRetryAfter(any())).thenReturn(Mono.empty());


        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(0, res.getProgressResponseElementList().size());
        Assertions.assertEquals(1000, res.getRetryAfter());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        timelineElementInternal.setLegalFactsIds(new ArrayList<>());
        timelineElementInternal.setStatusInfo(null);

        StreamRetryAfter streamRetryAfter = new StreamRetryAfter();
        streamRetryAfter.setPaId(xpagopacxid);
        streamRetryAfter.setStreamId(uuid);
        streamRetryAfter.setRetryAfter(Instant.now().plusMillis(10000));


        lasteventid = list.get(0).getEventId();

        when(streamEntityDao.getWithRetryAfter(xpagopacxid, uuid)).thenReturn(Mono.just(Tuples.of(entity, Optional.of(streamRetryAfter))));
        Mockito.doNothing().when(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        when(webhookUtils.getTimelineInternalFromEvent(any())).thenReturn(timelineElementInternal);
        when(streamStatsService.updateNumberOfReadingStreamStats(Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(Mono.empty());
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        when(eventEntityDao.findByStreamId(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(eventEntityBatch));
        when(webhookUtils.getVersion(xPagopaPnApiVersion)).thenReturn(10);

        //WHEN
        ProgressResponseElementDto res = webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d);

        //THEN
        assertNotNull(res);
        Assertions.assertEquals(2, res.getProgressResponseElementList().size());
        Mockito.verify(schedulerService).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateNumberOfReadingStreamStats(xpagopacxid,uuid, list.size());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid,uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        //WHEN
        Assertions.assertThrows(PnTooManyRequestException.class, () -> webhookEventsService.consumeEventStream(xpagopacxid, xPagopaPnCxGroups, xPagopaPnApiVersion, uuidd, lasteventid).block(d));
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid,uuid);
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(any(), any(), any() );
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        Mockito.verify(eventEntityDao, Mockito.never()).findByStreamId(Mockito.anyString(), Mockito.any());
        Mockito.verify(schedulerService, Mockito.never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);


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
                .legalFactsIds(List.of(LegalFactsIdV20.builder().key("key").category(LegalFactCategoryV20.DIGITAL_DELIVERY).build()))
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
                .legalFactsIds(List.of(LegalFactsIdV20.builder().key("key").category(LegalFactCategoryV20.DIGITAL_DELIVERY).build()))
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setGroups(groupsList);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationInt));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));

        //WHEN
        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).saveWithCondition(Mockito.any(EventEntity.class));
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        Mockito.verify(schedulerService, Mockito.never()).scheduleStreamEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

    }
    @Test
    void purgeEventsWithRetry() {
        //GIVEN
        String xpagopacxid = "PA-xpagopacxid";
        String lasteventid = "lasteventid";

        Mockito.when(eventEntityDao.delete(xpagopacxid, lasteventid, true))
                .thenReturn(Mono.just(true)).thenReturn(Mono.just(false));
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(notificationInt));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));

        //WHEN
        webhookEventsService.saveEvent(newtimeline).block(d);
        //THEN
        Mockito.verify(streamEntityDao).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(2)).saveWithCondition(Mockito.any(EventEntity.class));
        Mockito.verify(schedulerService, Mockito.times(2)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
        entity.setFilterValues(new HashSet<>());
        entity.getFilterValues().add(NotificationStatusInt.ACCEPTED.getValue());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));


        //WHEN
        webhookEventsService.saveEvent(timeline.get(0)).block(d);


        Mockito.when(webhookUtils.buildEventEntity(Mockito.anyLong(), Mockito.any(), Mockito.anyString(), Mockito.any())).thenReturn(eventEntity);

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(4)).saveWithCondition(Mockito.any(EventEntity.class));
        Mockito.verify(streamStatsService, Mockito.times(2)).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(schedulerService, Mockito.times(4)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(streamNotificationEntity));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));


        //WHEN
        webhookEventsService.saveEvent(newtimeline1 ).block(d);

        webhookEventsService.saveEvent(newtimeline2 ).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(3)).saveWithCondition(Mockito.any(EventEntity.class));
        Mockito.verify(schedulerService, Mockito.times(3)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
        entity.setFilterValues(new HashSet<>());
        entity.setActivationDate(Instant.now());
        entity.setEventAtomicCounter(1L);
        entity.setVersion("V10");
        list.add(entity);

        entity = new StreamEntity();
        entity.setStreamId(UUID.randomUUID().toString());
        entity.setTitle("2");
        entity.setPaId(xpagopacxid);
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(streamNotificationEntity));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));


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
        Mockito.verify(eventEntityDao, Mockito.times(6)).saveWithCondition(Mockito.any(EventEntity.class));
        Mockito.verify(streamStatsService, Mockito.times(3)).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(schedulerService, Mockito.times(6)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        streamEntity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
        streamEntity.setFilterValues(Set.of(TimelineElementCategoryInt.REQUEST_ACCEPTED.name()));
        streamEntity.setActivationDate(Instant.now());
        streamEntity.setEventAtomicCounter(1L);
        streamEntity.setGroups(groupsList);
        streamEntityList.add(streamEntity);

        Mockito.when(streamEntityDao.findByPa(xpagopacxid))
                .thenReturn(Flux.fromIterable(streamEntityList));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());


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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.just(new EventEntity()));
        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());



        //WHEN
        webhookEventsService.saveEvent(timeline.get(1))
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(2))
                .findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(0)).saveWithCondition(Mockito.any());
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        streamEntity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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

        SentNotificationV24 sentNotification = new SentNotificationV24();
        sentNotification.setGroup(authGroup);
        StreamNotificationEntity streamNotification = new StreamNotificationEntity();
        streamNotification.setGroup(authGroup);
        when(streamNotificationDao.findByIun(anyString())).thenReturn(Mono.empty());
        when(pnDeliveryClientReactive.getSentNotification(anyString())).thenReturn(Mono.just(sentNotification));
        when(streamNotificationDao.putItem(any())).thenReturn(Mono.just(streamNotification));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));

        Mockito.when(streamEntityDao.updateAndGetAtomicCounter(streamEntityList.get(0)))
                .thenReturn(Mono.just(2L));

        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any())).thenReturn(Mono.just(new EventEntity()));
        when(webhookUtils.getVersion("V23")).thenReturn(23);

        Mockito.when(streamStatsService.updateStreamStats(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());

        //WHEN
        webhookEventsService.saveEvent(newtimeline1)
                .block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .findByPa(xpagopacxid);
        Mockito.verify(streamEntityDao, Mockito.times(1))
                .updateAndGetAtomicCounter(Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(1))
                .saveWithCondition(Mockito.any());
        Mockito.verify(pnDeliveryClientReactive, Mockito.times(1))
                .getSentNotification(anyString());
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(streamStatsService).updateStreamStats(StreamStatsEnum.NUMBER_OF_WRITINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_EMPTY_READINGS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.NUMBER_OF_REQUESTS, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateStreamStats(StreamStatsEnum.RETRY_AFTER_VIOLATION, xpagopacxid, uuid);
        Mockito.verify(streamStatsService, Mockito.never()).updateNumberOfReadingStreamStats(Mockito.any(), Mockito.any(), Mockito.any());
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).saveWithCondition(Mockito.any(EventEntity.class));

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(Mockito.any())).thenReturn(Mono.just(unlockNotification));

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).saveWithCondition(Mockito.any(EventEntity.class));

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(Mockito.any())).thenReturn(Mono.empty());

        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventEntityDao, Mockito.times(1)).saveWithCondition(Mockito.any(EventEntity.class));

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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.STATUS.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(uuid+"_"+iun)).thenReturn(Mono.empty());
        Mockito.when(notificationUnlockedEntityDao.putItem(Mockito.any())).thenReturn(Mono.just(new NotificationUnlockedEntity()));
        Mockito.when(schedulerService.scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(true));


        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(notificationUnlockedEntityDao, Mockito.times(1)).putItem(Mockito.any());
        Mockito.verify(schedulerService, Mockito.times(1)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(1)).saveWithCondition(Mockito.any(EventEntity.class));
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
        entity.setEventType(StreamMetadataResponseV27.EventTypeEnum.TIMELINE.toString());
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
        Mockito.when(eventEntityDao.saveWithCondition(Mockito.any(EventEntity.class))).thenReturn(Mono.empty());
        Mockito.when(streamNotificationDao.findByIun(Mockito.anyString())).thenReturn(Mono.just(new StreamNotificationEntity()));
        Mockito.when(notificationUnlockedEntityDao.findByPk(uuid+"_"+iun)).thenReturn(Mono.empty());
        Mockito.when(eventsQuarantineEntityDao.putItem(Mockito.any())).thenReturn(Mono.just(new EventsQuarantineEntity()));


        webhookEventsService.saveEvent(newtimeline).block(d);

        //THEN
        Mockito.verify(streamEntityDao, Mockito.times(1)).findByPa(xpagopacxid);
        Mockito.verify(eventsQuarantineEntityDao, Mockito.times(1)).putItem(Mockito.any());

        Mockito.verify(notificationUnlockedEntityDao, Mockito.times(0)).putItem(Mockito.any());
        Mockito.verify(schedulerService, Mockito.times(0)).scheduleSortEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(eventEntityDao, Mockito.times(0)).saveWithCondition(Mockito.any(EventEntity.class));

    }
}