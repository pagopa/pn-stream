package it.pagopa.pn.stream.exceptions;

import it.pagopa.pn.commons.exceptions.PnExceptionsCodes;
import lombok.Getter;

public class PnStreamExceptionCodes extends PnExceptionsCodes {

    // raccolgo qui tutti i codici di errore di pn-stream
    public static final String ERROR_CODE_STREAM_NOTFOUND = "PN_STREAM_NOTFOUND";
    public static final String ERROR_CODE_STREAM_GETFILEERROR = "PN_STREAM_GETFILEERROR";
    public static final String ERROR_CODE_STREAM_UPDATEMETAFILEERROR = "PN_STREAM_UPDATEMETAFILEERROR";
    public static final String ERROR_CODE_WEBHOOK_SAVEEVENT = "PN_WEBHOOK_SAVEEVENT";
    public static final String ERROR_CODE_WEBHOOK_MAXSTREAMSCOUNTREACHED = "PN_WEBHOOK_MAXSTREAMSCOUNTREACHED";
    public static final String ERROR_CODE_WEBHOOK_FORBIDDEN = "PN_WEBHOOK_FORBIDDEN";
    public static final String ERROR_CODE_WEBHOOK_NOT_FOUND = "PN_WEBHOOK_NOT_FOUND";
    public static final String ERROR_CODE_STREAM_NOTIFICATIONSTATUSFAILED = "PN_STREAM_NOTIFICATION_STATUS_FAILED";
    public static final String ERROR_CODE_STREAM_ROOTIDNOTFOUND = "PN_STREAM_ROOTIDNOTFOUND";
    public static final String ERROR_CODE_STREAM_AUDITLOGFAILED = "PN_STREAM_AUDITLOGFAILED";
    public static final String ERROR_CODE_STREAM_HANDLEEVENTFAILED = "PN_STREAM_HANDLEEVENTFAILED";
    public static final String ERROR_CODE_STREAM_EVENTTYPENOTSUPPORTED = "PN_STREAM_EVENTTYPENOTSUPPORTED";
    public static final String ERROR_CODE_STREAM_ACTIONTYPENOTSUPPORTED = "PN_STREAM_ACTIONTYPENOTSUPPORTED";
    public static final String ERROR_CODE_STREAM_ACTIONEXCEPTION = "PN_STREAM_ACTIONEXCEPTION";
    public static final String ERROR_CODE_WEBHOOK_EVENTFAILED = "PN_WEBHOOK_EVENTFAILED";
    public static final String ERROR_CODE_STREAM_DUPLICATED_ITEM = "PN_STREAM_DUPLICATED_ITEM";
    public static final String ERROR_CODE_STREAM_TIMELINE_ELEMENT_NOT_PRESENT = "ERROR_CODE_STREAM_TIMELINEELEMENTNOTPRESENT";
    public static final String ERROR_CODE_DATAVAULT_FAILED = "ERROR_CODE_DATAVAULT_FAILED";
    public static final String ERROR_CODE_EXTERNAL_REGISTRY_GROUP_FAILED = "ERROR_CODE_EXTERNAL_REGISTRY_GROUP_FAILED";
    public static final String ERROR_CODE_STREAM_STREAMNOTFOUND = "PN_STREAM_STREAMNOTFOUND";
    public static final String ERROR_CODE_STREAM_NOTIFICATIONFAILED = "PN_STREAM_NOTIFICATIONFAILED";

    @Getter
    public enum NotificationRefusedErrorCodeInt {
        FILE_NOTFOUND("FILE_NOTFOUND"),

        FILE_SHA_ERROR("FILE_SHA_ERROR"),

        FILE_PDF_INVALID_ERROR("FILE_PDF_INVALID_ERROR"),

        FILE_PDF_TOOBIG_ERROR("FILE_PDF_TOOBIG_ERROR"),

        TAXID_NOT_VALID("TAXID_NOT_VALID"),

        NOT_VALID_ADDRESS("NOT_VALID_ADDRESS"),

        F24_METADATA_NOT_VALID("F24_METADATA_NOT_VALID"),

        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE"),

        RECIPIENT_ID_NOT_VALID("RECIPIENT_ID_NOT_VALID"),

        PAYMENT_NOT_VALID("PAYMENT_NOT_VALID"),

        SENDER_DISABLED_MORE_THAN_20_GRAMS("SENDER_DISABLED_MORE_THAN_20_GRAMS"),

        FILE_GONE("FILE_GONE");

        private final String value;

        NotificationRefusedErrorCodeInt(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}