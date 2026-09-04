package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.exceptions.PnStreamException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.stream.dto.CommunicationType.INFORMAL;
import static it.pagopa.pn.stream.dto.CommunicationType.LEGAL;
import static it.pagopa.pn.stream.dto.TimelineElementCategoryInt.*;
import static it.pagopa.pn.stream.dto.TimelineElementCategoryInt.StreamVersions.*;
import static it.pagopa.pn.stream.exceptions.PnStreamExceptionCodes.ERROR_CODE_STREAM_CONFIGURATION;
import static it.pagopa.pn.stream.utils.CommunicationTypeUtils.getDefaultCommunicationType;

@Component
public class StreamVersionsTable {
    private final Map<StreamVersionsKey, StreamVersionsValue> data = new HashMap<>();

    StreamVersionsTable() {
        data.put(new StreamVersionsKey(STREAM_V30, INFORMAL), new StreamVersionsValue(VERSION_10, VERSION_10));
        data.put(new StreamVersionsKey(STREAM_V30, LEGAL), new StreamVersionsValue(VERSION_28, VERSION_26));
        data.put(new StreamVersionsKey(STREAM_V29, LEGAL), new StreamVersionsValue(VERSION_28, VERSION_26));
        data.put(new StreamVersionsKey(STREAM_V28, LEGAL), new StreamVersionsValue(VERSION_27, VERSION_26));
        data.put(new StreamVersionsKey(STREAM_V27, LEGAL), new StreamVersionsValue(VERSION_26, VERSION_26));
        data.put(new StreamVersionsKey(STREAM_V26, LEGAL), new StreamVersionsValue(VERSION_26, VERSION_26));
        data.put(new StreamVersionsKey(STREAM_V25, LEGAL), new StreamVersionsValue(VERSION_25, VERSION_10));
        data.put(new StreamVersionsKey(STREAM_V24, LEGAL), new StreamVersionsValue(VERSION_24, VERSION_10));
        data.put(new StreamVersionsKey(STREAM_V23, LEGAL), new StreamVersionsValue(VERSION_23, VERSION_10));
        data.put(new StreamVersionsKey(STREAM_V20, LEGAL), new StreamVersionsValue(VERSION_20, VERSION_10));
        data.put(new StreamVersionsKey(STREAM_V10, LEGAL), new StreamVersionsValue(VERSION_10, VERSION_10));
    }

    public int getTimelineVersion(TimelineElementCategoryInt.StreamVersions sv, CommunicationType type) {
        return getByKeyNonNull(StreamVersionsKey.of(sv, getDefaultCommunicationType(type))).timelineVersion;
    }

    public int getStatusVersion(TimelineElementCategoryInt.StreamVersions sv, CommunicationType type) {
        return getByKeyNonNull(StreamVersionsKey.of(sv, getDefaultCommunicationType(type))).statusVersion;
    }

    private StreamVersionsValue getByKeyNonNull(StreamVersionsKey key) {
        StreamVersionsValue value = data.get(key);
        if (value == null) {
            throw new PnStreamException("Row not found for stream version: " + key.streamVersion() + " and communication type: " + key.communicationType(), 500, ERROR_CODE_STREAM_CONFIGURATION);
        }
        return value;
    }

    public record StreamVersionsKey(TimelineElementCategoryInt.StreamVersions streamVersion, CommunicationType communicationType) {
        public static StreamVersionsKey of(TimelineElementCategoryInt.StreamVersions streamVersion, CommunicationType communicationType) {
            return new StreamVersionsKey(streamVersion, communicationType);
        }
    }
    public record StreamVersionsValue(int timelineVersion, int statusVersion) {
        public static StreamVersionsValue of(int timelineVersion, int statusVersion) {
            return new StreamVersionsValue(timelineVersion, statusVersion);
        }
    }
}
