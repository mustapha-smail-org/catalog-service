package com.citypulse.catalog.service;

import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.utils.DateRange;

import java.time.Instant;

public record EventSearchCriteria(
        DateRange dateRange,
        String category,
        PricingFilter pricing,
        String arrondissement,
        String query,
        CursorPosition cursor
) {

    public record CursorPosition(
            Instant startDate,
            String eventId
    ) {
    }
}