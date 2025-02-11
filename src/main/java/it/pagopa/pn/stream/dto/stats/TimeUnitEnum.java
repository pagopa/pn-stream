package it.pagopa.pn.stream.dto.stats;

import lombok.Getter;

@Getter
public enum TimeUnitEnum {
    DAYS("days"),
    HOURS("hours"),
    MINUTES("minutes");

    private final String value;

    TimeUnitEnum(String value) {
        this.value = value;
    }
}
