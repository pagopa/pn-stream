package it.pagopa.pn.stream.utils;

import it.pagopa.pn.stream.dto.CommunicationType;

public class CommunicationTypeUtils {
    private CommunicationTypeUtils() {}

    // Gli elementi di timeline legali non hanno un communicationType impostato, quindi se il communicationType è null, viene considerato LEGAL come default
    public static CommunicationType getDefaultCommunicationType(CommunicationType communicationType) {
        return (communicationType == null) ? CommunicationType.LEGAL : communicationType;
    }

    // Metodo per convertire il communicationType generato da OpenAPI in quello interno, con default LEGAL se null
    public static CommunicationType getDefaultCommunicationType(it.pagopa.pn.stream.generated.openapi.server.v1.dto.CommunicationType reqCommunicationType) {
        if(reqCommunicationType == null) {
            return CommunicationType.LEGAL;
        }

        return CommunicationType.valueOf(reqCommunicationType.name());
    }
}
