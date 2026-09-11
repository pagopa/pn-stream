package it.pagopa.pn.stream.service.mapper;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.EventTimelineInternalDto;
import it.pagopa.pn.stream.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.InformalNotificationStatusV1;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.NotificationStatusV26;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.ProgressResponseElementV30;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.TimelineElementCategoryV28;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.EventEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.entity.RefusedReasonEntity;
import it.pagopa.pn.stream.middleware.dao.dynamo.mapper.EntityToDtoRefusedReasonMapper;
import it.pagopa.pn.stream.utils.CommunicationTypeUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProgressResponseElementMapperTest {

    private static EventEntity buildBaseEventEntity() {
        EventEntity ev = new EventEntity();
        ev.setEventId("00000000000000000000000000000000000001");
        ev.setTimestamp(Instant.parse("2026-09-01T10:15:30.00Z"));
        ev.setIun("ABCD-EFGH-ILMN-202609-A-1");
        ev.setNotificationRequestId("REQ-12345");
        ev.setChannel("PEC");
        ev.setRecipientIndex(0);
        ev.setAnalogCost(100);
        ev.setValidationErrors(null);
        ev.setElement(null);
        ev.setTimelineEventCategory(null);
        ev.setNewStatus(null);
        ev.setLegalfactIds(null);
        return ev;
    }

    private static TimelineElementInternal buildTimelineElementInternal(CommunicationType communicationType) {
        return TimelineElementInternal.builder()
                .iun("ABCD-EFGH-ILMN-202609-A-1")
                .timelineElementId("TIMELINE-ELEMENT-1")
                .timestamp(Instant.parse("2026-09-01T10:15:00.00Z"))
                .paId("PA-001")
                .communicationType(communicationType)
                .category("REQUEST_ACCEPTED")
                .details("{}")
                .build();
    }

    private static EventTimelineInternalDto buildDto(EventEntity ev, TimelineElementInternal timelineElementInternal) {
        return EventTimelineInternalDto.builder()
                .eventEntity(ev)
                .timelineElementInternal(timelineElementInternal)
                .build();
    }

    private static void assertBaseFieldsMatch(EventEntity ev, ProgressResponseElementV30 result) {
        assertEquals(ev.getEventId(), result.getEventId());
        assertEquals(ev.getTimestamp(), result.getTimestamp());
        assertEquals(ev.getIun(), result.getIun());
        assertEquals(ev.getNotificationRequestId(), result.getNotificationRequestId());
        assertEquals(ev.getChannel(), result.getChannel());
        assertEquals(ev.getRecipientIndex(), result.getRecipientIndex());
        assertEquals(ev.getAnalogCost(), result.getAnalogCost());
    }

    @Test
    @DisplayName("element == null: vengono mappati solo i campi base")
    void shouldMapOnlyBaseFieldsWhenElementIsNull() {
        EventEntity ev = buildBaseEventEntity();
        ev.setElement(null);
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertBaseFieldsMatch(ev, result);
        assertNull(result.getCommunicationType());
        assertNull(result.getElement());
        assertNull(result.getInformalElement());
        assertNull(result.getLegalfactIds());
        assertNull(result.getNewStatus());
        assertNull(result.getInformalNewStatus());
        assertNull(result.getTimelineEventCategory());
    }

    @Test
    @DisplayName("element blank: vengono mappati solo i campi base")
    void shouldMapOnlyBaseFieldsWhenElementIsBlank() {
        EventEntity ev = buildBaseEventEntity();
        ev.setElement("   ");
        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.INFORMAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertBaseFieldsMatch(ev, result);
        assertNull(result.getCommunicationType());
        assertNull(result.getElement());
        assertNull(result.getInformalElement());
    }

    // ---------------------------------------------------------------------
    // Caso: communicationType risolto == LEGAL
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("valorizza element, legalfactIds, timelineEventCategory e newStatus")
    void shouldMapLegalFieldsCorrectly() {
        EventEntity ev = buildBaseEventEntity();
        ev.setElement("SOME_ELEMENT");
        ev.setLegalfactIds(List.of("legal-fact-1", "legal-fact-2"));

        TimelineElementCategoryV28 expectedCategory = TimelineElementCategoryV28.values()[0];
        ev.setTimelineEventCategory(expectedCategory.getValue());

        NotificationStatusV26 expectedStatus = NotificationStatusV26.values()[0];
        ev.setNewStatus(expectedStatus.name());

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertBaseFieldsMatch(ev, result);

        CommunicationType expectedResolvedType =
                CommunicationTypeUtils.getDefaultCommunicationType(timelineElementInternal.getCommunicationType());
        assertEquals(
                it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType.fromValue(expectedResolvedType.name()),
                result.getCommunicationType());

        assertEquals(expectedCategory, result.getTimelineEventCategory());
        assertEquals(ev.getLegalfactIds(), result.getLegalfactIds());
        assertEquals(expectedStatus, result.getNewStatus());

        assertNotNull(result.getElement());
        assertEquals(TimelineElementMapper.internalToExternal(timelineElementInternal), result.getElement());

        // campi informal non devono essere valorizzati nel ramo LEGAL
        assertNull(result.getInformalElement());
        assertNull(result.getInformalNewStatus());
    }

    @Test
    @DisplayName("timelineEventCategory e newStatus null su EventEntity restano null nel risultato")
    void shouldLeaveCategoryAndStatusNullWhenNotSetOnEntity() {
        EventEntity ev = buildBaseEventEntity();
        ev.setElement("SOME_ELEMENT");
        ev.setTimelineEventCategory(null);
        ev.setNewStatus(null);

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertNull(result.getTimelineEventCategory());
        assertNull(result.getNewStatus());
        assertNotNull(result.getElement());
    }

    // ---------------------------------------------------------------------
    // Caso: communicationType risolto == INFORMAL
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("valorizza informalElement e informalNewStatus, non tocca i campi legal")
    void shouldMapInformalFieldsCorrectly() {
        EventEntity ev = buildBaseEventEntity();
        ev.setElement("SOME_ELEMENT");

        InformalNotificationStatusV1 expectedInformalStatus = InformalNotificationStatusV1.values()[0];
        ev.setNewStatus(expectedInformalStatus.name());

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.INFORMAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertBaseFieldsMatch(ev, result);

        CommunicationType expectedResolvedType =
                CommunicationTypeUtils.getDefaultCommunicationType(timelineElementInternal.getCommunicationType());
        assertEquals(
                it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType.fromValue(expectedResolvedType.name()),
                result.getCommunicationType());

        assertNotNull(result.getInformalElement());
        assertEquals(TimelineElementMapper.internalToInformalExternal(timelineElementInternal), result.getInformalElement());
        assertEquals(expectedInformalStatus, result.getInformalNewStatus());

        // campi legal non devono essere valorizzati nel ramo INFORMAL
        assertNull(result.getElement());
        assertNull(result.getNewStatus());
        assertNull(result.getTimelineEventCategory());
        // legalfactIds non viene toccato in questo ramo: deve restare quello (eventualmente null) di partenza
        assertNull(result.getLegalfactIds());
    }

    // ---------------------------------------------------------------------
    // Mapping dei validationErrors (baseMapping)
    // ---------------------------------------------------------------------


    @Test
    @DisplayName("lista di errori presente: viene mappata elemento per elemento")
    void shouldMapValidationErrorsWhenPresent() {
        RefusedReasonEntity error1 = new RefusedReasonEntity();
        error1.setErrorCode("ERR-001");
        error1.setDetail("primo errore di validazione");

        RefusedReasonEntity error2 = new RefusedReasonEntity();
        error2.setErrorCode("ERR-002");
        error2.setDetail("secondo errore di validazione");

        EventEntity ev = buildBaseEventEntity();
        ev.setValidationErrors(List.of(error1, error2));

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertNotNull(result.getValidationErrors());
        assertEquals(2, result.getValidationErrors().size());
        assertEquals(EntityToDtoRefusedReasonMapper.entityToDto(error1), result.getValidationErrors().get(0));
        assertEquals(EntityToDtoRefusedReasonMapper.entityToDto(error2), result.getValidationErrors().get(1));
    }

    @Test
    @DisplayName("lista di errori vuota: validationErrors risulta null nel risultato")
    void shouldSetNullValidationErrorsWhenListIsEmpty() {
        EventEntity ev = buildBaseEventEntity();
        ev.setValidationErrors(List.of());

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertNull(result.getValidationErrors());
    }

    @Test
    @DisplayName("lista di errori null: validationErrors risulta null nel risultato")
    void shouldSetNullValidationErrorsWhenListIsNull() {
        EventEntity ev = buildBaseEventEntity();
        ev.setValidationErrors(null);

        TimelineElementInternal timelineElementInternal = buildTimelineElementInternal(CommunicationType.LEGAL);

        ProgressResponseElementV30 result = ProgressResponseElementMapper.internalToExternal(
                buildDto(ev, timelineElementInternal));

        assertNull(result.getValidationErrors());
    }
}
