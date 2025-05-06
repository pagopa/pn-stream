package it.pagopa.pn.stream.service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.pagopa.pn.stream.config.PnStreamConfigs;
import it.pagopa.pn.stream.config.springbootcfg.AbstractCachedSsmParameterConsumerActivation;
import it.pagopa.pn.stream.dto.CustomRetryAfterParameter;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactCategoryV20;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.LegalFactsIdV20;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.NotificationUnlockedEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.StreamEntity;
import it.pagopa.pn.stream.middleware.dao.mapper.DtoToEntityWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.EntityToDtoWebhookTimelineMapper;
import it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.mapper.webhook.WebhookTimelineElementJsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamUtilsTest {

    private DtoToEntityWebhookTimelineMapper timelineMapper;
    private WebhookTimelineElementJsonConverter timelineElementJsonConverter;
    private ObjectMapper objectMapper;

    private AbstractCachedSsmParameterConsumerActivation ssmParameterConsumerActivation;


    private StreamUtils streamUtils;

    @BeforeEach
    void setup() {
        timelineMapper = new DtoToEntityWebhookTimelineMapper();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        timelineElementJsonConverter = new WebhookTimelineElementJsonConverter(objectMapper);
        ssmParameterConsumerActivation = mock(AbstractCachedSsmParameterConsumerActivation.class);

        PnStreamConfigs webhook = new PnStreamConfigs();
        webhook.setScheduleInterval(1000L);
        webhook.setMaxLength(10);
        webhook.setPurgeDeletionWaittime(1000);
        webhook.setReadBufferDelay(1000);
        webhook.setMaxStreams(10);
        webhook.setTtl(Duration.ofDays(30));
        webhook.setCurrentVersion("v23");
        webhook.setRetryParameterPrefix("retryParameterPrefix");
        webhook.setNotificationSla(Duration.ofDays(2));
        webhook.setUnlockedEventTtl(Duration.ofDays(1));

        EntityToDtoWebhookTimelineMapper entityToDtoTimelineMapper = new EntityToDtoWebhookTimelineMapper();
        streamUtils = new StreamUtils(timelineMapper, entityToDtoTimelineMapper, timelineElementJsonConverter, webhook, ssmParameterConsumerActivation);
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
    void getVersionV1() {
        String streamVersion = "v10";
        int version = streamUtils.getVersion(streamVersion);

        assertEquals(10, version);

    }

    @Test
    void getVersionNull() {

        int version = streamUtils.getVersion(null);

        assertEquals(23, version);

    }

    private List<TimelineElementInternal> generateTimeline(String iun, String paId) {
        List<TimelineElementInternal> res = new ArrayList<>();
        Instant t0 = Instant.now();

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.REQUEST_ACCEPTED.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.REQUEST_ACCEPTED)
                .timestamp(t0)
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.AAR_GENERATION.name())
                .legalFactId(List.of(LegalFactsIdV20.builder().category(LegalFactCategoryV20.SENDER_ACK).key("KEY1").build(), LegalFactsIdV20.builder().category(LegalFactCategoryV20.SENDER_ACK).key("KEY2").build()))
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.AAR_GENERATION)
                .timestamp(t0.plusMillis(1000))
                .details("{\"recIndex\":\"0\"}")
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.name())
                .legalFactId(List.of(LegalFactsIdV20.builder().category(LegalFactCategoryV20.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                .timestamp(t0.plusMillis(1000))
                .details("{\"recIndex\":\"0\"}")
                .paId(paId)
                .build());
        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE.name())
                .legalFactId(List.of(LegalFactsIdV20.builder().category(LegalFactCategoryV20.PEC_RECEIPT).key("KEY1").build()))
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                .timestamp(t0.plusMillis(1000))
                .details("{\"recIndex\":\"0\"}")
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SEND_SIMPLE_REGISTERED_LETTER)
                .timestamp(t0.plusMillis(1000))
                .details("{\"recIndex\":\"0\"}")
                .paId(paId)
                .build());

        res.add(TimelineElementInternal.builder()
                .category(TimelineElementCategoryInt.SEND_COURTESY_MESSAGE.name())
                .iun(iun)
                .timelineElementId(iun + "_" + TimelineElementCategoryInt.SEND_COURTESY_MESSAGE)
                .timestamp(t0.plusMillis(1000))
                .details("{\"recIndex\":\"0\", \"digitalAddress\":{\"address\":\"\",\"type\":\"MAIL\"}}")
                .paId(paId)
                .build());

        return res;
    }

    @Test
    void retrieveRetryAfterWithValidInputs() {
        CustomRetryAfterParameter customRetryAfterParameter = new CustomRetryAfterParameter();
        customRetryAfterParameter.setRetryAfter(1000L);
        when(ssmParameterConsumerActivation.getParameterValue("retryParameterPrefix" + "xPagopaPnCxId", CustomRetryAfterParameter.class))
                .thenReturn(Optional.of(customRetryAfterParameter));

        Instant retryAfter = streamUtils.retrieveRetryAfter("xPagopaPnCxId");
        assertNotNull(retryAfter);
    }

    @Test
    void buildNotificationUnlockedEntityTtlEqualsNotificationSlaMaxTtl() {
        Instant notificationSentAt = Instant.parse("2021-01-01T00:00:00Z");

        NotificationUnlockedEntity result = streamUtils.buildNotificationUnlockedEntity("streamId", "iun", notificationSentAt);

        assertEquals("streamId_iun", result.getPk());
        assertEquals(notificationSentAt.plus(Duration.ofDays(2)).atZone(ZoneOffset.UTC).toEpochSecond(), result.getTtl());
    }

    @Test
    void checkIfTtlIsExpiredTrue(){
        Instant notificationSentAt = Instant.parse("2021-01-01T00:00:00Z");
        boolean result = streamUtils.checkIfTtlIsExpired(notificationSentAt);
        Assertions.assertTrue(result);
    }

    @Test
    void checkIfTtlIsExpiredFalse(){
        Instant notificationSentAt = Instant.now().minus(Duration.ofDays(1));
        boolean result = streamUtils.checkIfTtlIsExpired(notificationSentAt);
        Assertions.assertFalse(result);
    }
}