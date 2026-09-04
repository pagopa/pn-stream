package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.exceptions.PnStreamException;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static it.pagopa.pn.stream.dto.CommunicationTypeVersion.informal;
import static it.pagopa.pn.stream.dto.CommunicationTypeVersion.legal;
import static it.pagopa.pn.stream.utils.CommunicationTypeUtils.getDefaultCommunicationType;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_10)),
    VALIDATE_F24_REQUEST(legal(TimelineElementCategoryInt.VERSION_20)),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    VALIDATED_F24(legal(TimelineElementCategoryInt.VERSION_20)),
    NORMALIZED_ADDRESS(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    REQUEST_ACCEPTED(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    GENERATE_F24_REQUEST(legal(TimelineElementCategoryInt.VERSION_23)),
    GENERATED_F24(legal(TimelineElementCategoryInt.VERSION_23)),
    SEND_COURTESY_MESSAGE(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    GET_ADDRESS(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    PUBLIC_REGISTRY_CALL(legal(TimelineElementCategoryInt.VERSION_10),informal(TimelineElementCategoryInt.VERSION_10)),
    PUBLIC_REGISTRY_RESPONSE(legal(TimelineElementCategoryInt.VERSION_10)),
    SCHEDULE_ANALOG_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    SCHEDULE_DIGITAL_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    PREPARE_DIGITAL_DOMICILE(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_DOMICILE(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_FEEDBACK(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_PROGRESS(legal(TimelineElementCategoryInt.VERSION_10)),
    REFINEMENT(legal(TimelineElementCategoryInt.VERSION_10)),
    SCHEDULE_REFINEMENT(legal(TimelineElementCategoryInt.VERSION_10)),
    DIGITAL_DELIVERY_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_10)),
    DIGITAL_SUCCESS_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    DIGITAL_FAILURE_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    ANALOG_SUCCESS_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    ANALOG_FAILURE_WORKFLOW(legal(TimelineElementCategoryInt.VERSION_10)),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_10)),
    PREPARE_SIMPLE_REGISTERED_LETTER(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_SIMPLE_REGISTERED_LETTER(legal(TimelineElementCategoryInt.VERSION_10)),
    NOTIFICATION_VIEWED_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_10)),
    NOTIFICATION_VIEWED(legal(TimelineElementCategoryInt.VERSION_10)),
    PREPARE_ANALOG_DOMICILE(legal(TimelineElementCategoryInt.VERSION_10)),
    PREPARE_ANALOG_DOMICILE_FAILURE(legal(TimelineElementCategoryInt.VERSION_20)),
    SEND_ANALOG_DOMICILE(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_PROGRESS(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_FEEDBACK(legal(TimelineElementCategoryInt.VERSION_10)),
    PAYMENT(legal(TimelineElementCategoryInt.VERSION_10)),
    COMPLETELY_UNREACHABLE(legal(TimelineElementCategoryInt.VERSION_10)),
    REQUEST_REFUSED(legal(TimelineElementCategoryInt.VERSION_10)),
    AAR_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_10)),
    AAR_GENERATION(legal(TimelineElementCategoryInt.VERSION_10)),
    NOT_HANDLED(legal(TimelineElementCategoryInt.VERSION_10)),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(legal(TimelineElementCategoryInt.VERSION_10)),
    PROBABLE_SCHEDULING_ANALOG_DATE(legal(TimelineElementCategoryInt.VERSION_20)),
    NOTIFICATION_CANCELLATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_20)),
    NOTIFICATION_CANCELLED(legal(TimelineElementCategoryInt.VERSION_20)),
    NOTIFICATION_RADD_RETRIEVED(legal(TimelineElementCategoryInt.VERSION_23)),
    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_25)),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(legal(TimelineElementCategoryInt.VERSION_26)),
    PUBLIC_REGISTRY_VALIDATION_CALL(legal(TimelineElementCategoryInt.VERSION_27),informal(TimelineElementCategoryInt.VERSION_10)),
    PUBLIC_REGISTRY_VALIDATION_RESPONSE(legal(TimelineElementCategoryInt.VERSION_27),informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_TIMEOUT_CREATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_27)),
    SEND_ANALOG_TIMEOUT(legal(TimelineElementCategoryInt.VERSION_27)),
    ANALOG_FAILURE_WORKFLOW_TIMEOUT(legal(TimelineElementCategoryInt.VERSION_27)),
    NOTIFICATION_TIMELINE_REWORKED(legal(TimelineElementCategoryInt.VERSION_28)),
    NOTIFICATION_COST_VALIDATION_REQUEST(legal(TimelineElementCategoryInt.VERSION_28)),
    NOTIFICATION_COST_VALIDATION_RESPONSE(legal(TimelineElementCategoryInt.VERSION_28)),
    COURTESY_CHANNEL_FAILED(legal(TimelineElementCategoryInt.VERSION_28)),
    //Timeline Element for Informal Notification
    SEND_DIGITAL_MESSAGE(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_MESSAGE_SKIP(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_MESSAGE_PROGRESS(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_DIGITAL_MESSAGE_FEEDBACK(informal(TimelineElementCategoryInt.VERSION_10)),
    PREPARE_ANALOG_DELIVERY(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_MESSAGE(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_MESSAGE_PROGRESS(informal(TimelineElementCategoryInt.VERSION_10)),
    SEND_ANALOG_MESSAGE_FEEDBACK(informal(TimelineElementCategoryInt.VERSION_10)),
    DELIVERED(informal(TimelineElementCategoryInt.VERSION_10)),
    WORKFLOW_ENDED_REACHED(informal(TimelineElementCategoryInt.VERSION_10)),
    WORKFLOW_ENDED_UNREACHED(informal(TimelineElementCategoryInt.VERSION_10)),
    WORKFLOW_ENDED_UNDELIVERABLE(informal(TimelineElementCategoryInt.VERSION_10)),
    WORKFLOW_DONE_REACHED(informal(TimelineElementCategoryInt.VERSION_10)),
    WORKFLOW_DONE_UNREACHED(informal(TimelineElementCategoryInt.VERSION_10)),
    INFORMAL_NOTIFICATION_VIEWED(informal(TimelineElementCategoryInt.VERSION_10)),
    COVERPAGE_CREATION_REQUEST(informal(TimelineElementCategoryInt.VERSION_10));


    private final Map<CommunicationType, Integer> versionsByCommunicationType;

    TimelineElementCategoryInt(Map<CommunicationType, Integer> versionsByCommunicationType) {
        this.versionsByCommunicationType = versionsByCommunicationType;
    }

    public boolean isSupportedBy(CommunicationType communicationType) {
        return versionsByCommunicationType.containsKey(communicationType);
    }

    public static TimelineElementCategoryInt[] getSupportedCategoriesBy(CommunicationType communicationType) {
        CommunicationType defaultCommunicationType = getDefaultCommunicationType(communicationType);
        return Arrays.stream(TimelineElementCategoryInt.values())
                .filter(category -> category.isSupportedBy(communicationType))
                .toArray(TimelineElementCategoryInt[]::new);
    }

    public int getVersionNonNull(CommunicationType communicationType) {
        return Optional.ofNullable(versionsByCommunicationType.get(communicationType))
                .orElseThrow(() -> new PnStreamException("TimelineElementCategory " + this.name() + " is not supported for communication type " + communicationType, 500, "ERROR_CODE_STREAM_CONFIGURATION"));
    }

    TimelineElementCategoryInt(CommunicationTypeVersion... versions) {
        this.versionsByCommunicationType = Arrays.stream(versions)
                .collect(Collectors.toMap(CommunicationTypeVersion::type, CommunicationTypeVersion::version));
    }

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;
    public static final int VERSION_23 = 23;
    public static final int VERSION_24 = 24;
    public static final int VERSION_25 = 25;
    public static final int VERSION_26 = 26;
    public static final int VERSION_27 = 27;
    public static final int VERSION_28 = 28;
    public static final int VERSION_29 = 29;
    public static final int VERSION_30 = 30;

    public enum DiagnosticTimelineElementCategory {
        VALIDATED_F24,
        VALIDATE_F24_REQUEST,
        GENERATED_F24,
        GENERATE_F24_REQUEST,
        NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST,
        SEND_ANALOG_TIMEOUT_CREATION_REQUEST,
        SEND_ANALOG_TIMEOUT,
        ANALOG_FAILURE_WORKFLOW_TIMEOUT,
        NOTIFICATION_COST_VALIDATION_REQUEST,
        NOTIFICATION_COST_VALIDATION_RESPONSE,
        COURTESY_CHANNEL_FAILED
    }

    public enum UnlockTimelineElementCategory {
        REQUEST_ACCEPTED,
        REQUEST_REFUSED
    }

    public enum SkipSortCategory {
        SENDER_ACK_CREATION_REQUEST,
        VALIDATE_NORMALIZE_ADDRESSES_REQUEST,
        NORMALIZED_ADDRESS,
        VALIDATED_F24,
        VALIDATE_F24_REQUEST,
        PUBLIC_REGISTRY_VALIDATION_CALL,
        PUBLIC_REGISTRY_VALIDATION_RESPONSE

    }

    @Getter
    public enum StreamVersions {
        STREAM_V30(VERSION_30),
        STREAM_V29(VERSION_29),
        STREAM_V28(VERSION_28),
        STREAM_V27(VERSION_27),
        STREAM_V26(VERSION_26),
        STREAM_V25(VERSION_25),
        STREAM_V24(VERSION_24),
        STREAM_V23(VERSION_23),
        STREAM_V20(VERSION_20),
        STREAM_V10(VERSION_10);

        private final int streamVersion;

        StreamVersions(int streamVersion) {
            this.streamVersion = streamVersion;
        }

        public static StreamVersions fromIntValue(int version) {
            return Arrays.stream(StreamVersions.values())
                    .filter(streamVersions -> streamVersions.streamVersion == version)
                    .findFirst()
                    .orElse(STREAM_V10);
        }
    }

}

