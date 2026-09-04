package it.pagopa.pn.stream.dto.ext.delivery.notification.status;

import it.pagopa.pn.stream.dto.CommunicationType;
import it.pagopa.pn.stream.dto.CommunicationTypeVersion;
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
public enum NotificationStatusInt {
    IN_VALIDATION("IN_VALIDATION", legal(NotificationStatusInt.VERSION_10), informal(NotificationStatusInt.VERSION_10)),

    ACCEPTED("ACCEPTED", legal(NotificationStatusInt.VERSION_10), informal(NotificationStatusInt.VERSION_10)),

    DELIVERING("DELIVERING", legal(NotificationStatusInt.VERSION_10)),

    DELIVERED("DELIVERED", legal(NotificationStatusInt.VERSION_10)),

    VIEWED("VIEWED", legal(NotificationStatusInt.VERSION_10)),

    EFFECTIVE_DATE("EFFECTIVE_DATE", legal(NotificationStatusInt.VERSION_10)),

    PAID("PAID", legal(NotificationStatusInt.VERSION_10)),

    UNREACHABLE("UNREACHABLE", legal(NotificationStatusInt.VERSION_10)),

    REFUSED("REFUSED", legal(NotificationStatusInt.VERSION_10), informal(NotificationStatusInt.VERSION_10)),

    CANCELLED("CANCELLED", legal(NotificationStatusInt.VERSION_10)),

    RETURNED_TO_SENDER("RETURNED_TO_SENDER", legal(NotificationStatusInt.VERSION_26)),

    PROCESSING("PROCESSING", informal(NotificationStatusInt.VERSION_10)),

    COMPLETED_REACHED("COMPLETED_REACHED", informal(NotificationStatusInt.VERSION_10)),

    COMPLETED_UNREACHED("COMPLETED_UNREACHED", informal(NotificationStatusInt.VERSION_10)),

    UNDELIVERABLE("UNDELIVERABLE", informal(NotificationStatusInt.VERSION_10));

    private final String value;
    private final Map<CommunicationType, Integer> versionsByCommunicationType;

    public static final int VERSION_10 = 10;
    public static final int VERSION_26 = 26;

    NotificationStatusInt(String value, CommunicationTypeVersion... versions) {
        this.value = value;
        this.versionsByCommunicationType = Arrays.stream(versions)
                .collect(Collectors.toMap(CommunicationTypeVersion::type, CommunicationTypeVersion::version));
    }

    public boolean isSupportedBy(CommunicationType communicationType) {
        return versionsByCommunicationType.containsKey(communicationType);
    }

    public int getVersionNonNull(CommunicationType communicationType) {
        return Optional.ofNullable(versionsByCommunicationType.get(communicationType))
                .orElseThrow(() -> new PnStreamException("TimelineElementCategory " + this.name() + " is not supported for communication type " + communicationType, 500, "ERROR_CODE_STREAM_CONFIGURATION"));
    }

    public static NotificationStatusInt[] getSupportedCategoriesBy(CommunicationType communicationType) {
        CommunicationType defaultCommunicationType = getDefaultCommunicationType(communicationType);
        return Arrays.stream(NotificationStatusInt.values())
                .filter(category -> category.isSupportedBy(defaultCommunicationType))
                .toArray(NotificationStatusInt[]::new);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
