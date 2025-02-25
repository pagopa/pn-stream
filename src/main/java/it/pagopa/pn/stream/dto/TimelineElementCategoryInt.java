package it.pagopa.pn.stream.dto;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TimelineElementCategoryInt {
    SENDER_ACK_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_10),
    VALIDATE_F24_REQUEST(TimelineElementCategoryInt.VERSION_20),
    VALIDATE_NORMALIZE_ADDRESSES_REQUEST(TimelineElementCategoryInt.VERSION_10),
    VALIDATED_F24(TimelineElementCategoryInt.VERSION_20),
    NORMALIZED_ADDRESS(TimelineElementCategoryInt.VERSION_10),
    REQUEST_ACCEPTED(TimelineElementCategoryInt.VERSION_10),
    GENERATE_F24_REQUEST(TimelineElementCategoryInt.VERSION_23),
    GENERATED_F24(TimelineElementCategoryInt.VERSION_23),
    SEND_COURTESY_MESSAGE(TimelineElementCategoryInt.VERSION_10),
    GET_ADDRESS(TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_CALL(TimelineElementCategoryInt.VERSION_10),
    PUBLIC_REGISTRY_RESPONSE(TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_ANALOG_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_DIGITAL_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    PREPARE_DIGITAL_DOMICILE(TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_DOMICILE(TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_FEEDBACK(TimelineElementCategoryInt.VERSION_10),
    SEND_DIGITAL_PROGRESS(TimelineElementCategoryInt.VERSION_10),
    REFINEMENT(TimelineElementCategoryInt.VERSION_10),
    SCHEDULE_REFINEMENT(TimelineElementCategoryInt.VERSION_10),
    DIGITAL_DELIVERY_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_10),
    DIGITAL_SUCCESS_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    DIGITAL_FAILURE_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    ANALOG_SUCCESS_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    ANALOG_FAILURE_WORKFLOW(TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_10),
    PREPARE_SIMPLE_REGISTERED_LETTER(TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER(TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_10),
    NOTIFICATION_VIEWED(TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE(TimelineElementCategoryInt.VERSION_10),
    PREPARE_ANALOG_DOMICILE_FAILURE(TimelineElementCategoryInt.VERSION_20),
    SEND_ANALOG_DOMICILE(TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_PROGRESS(TimelineElementCategoryInt.VERSION_10),
    SEND_ANALOG_FEEDBACK(TimelineElementCategoryInt.VERSION_10),
    PAYMENT(TimelineElementCategoryInt.VERSION_10),
    COMPLETELY_UNREACHABLE( TimelineElementCategoryInt.VERSION_10),
    REQUEST_REFUSED(TimelineElementCategoryInt.VERSION_10),
    AAR_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_10),
    AAR_GENERATION(TimelineElementCategoryInt.VERSION_10),
    NOT_HANDLED(TimelineElementCategoryInt.VERSION_10),
    SEND_SIMPLE_REGISTERED_LETTER_PROGRESS(TimelineElementCategoryInt.VERSION_10),
    PROBABLE_SCHEDULING_ANALOG_DATE(TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLATION_REQUEST(TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_CANCELLED(TimelineElementCategoryInt.VERSION_20),
    NOTIFICATION_RADD_RETRIEVED(TimelineElementCategoryInt.VERSION_23),
    NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST(TimelineElementCategoryInt.VERSION_25),
    ANALOG_WORKFLOW_RECIPIENT_DECEASED(TimelineElementCategoryInt.VERSION_26);


    private final int version;

    TimelineElementCategoryInt(int version) {
        this.version = version;
    }

    public static final int VERSION_10 = 10;
    public static final int VERSION_20 = 20;
    public static final int VERSION_23 = 23;
    public static final int VERSION_25 = 25;
    public static final int VERSION_26 = 26;
    public static final int VERSION_27 = 27;

    public enum DiagnosticTimelineElementCategory {
        VALIDATED_F24,
        VALIDATE_F24_REQUEST,
        GENERATED_F24,
        GENERATE_F24_REQUEST,
        NOTIFICATION_CANCELLED_DOCUMENT_CREATION_REQUEST;
    }

    public enum UnlockTimelineElementCategory {
        REQUEST_ACCEPTED,
        REQUEST_REFUSED;
    }

    public enum SkipSortCategory {
        SENDER_ACK_CREATION_REQUEST,
        VALIDATE_NORMALIZE_ADDRESSES_REQUEST,
        NORMALIZED_ADDRESS,
        VALIDATED_F24,
        VALIDATE_F24_REQUEST
    }

    @Getter
    public enum StreamVersions {
        STREAM_V27(VERSION_27, VERSION_26,VERSION_10),
        STREAM_V26(VERSION_26, VERSION_26,VERSION_10),
        STREAM_V25(VERSION_25, VERSION_25,VERSION_10),
        STREAM_V23(VERSION_23, VERSION_23,VERSION_10),
        STREAM_V20(VERSION_20, VERSION_20,VERSION_10),
        STREAM_V10(VERSION_10, VERSION_10,VERSION_10);

        private final int streamVersion;
        private final int timelineVersion;
        private final int statusVersion;

        StreamVersions(int streamVersion, int timelineVersion, int statusVersion) {
            this.streamVersion = streamVersion;
            this.timelineVersion = timelineVersion;
            this.statusVersion = statusVersion;
        }

        public static StreamVersions fromIntValue(int version) {
            return Arrays.stream(StreamVersions.values())
                    .filter(streamVersions -> streamVersions.streamVersion == version)
                    .findFirst()
                    .orElse(null);
        }
    }

}

