package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV30;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.stream.service.impl.StreamEventsServiceImpl.DEFAULT_CATEGORIES;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterValuesValidatorTest {
    @Mock
    private StreamUtils streamUtils;
    @InjectMocks
    private FilterValuesValidator validator;

    @Test
    void emptyFilteredValues_returnsEmpty() {
        StepVerifier.create(validator.validateFilterValues(
                        "10", Collections.emptyList(), null, EventType.TIMELINE))
                .verifyComplete();

        StepVerifier.create(validator.validateFilterValues(
                        "10", Collections.emptyList(), null, EventType.STATUS))
                .verifyComplete();
    }

    @Test
    void timeline_allowedValue_returnsEmpty() {
        String version = "10";
        TimelineElementCategoryInt sample = TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST;
        when(streamUtils.getVersion(version)).thenReturn(10);

        StepVerifier.create(validator.validateFilterValues(
                        version, List.of(sample.name()), null, EventType.TIMELINE))
                .verifyComplete();
    }

    @Test
    void timeline_notAllowedValue_returnsError() {
        String version = "10";
        TimelineElementCategoryInt sample = TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST;
        when(streamUtils.getVersion(version)).thenReturn(10);

        StepVerifier.create(validator.validateFilterValues(
                        version, List.of(sample.name()), CommunicationType.INFORMAL, EventType.TIMELINE))
                .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                .verify();
    }

    @Test
    void timeline_notAnEnumValue_returnsError() {
        String version = "10";
        when(streamUtils.getVersion(version)).thenReturn(10);

        StepVerifier.create(validator.validateFilterValues(
                        version, List.of("ERROR"), null, EventType.TIMELINE))
                .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                .verify();
    }

    @Test
    void timeline_defaultCategoriesValue_isAlwaysAllowed() {
        String version = "10";
        when(streamUtils.getVersion(version)).thenReturn(10);
        StepVerifier.create(validator.validateFilterValues(
                        version, List.of(DEFAULT_CATEGORIES), null, EventType.TIMELINE))
                .verifyComplete();
    }

    @Test
    void status_allowedValue_returnsEmpty() {
        String version = "10";
        NotificationStatusInt sample = NotificationStatusInt.IN_VALIDATION;
        when(streamUtils.getVersion(version)).thenReturn(10);
        StepVerifier.create(validator.validateFilterValues(
                        version, List.of(sample.name()), null, EventType.STATUS))
                .verifyComplete();
    }

    @Test
    void status_notAllowedValue_returnsError() {
        String version = "10";
        NotificationStatusInt sample = NotificationStatusInt.DELIVERING;
        when(streamUtils.getVersion(version)).thenReturn(10);
        StepVerifier.create(validator.validateFilterValues(
                        version, List.of(sample.name()), CommunicationType.INFORMAL, EventType.STATUS))
                .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                .verify();
    }

    @Test
    void status_notAnEnumValue_returnsError() {
        String version = "10";
        when(streamUtils.getVersion(version)).thenReturn(10);
        StepVerifier.create(validator.validateFilterValues(
                        version, List.of("ERROR"), null, EventType.STATUS))
                .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                .verify();
    }

    private static void assertBadRequestStreamConfig(Throwable throwable) {
        org.assertj.core.api.Assertions.assertThat(throwable)
                .isInstanceOf(PnStreamException.class);
        PnStreamException ex = (PnStreamException) throwable;
        org.assertj.core.api.Assertions.assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    void sortedStream_nullFilterValues_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.TIMELINE, null);

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedStream_emptyFilterValues_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.STATUS, Collections.emptyList());

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedTimeline_withDefaultCategories_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.TIMELINE, List.of(DEFAULT_CATEGORIES));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedTimeline_withRequestAccepted_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.TIMELINE,
                List.of(TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST.name(), "REQUEST_ACCEPTED"));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedTimeline_withoutDefaultOrRequestAccepted_returnsError() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.TIMELINE,
                List.of(TimelineElementCategoryInt.SENDER_ACK_CREATION_REQUEST.name()));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectError(PnStreamForbiddenException.class)
                .verify();
    }

    @Test
    void sortedTimeline_withAcceptedOnly_returnsError() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.TIMELINE,
                List.of(NotificationStatusInt.ACCEPTED.name()));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectError(PnStreamForbiddenException.class)
                .verify();
    }

    @Test
    void sortedStatus_withAccepted_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.STATUS,
                List.of(NotificationStatusInt.ACCEPTED.name()));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedStatus_withAcceptedAmongOthers_isAllowed() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.STATUS,
                List.of(NotificationStatusInt.DELIVERING.name(), NotificationStatusInt.ACCEPTED.name()));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectNext(request)
                .verifyComplete();
    }

    @Test
    void sortedStatus_withoutAccepted_returnsError() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.STATUS,
                List.of(NotificationStatusInt.DELIVERING.name(), NotificationStatusInt.VIEWED.name()));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectError(PnStreamForbiddenException.class)
                .verify();
    }

    @Test
    void sortedStatus_withRequestAcceptedOnly_returnsError() {
        StreamCreationRequestV30 request = buildRequest(
                StreamCreationRequestV30.EventTypeEnum.STATUS, List.of("REQUEST_ACCEPTED"));

        StepVerifier.create(validator.validateFilterValuesIfWaitForAccepted(request))
                .expectError(PnStreamForbiddenException.class)
                .verify();
    }

    private StreamCreationRequestV30 buildRequest(StreamCreationRequestV30.EventTypeEnum eventType,
                                                  List<String> filterValues) {
        StreamCreationRequestV30 request = new StreamCreationRequestV30();
        request.setEventType(eventType);
        request.setFilterValues(filterValues);
        return request;
    }
}