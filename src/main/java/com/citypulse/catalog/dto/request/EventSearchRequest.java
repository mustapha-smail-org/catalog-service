package com.citypulse.catalog.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record EventSearchRequest(
        PeriodFilter period,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        List<@Size(max = 100) String> categories,

        /**
         * Deprecated single-value alias, folded into {@link #categories}.
         * Kept so clients built against the previous contract keep working.
         */
        @Size(max = 100)
        String category,

        PricingFilter pricing,

        List<@Pattern(
                regexp = "^(?:[1-9]|1[0-9]|20|OUTSIDE_PARIS|UNKNOWN)$",
                message = """
                        arrondissement must be 1-20, \
                        OUTSIDE_PARIS or UNKNOWN
                        """
        ) String> arrondissements,

        /**
         * Deprecated single-value alias, folded into {@link #arrondissements}.
         */
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

    public List<String> effectiveCategories() {
        return merge(categories, category);
    }

    public List<String> effectiveArrondissements() {
        return merge(arrondissements, arrondissement);
    }

    public PricingFilter effectivePricing() {
        return pricing == null ? PricingFilter.ALL : pricing;
    }

    public EventSort effectiveSort() {
        return sort == null ? EventSort.START_DATE : sort;
    }

    public int effectiveLimit() {
        return limit == null ? 30 : limit;
    }

    private static List<String> merge(List<String> values, String single) {
        List<String> result = new ArrayList<>();

        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(result::add);
        }

        if (single != null && !single.isBlank()) {
            result.add(single.trim());
        }

        return result;
    }
}
