package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.stream.service.impl.StreamEventsServiceImpl.DEFAULT_CATEGORIES;

@Slf4j
public class FilterValuesValidator {

    public Mono<Void> validateFilterValues(String version, List<String> filteredValues, it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType communicationType, EventType eventType) {
        if (filteredValues.isEmpty()) {
            return Mono.empty();
        }
        CommunicationType communicationTypeint = null;
        if (communicationType != null) {
            communicationTypeint = CommunicationType.valueOf(communicationType.getValue());
        }

        if (EventType.TIMELINE.equals(eventType)) {
            return validateTimelineFilters(version, filteredValues, communicationTypeint);
        }

        if (EventType.STATUS.equals(eventType)) {
            return validateStatusFilters(version, filteredValues, communicationTypeint);
        }

        return Mono.empty();
    }

    private Mono<Void> validateTimelineFilters(String version, List<String> filteredValues, CommunicationType communicationType) {
        int parsedVersion = Integer.parseInt(version);
        List<TimelineElementCategoryInt> allowedCategories =
                TimelineElementCategoryInt.getSupportedCategoriesByCommunicationTypeAndVersion(communicationType, parsedVersion);

        List<String> forbiddenValues = filteredValues.stream()
                .filter(value -> !isDefault(value) && !isTimelineCategoryAllowed(value, allowedCategories))
                .toList();

        if (!forbiddenValues.isEmpty()) {
            return Mono.error(new PnStreamException(
                    "Invalid filteredValue for TIMELINE stream: " + forbiddenValues,
                    400,
                    "ERROR_CODE_STREAM_CONFIGURATION"
            ));
        }
        return Mono.empty();
    }

    private Mono<Void> validateStatusFilters(String version, List<String> filteredValues, CommunicationType communicationType) {
        try {
            int parsedVersion = Integer.parseInt(version);
            List<NotificationStatusInt> allowedStatuses =
                    NotificationStatusInt.getSupportedStatusByCommunicationTypeAndVersion(communicationType, parsedVersion);

            List<String> forbiddenValues = filteredValues.stream()
                    .filter(value -> !isNotificationStatusAllowed(value, allowedStatuses))
                    .toList();

            if (!forbiddenValues.isEmpty()) {
                return Mono.error(new PnStreamException(
                        "Invalid filteredValue for STATUS stream: " + forbiddenValues,
                        400,
                        "ERROR_CODE_STREAM_CONFIGURATION"
                ));
            }
        } catch (IllegalArgumentException e) {
            log.error("Invalid filteredValue for STATUS stream: {}", filteredValues, e);
        }
        return Mono.empty();
    }

    private boolean isTimelineCategoryAllowed(String filteredValue, List<TimelineElementCategoryInt> allowedCategories) {
        try {
            return allowedCategories.contains(TimelineElementCategoryInt.valueOf(filteredValue));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isNotificationStatusAllowed(String filteredValue, List<NotificationStatusInt> allowedStatuses) {
        try {
            return allowedStatuses.contains(NotificationStatusInt.valueOf(filteredValue));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isDefault(String filteredValue) {
        return filteredValue.equals(DEFAULT_CATEGORIES);
    }
}
