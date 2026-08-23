package com.citypulse.catalog.utils;

import com.citypulse.catalog.dto.request.PeriodFilter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeResolverTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-13T10:30:00Z"), PARIS
    );

    private final DateRangeResolver resolver = new DateRangeResolver(CLOCK);

    @Test
    void shouldReturnNoRangeWhenPeriodIsAbsent() {
        assertThat(resolver.resolve(null)).isNull();
    }

    @Test
    void shouldResolveTodayFromNowUntilMidnight() {
        assertThat(resolver.resolve(PeriodFilter.TODAY)).isEqualTo(new DateRange(
                Instant.parse("2026-08-13T10:30:00Z"),
                Instant.parse("2026-08-13T22:00:00Z")
        ));
    }

    @Test
    void shouldResolveTomorrowUsingParisMidnights() {
        assertThat(resolver.resolve(PeriodFilter.TOMORROW)).isEqualTo(new DateRange(
                Instant.parse("2026-08-13T22:00:00Z"),
                Instant.parse("2026-08-14T22:00:00Z")
        ));
    }

    @Test
    void shouldResolveCurrentWeekUntilNextMonday() {
        assertThat(resolver.resolve(PeriodFilter.THIS_WEEK)).isEqualTo(new DateRange(
                Instant.parse("2026-08-13T10:30:00Z"),
                Instant.parse("2026-08-16T22:00:00Z")
        ));
    }

    @Test
    void shouldResolveCurrentMonthUntilFirstDayOfNextMonth() {
        assertThat(resolver.resolve(PeriodFilter.THIS_MONTH)).isEqualTo(new DateRange(
                Instant.parse("2026-08-13T10:30:00Z"),
                Instant.parse("2026-08-31T22:00:00Z")
        ));
    }

    @Test
    void shouldReturnNoRangeWhenDateIsAbsent() {
        assertThat(resolver.resolveDate(null)).isNull();
    }

    @Test
    void shouldResolveSpecificDateToFullParisDay() {
        assertThat(resolver.resolveDate(LocalDate.of(2026, 8, 20))).isEqualTo(new DateRange(
                Instant.parse("2026-08-19T22:00:00Z"),
                Instant.parse("2026-08-20T22:00:00Z")
        ));
    }

    @Test
    void shouldResolveWinterDateAcrossParisOffset() {
        assertThat(resolver.resolveDate(LocalDate.of(2026, 1, 15))).isEqualTo(new DateRange(
                Instant.parse("2026-01-14T23:00:00Z"),
                Instant.parse("2026-01-15T23:00:00Z")
        ));
    }
}
