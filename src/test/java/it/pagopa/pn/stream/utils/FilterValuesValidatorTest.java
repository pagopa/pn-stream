package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.stream.service.impl.StreamEventsServiceImpl.DEFAULT_CATEGORIES;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;

class FilterValuesValidatorTest {

    private final FilterValuesValidator validator = new FilterValuesValidator();

    @Test
    void emptyFilteredValues_returnsEmpty() {
        StepVerifier.create(validator.validateFilterValues(
                        "1", Collections.emptyList(), null, EventType.TIMELINE))
                .verifyComplete();

        StepVerifier.create(validator.validateFilterValues(
                        "1", Collections.emptyList(), null, EventType.STATUS))
                .verifyComplete();
    }

    @Test
    void timeline_allowedValue_returnsEmpty() {
        try (MockedStatic<TimelineElementCategoryInt> mocked =
                     mockStatic(TimelineElementCategoryInt.class, Mockito.CALLS_REAL_METHODS)) {

            TimelineElementCategoryInt sample = TimelineElementCategoryInt.values()[0];

            mocked.when(() -> TimelineElementCategoryInt
                            .getSupportedCategoriesByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(List.of(sample));

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of(sample.name()), null, EventType.TIMELINE))
                    .verifyComplete();
        }
    }

    @Test
    void timeline_notAllowedValue_returnsError() {
        try (MockedStatic<TimelineElementCategoryInt> mocked =
                     mockStatic(TimelineElementCategoryInt.class, Mockito.CALLS_REAL_METHODS)) {

            TimelineElementCategoryInt sample = TimelineElementCategoryInt.values()[0];

            mocked.when(() -> TimelineElementCategoryInt
                            .getSupportedCategoriesByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(List.of(sample));

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of("VALORE_NON_SUPPORTATO"), null, EventType.TIMELINE))
                    .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                    .verify();
        }
    }

    @Test
    void timeline_notAnEnumValue_returnsError() {
        try (MockedStatic<TimelineElementCategoryInt> mocked =
                     mockStatic(TimelineElementCategoryInt.class, Mockito.CALLS_REAL_METHODS)) {

            mocked.when(() -> TimelineElementCategoryInt
                            .getSupportedCategoriesByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of("ERROR"), null, EventType.TIMELINE))
                    .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                    .verify();
        }
    }

    @Test
    void timeline_defaultCategoriesValue_isAlwaysAllowed() {
        try (MockedStatic<TimelineElementCategoryInt> mocked =
                     mockStatic(TimelineElementCategoryInt.class, Mockito.CALLS_REAL_METHODS)) {

            mocked.when(() -> TimelineElementCategoryInt
                            .getSupportedCategoriesByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(Collections.emptyList());

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of(DEFAULT_CATEGORIES), null, EventType.TIMELINE))
                    .verifyComplete();
        }
    }

    @Test
    void status_allowedValue_returnsEmpty() {
        try (MockedStatic<NotificationStatusInt> mocked =
                     mockStatic(NotificationStatusInt.class, Mockito.CALLS_REAL_METHODS)) {

            NotificationStatusInt sample = NotificationStatusInt.values()[0];

            mocked.when(() -> NotificationStatusInt
                            .getSupportedStatusByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(List.of(sample));

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of(sample.name()), null, EventType.STATUS))
                    .verifyComplete();
        }
    }

    @Test
    void status_notAllowedValue_returnsError() {
        try (MockedStatic<NotificationStatusInt> mocked =
                     mockStatic(NotificationStatusInt.class, Mockito.CALLS_REAL_METHODS)) {

            NotificationStatusInt sample = NotificationStatusInt.values()[0];

            mocked.when(() -> NotificationStatusInt
                            .getSupportedStatusByCommunicationTypeAndVersion(any(), anyInt()))
                    .thenReturn(List.of(sample));

            StepVerifier.create(validator.validateFilterValues(
                            "1", List.of("ERROR"), null, EventType.STATUS))
                    .expectErrorSatisfies(FilterValuesValidatorTest::assertBadRequestStreamConfig)
                    .verify();
        }
    }

    @Test
    void status_nullCommunicationType_doesNotThrow() {
        try (MockedStatic<NotificationStatusInt> mocked =
                     mockStatic(NotificationStatusInt.class, Mockito.CALLS_REAL_METHODS)) {

            NotificationStatusInt sample = NotificationStatusInt.values()[0];

            mocked.when(() -> NotificationStatusInt
                            .getSupportedStatusByCommunicationTypeAndVersion(eq(null), anyInt()))
                    .thenReturn(List.of(sample));

            StepVerifier.create(validator.validateFilterValues(
                            "2", List.of(sample.name()), null, EventType.STATUS))
                    .verifyComplete();
        }
    }

    private static void assertBadRequestStreamConfig(Throwable throwable) {
        org.assertj.core.api.Assertions.assertThat(throwable)
                .isInstanceOf(PnStreamException.class);
        PnStreamException ex = (PnStreamException) throwable;
        org.assertj.core.api.Assertions.assertThat(ex.getStatus()).isEqualTo(400);
    }

}