package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType;
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

}