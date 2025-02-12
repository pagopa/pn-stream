package it.pagopa.pn.stream.dto.stats;

import lombok.Getter;

@Getter
public enum WebhookStatsEnum {

    NUMBER_OF_REQUESTS,
    RETRY_AFTER_VIOLATION,
    NUMBER_OF_READINGS,
    NUMBER_OF_WRITINGS,
    NUMBER_OF_EMPTY_READINGS
}