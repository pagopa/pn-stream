package it.pagopa.pn.stream.dto;

public record CommunicationTypeVersion(CommunicationType type, int version) {
    public static CommunicationTypeVersion legal(int v) {
        return new CommunicationTypeVersion(CommunicationType.LEGAL, v);
    }
    public static CommunicationTypeVersion informal(int v) {
        return new CommunicationTypeVersion(CommunicationType.INFORMAL, v);
    }
}

