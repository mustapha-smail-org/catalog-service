package com.citypulse.catalog.service;

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

    public record CursorPosition(
            Instant startDate,
            String eventId
    ) {
    }
}