package it.pagopa.pn.stream.dto;

import lombok.Getter;

@Getter
public enum ConfidentialInformationEnum {

    TAX_ID("taxId", "taxId",  null, "delegateInfo"),
    DENOMINATION("denomination", "denomination", null, "delegateInfo"),
    DIGITAL_ADDRESS("address","digitalAddress", null , "digitalAddress"),
    PHYSICAL_ADDRESS("physicalAddress", "physicalAddress", "physicalAddress",null),
    NORMALIZED_ADDRESS("newAddress", "newPhysicalAddress", "normalizedAddress",null),
    NEW_ADDRESS("newAddress", "newPhysicalAddress", "newAddress" ,null),
    OLD_ADDRESS("physicalAddress", "physicalAddress", "oldAddress" ,null),
    FOUND_ADDRESS("physicalAddress", "physicalAddress", "foundAddress" ,null);

    private final String timelineKey;
    private final String confidentialValue;
    private final String eventValue;
    private final String parent;


    ConfidentialInformationEnum(String timelineKey, String confidentialValue,  String eventValue, String parent) {
        this.timelineKey = timelineKey;
        this.confidentialValue = confidentialValue;
        this.eventValue = eventValue;
        this.parent = parent;
    }

    public enum CustomCategory {
        NORMALIZED_ADDRESS,
        SEND_ANALOG_FEEDBACK,
        PREPARE_ANALOG_DOMICILE_FAILURE

    }

}


