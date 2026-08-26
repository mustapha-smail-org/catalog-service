package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.EventSort;
import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.utils.DateRange;

import java.time.Instant;
import java.util.List;

public record EventSearchCriteria(
        DateRange dateRange,
        List<String> categories,
        PricingFilter pricing,
        List<String> arrondissements,
        String query,
        CursorPosition cursor
) {

    /**
     * Keyset position. Carries the {@link EventSort} it was issued for so a
     * cursor cannot be replayed under a different sort. {@code startDate} is set
     * for START_DATE; {@code rankScore} (nullable — unenriched tail) for
     * RELEVANCE.
     */
    public record CursorPosition(
            EventSort sort,
            Instant startDate,
            Double rankScore,
            String eventId
    ) {
    }
}