package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.CommunicationType;

public class CommunicationTypeUtils {
    private CommunicationTypeUtils() {}

    // Gli elementi di timeline legali non hanno un communicationType impostato, quindi se il communicationType è null, viene considerato LEGAL come default
    public static CommunicationType getDefaultCommunicationType(CommunicationType communicationType) {
        return (communicationType == null) ? CommunicationType.LEGAL : communicationType;
    }
}
