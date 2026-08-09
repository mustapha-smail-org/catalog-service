package com.citypulse.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EventSearchRequest(
        PeriodFilter period,

        @Size(max = 100)
        String category,

        PricingFilter pricing,

        @Pattern(
                regexp = "^(?:[1-9]|1[0-9]|20|OUTSIDE_PARIS|UNKNOWN)$",
                message = """
                        arrondissement must be 1-20, \
                        OUTSIDE_PARIS or UNKNOWN
                        """
        )
        String arrondissement,

        @Size(max = 200)
        String query,

        EventSort sort,

        @Min(1)
        @Max(100)
        Integer limit,

        String cursor
) {

    public PricingFilter effectivePricing() {
        return pricing == null ? PricingFilter.ALL : pricing;
    }

    public EventSort effectiveSort() {
        return sort == null ? EventSort.START_DATE : sort;
    }

    public int effectiveLimit() {
        return limit == null ? 30 : limit;
    }
}