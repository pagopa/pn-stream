package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.stream.service.impl.StreamEventsServiceImpl.DEFAULT_CATEGORIES;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterValuesValidator {
    private final StreamUtils streamUtils;

    public Mono<Void> validateFilterValues(String version, List<String> filteredValues, it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType communicationType, EventType eventType) {
        if (filteredValues.isEmpty()) {
            return Mono.empty();
        }

        CommunicationType communicationTypeint = null;
        if (communicationType != null) {
            communicationTypeint = CommunicationType.valueOf(communicationType.getValue());
        }

        int parsedVersion = streamUtils.getVersion(version);

        return switch (eventType) {
            case TIMELINE -> validateTimelineFilters(parsedVersion, filteredValues, communicationTypeint);
            case STATUS -> validateStatusFilters(parsedVersion, filteredValues, communicationTypeint);
        };

    }

    private Mono<Void> validateTimelineFilters(int version, List<String> filteredValues, CommunicationType communicationType) {
        List<TimelineElementCategoryInt> allowedCategories =
                TimelineElementCategoryInt.getSupportedCategoriesByCommunicationTypeAndVersion(communicationType, version);

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

    private Mono<Void> validateStatusFilters(int version, List<String> filteredValues, CommunicationType communicationType) {
        List<NotificationStatusInt> allowedStatuses =
                NotificationStatusInt.getSupportedStatusByCommunicationTypeAndVersion(communicationType, version);

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
