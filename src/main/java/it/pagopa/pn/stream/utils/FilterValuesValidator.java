package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.EventType;
import it.pagopa.pn.stream.dto.TimelineElementCategoryInt;
import it.pagopa.pn.stream.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.stream.exceptions.PnStreamException;
import it.pagopa.pn.stream.exceptions.PnStreamForbiddenException;
import it.pagopa.pn.stream.generated.openapi.server.v1.dto.StreamCreationRequestV30;
import it.pagopa.pn.stream.service.utils.StreamUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.stream.service.impl.StreamEventsServiceImpl.DEFAULT_CATEGORIES;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterValuesValidator {
    private final StreamUtils streamUtils;

    public Mono<Void> validateFilterValues(String version, List<String> filteredValues, CommunicationType communicationType, EventType eventType) {
        if (CollectionUtils.isEmpty(filteredValues)) {
            return Mono.empty();
        }

        int parsedVersion = streamUtils.getVersion(version);

        return switch (eventType) {
            case TIMELINE -> validateTimelineFilters(parsedVersion, filteredValues, communicationType);
            case STATUS -> validateStatusFilters(parsedVersion, filteredValues, communicationType);
        };

    }

    private Mono<Void> validateTimelineFilters(int version, List<String> filteredValues, CommunicationType communicationType) {
        List<TimelineElementCategoryInt> allowedCategories =
                TimelineElementCategoryInt.getSupportedCategoriesByCommunicationTypeAndVersion(communicationType, version);

        List<String> forbiddenValues = filteredValues.stream()
                .filter(value -> !isDefault(value) && !isTimelineCategoryAllowed(value, allowedCategories))
                .toList();

        if (!forbiddenValues.isEmpty()) {
            String message = "Invalid filteredValue for TIMELINE stream: " + forbiddenValues;
            return Mono.error(new PnStreamException(
                    message,
                    400,
                    "ERROR_CODE_STREAM_CONFIGURATION",
                    message
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
            String message = "Invalid filteredValue for STATUS stream: " + forbiddenValues;
            return Mono.error(new PnStreamException(
                    message,
                    400,
                    "ERROR_CODE_STREAM_CONFIGURATION",
                    message
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

    public Mono<StreamCreationRequestV30> validateFilterValuesIfWaitForAccepted(StreamCreationRequestV30 streamCreationRequest) {
        List<String> filterValues = streamCreationRequest.getFilterValues();
        if (filterValues != null && !filterValues.isEmpty()) {
            EventType eventType = EventType.valueOf(streamCreationRequest.getEventType().name());
            if (eventType == EventType.TIMELINE
                    && filterValues.stream().noneMatch(f -> f.equals(DEFAULT_CATEGORIES) || f.equals("REQUEST_ACCEPTED"))) {
                return Mono.error(new PnStreamForbiddenException(
                        "Not Allowed the creation of sorted TIMELINE streams without DEFAULT or REQUEST_ACCEPTED filter"));
            }
            if (eventType == EventType.STATUS
                    && filterValues.stream().noneMatch(f -> f.equals("ACCEPTED"))) {
                return Mono.error(new PnStreamForbiddenException(
                        "Not Allowed the creation of sorted STATUS streams without ACCEPTED filter"));
            }
        }
        return Mono.just(streamCreationRequest);
    }
}
