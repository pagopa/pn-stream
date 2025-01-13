package it.pagopa.pn.stream.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.stream.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.dto.timeline.details.*;
import it.pagopa.pn.stream.middleware.dao.notificationdao.NotificationDao;
import it.pagopa.pn.stream.middleware.dao.notificationdao.dynamo.entity.NotificationEntity;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.webhook.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.service.NotificationService;
import it.pagopa.pn.stream.service.StatusService;
import it.pagopa.pn.stream.service.TimelineService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class StreamUtilsTest {


    public static final String ERROR_CODE = "FILE_NOTFOUND";
    public static final String DETAIL = "Allegato non trovato. fileKey=81dde2a8-9719-4407-b7b3-63e7ea694869";
    private TimelineService timelineService;
    private StatusService statusService;
    private NotificationService notificationService;
    private PnStreamConfigs pnStreamConfigs;
    private DtoToEntityWebhookTimelineMapper timelineMapper;
    private WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private ObjectMapper objectMapper;
    private EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper;
    private NotificationDao notificationDao;

    private StreamUtils streamUtils;

    @BeforeEach
    void setup() {

        notificationService = Mockito.mock(NotificationService.class);
        pnStreamConfigs = Mockito.mock( PnStreamConfigs.class );
        timelineMapper = new DtoToEntityWebhookTimelineMapper();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        timelineElementJsonConverter = new WebhookTimelineElementJsonConverter(objectMapper);
        notificationDao = Mockito.mock(NotificationDao.class);

        PnStreamConfigs.Webhook webhook = new PnStreamConfigs.Webhook();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(10);
        webhook.setTtl(Duration.ofDays(30));
        webhook.setCurrentVersion("v23");
        Mockito.when(pnStreamConfigs.getWebhook()).thenReturn(webhook);

        streamUtils = new StreamUtils(notificationService, pnStreamConfigs, timelineMapper, entityToDtoTimelineMapper, timelineElementJsonConverter, notificationDao);
    }

    @Test
    void testGetNotification_Success() {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setGroups(Collections.singletonList("group1"));

        when(notificationDao.getNotificationEntity(anyString())).thenReturn(Mono.just(notificationEntity));

        List<String> result = streamUtils.getNotification("IUN-123");
        assertEquals(Collections.singletonList("group1"), result);
    }

    @Test
    void testGetNotification_FallbackSuccess() {
        when(notificationDao.getNotificationEntity(anyString())).thenReturn(Mono.empty());

        NotificationInt notificationInt = NotificationInt.builder().group("group1").build();

        when(notificationService.getNotificationByIun(anyString())).thenReturn(notificationInt);

        List<String> result = streamUtils.getNotification("IUN-123");
        assertEquals(Collections.singletonList("group1"), result);
    }

    @Test
    void testGetNotification_FallbackFailure() {
        when(notificationDao.getNotificationEntity(anyString())).thenReturn(Mono.empty());
        when(notificationService.getNotificationByIun(anyString())).thenThrow(new RuntimeException("Service error"));

        assertThrows(PnInternalException.class, () -> {
            streamUtils.getNotification("IUN-123");
        });
    }
    @Test
    void buildEventEntity() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(2); //SEND_DIGITAL_DOMICILE
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        EventEntity eventEntity = streamUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
    }

    @Test
    void buildEventEntity_2() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(1);          //AAR_GENERATION
        StreamEntity streamEntity = new StreamEntity("paid", "abc");
        EventEntity eventEntity = streamUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_3() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(3);          //SEND_ANALOG_DOMICILE
        StreamEntity streamEntity = new StreamEntity("paid", "abc");

        EventEntity eventEntity = streamUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_4() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(4);          //SEND_SIMPLE_REGISTERED_LETTER
        StreamEntity streamEntity = new StreamEntity("paid", "abc");

        EventEntity eventEntity = streamUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }


    @Test
    void buildEventEntity_5() {

        String iun = "IUN-ABC-123";
        String xpagopacxid = "PF-123456";

        List<TimelineElementInternal> timeline = generateTimeline(iun, xpagopacxid);
        TimelineElementInternal timelineElementInternal = timeline.get(5);          //SEND_SIMPLE_REGISTERED_LETTER
        StreamEntity streamEntity = new StreamEntity("paid", "abc");

        EventEntity eventEntity = streamUtils.buildEventEntity(1L, streamEntity, "ACCEPTED", timelineElementInternal);

        assertNotNull(eventEntity);
        assertEquals(StringUtils.leftPad("1", 38, "0"), eventEntity.getEventId());
        assertNotNull(eventEntity.getElement());
        assertNotNull(eventEntity.getTtl());
    }

    @Test
    void getVersionV1 (){
        String streamVersion = "v10";
        int version = streamUtils.getVersion(streamVersion);

        assertEquals(10, version);

    }
    @Test
    void getVersionNull (){

        int version = streamUtils.getVersion(null);

        assertEquals(23, version);

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
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.SENDER_ACK).key("KEY1").build(), LegalFactsIdInt.builder().category(LegalFactCategoryInt.SENDER_ACK).key("KEY2").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION )
                .timestamp(t0.plusMillis(1000))
                        .details(AarGenerationDetailsInt.builder()
                                .recIndex(1)
                                .build())
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE )
                .timestamp(t0.plusMillis(1000))
                .details(SendDigitalDetailsInt.builder()
                        .recIndex(1)
                                .build())
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .legalFactsIds(List.of(LegalFactsIdInt.builder().category(LegalFactCategoryInt.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_ANALOG_DOMICILE )
                .timestamp(t0.plusMillis(1000))
                .details(SendAnalogDetailsInt.builder()
                        .recIndex(1)
                        .analogCost(500)
                        .build())
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER )
                .timestamp(t0.plusMillis(1000))
                .details(SimpleRegisteredLetterDetailsInt.builder()
                        .recIndex(1)
                        .analogCost(500)
                        .build())
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .iun(iun)
                .elementId(iun + "_" + TimelineElementCategoryInt.SEND_COURTESY_MESSAGE )
                .timestamp(t0.plusMillis(1000))
                .details(SendCourtesyMessageDetailsInt.builder()
                        .recIndex(1)
                        .digitalAddress(CourtesyDigitalAddressInt.builder()
                                .type(CourtesyDigitalAddressInt.COURTESY_DIGITAL_ADDRESS_TYPE_INT.EMAIL)
                                .address("secret")
                                .build() )
                        .build())
                .paId(paId)
                .build());

        return res;
    }

}