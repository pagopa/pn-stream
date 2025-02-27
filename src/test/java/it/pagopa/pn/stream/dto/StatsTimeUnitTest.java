package it.pagopa.pn.stream.dto;

import it.pagopa.pn.stream.dto.stats.StatsTimeUnit;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatsTimeUnitTest {

    @Test
    void statsTimeUnitShouldContainDays() {
        assertTrue(EnumSet.allOf(StatsTimeUnit.class).contains(StatsTimeUnit.DAYS));
    }

    @Test
    void statsTimeUnitShouldContainHours() {
        assertTrue(EnumSet.allOf(StatsTimeUnit.class).contains(StatsTimeUnit.HOURS));
    }

    @Test
    void statsTimeUnitShouldContainMinutes() {
        assertTrue(EnumSet.allOf(StatsTimeUnit.class).contains(StatsTimeUnit.MINUTES));
    }

    @Test
    void statsTimeUnitShouldNotContainInvalidValue() {
        assertFalse(EnumSet.allOf(StatsTimeUnit.class).contains(null));
    }
}
