package it.pagopa.pn.stream.middleware.dao.timelinedao.dynamo.entity;

public enum DeliveryModeEntity {
    DIGITAL("DIGITAL"),

    ANALOG("ANALOG");

    private final String value;

    DeliveryModeEntity(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
}
