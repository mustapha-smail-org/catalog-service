package com.citypulse.catalog.utils;

import com.citypulse.catalog.dto.request.PeriodFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

@Component
@RequiredArgsConstructor
public class DateRangeResolver {

    private final Clock clock;

    public DateRange resolve(PeriodFilter period) {
        if (period == null) {
            return null;
        }

        ZonedDateTime now = ZonedDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        return switch (period) {
            case TODAY -> new DateRange(
                    now.toInstant(),
                    today.plusDays(1)
                            .atStartOfDay(clock.getZone())
                            .toInstant()
            );

            case TOMORROW -> {
                LocalDate tomorrow = today.plusDays(1);

                yield new DateRange(
                        tomorrow.atStartOfDay(clock.getZone()).toInstant(),
                        tomorrow.plusDays(1)
                                .atStartOfDay(clock.getZone())
                                .toInstant()
                );
            }

            case THIS_WEEK -> {
                LocalDate nextMonday = today.with(
                        TemporalAdjusters.next(DayOfWeek.MONDAY)
                );

                yield new DateRange(
                        now.toInstant(),
                        nextMonday.atStartOfDay(clock.getZone()).toInstant()
                );
            }

            case THIS_MONTH -> {
                LocalDate firstDayNextMonth = today
                        .withDayOfMonth(1)
                        .plusMonths(1);

                yield new DateRange(
                        now.toInstant(),
                        firstDayNextMonth
                                .atStartOfDay(clock.getZone())
                                .toInstant()
                );
            }
        };
    }
}