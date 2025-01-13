package it.pagopa.pn.stream.dto;

import java.util.Map;

import static java.util.Map.entry;

public enum ConfidentialInformationEnum {

    TAX_ID("taxId", "taxId", null),
    DENOMINATION("denomination", "denomination", null),
    NORMALIZED_ADDRESS("normalizedAddress", "newPhysicalAddress", null),
    NEW_ADDRESS("newAddress", "newPhysicalAddress", null),
    PHYSICAL_ADDRESS("physicalAddress", "physicalAddress", null),
    OLD_ADDRESS("oldAddress", "physicalAddress", null),
    FOUND_ADDRESS("foundAddress", "physicalAddress", null),
    ADDRESS("address","digitalAddress", "digitalAddress");

    private final String timelineKey;
    private final String confidentialValue;
    private final String parent;


    ConfidentialInformationEnum(String timelineKey, String confidentialValue, String parent) {
        this.timelineKey = timelineKey;
        this.confidentialValue = confidentialValue;
        this.parent = parent;
    }

    public String getTimelineKey() {
        return timelineKey;
    }

    public String getConfidentialValue() {
        return confidentialValue;
    }

    public String getParent() {
        return parent;
    }
}


