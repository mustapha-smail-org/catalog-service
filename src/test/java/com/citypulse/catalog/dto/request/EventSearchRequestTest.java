package com.citypulse.catalog.dto.request;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventSearchRequestTest {

    private static EventSearchRequest with(
            List<String> categories,
            String category,
            List<String> arrondissements,
            String arrondissement
    ) {
        return new EventSearchRequest(
                null, null, categories, category, null,
                arrondissements, arrondissement, null, null, null, null
        );
    }

    @Test
    void shouldReturnEmptyCategoriesWhenNothingProvided() {
        assertThat(with(null, null, null, null).effectiveCategories()).isEmpty();
    }

    @Test
    void shouldKeepMultipleCategories() {
        assertThat(with(List.of("Concerts", "Expositions"), null, null, null).effectiveCategories())
                .containsExactly("Concerts", "Expositions");
    }

    @Test
    void shouldFoldDeprecatedSingularCategoryIntoList() {
        assertThat(with(null, "Concerts", null, null).effectiveCategories())
                .containsExactly("Concerts");
    }

    @Test
    void shouldMergeListAndSingularCategoryAndTrimAndDropBlanks() {
        assertThat(with(List.of("Concerts", "  ", " Expos "), "  Cinema  ", null, null).effectiveCategories())
                .containsExactly("Concerts", "Expos", "Cinema");
    }

    @Test
    void shouldReturnEmptyArrondissementsWhenNothingProvided() {
        assertThat(with(null, null, null, null).effectiveArrondissements()).isEmpty();
    }

    @Test
    void shouldMergeListAndSingularArrondissement() {
        assertThat(with(null, null, List.of("1", "20"), "OUTSIDE_PARIS").effectiveArrondissements())
                .containsExactly("1", "20", "OUTSIDE_PARIS");
    }

    @Test
    void shouldDropBlankArrondissementValues() {
        assertThat(with(null, null, List.of("2", "  "), null).effectiveArrondissements())
                .containsExactly("2");
    }

    @Test
    void shouldDefaultPricingSortAndLimit() {
        EventSearchRequest request = with(null, null, null, null);

        assertThat(request.effectivePricing()).isEqualTo(PricingFilter.ALL);
        assertThat(request.effectiveSort()).isEqualTo(EventSort.START_DATE);
        assertThat(request.effectiveLimit()).isEqualTo(30);
    }
}
