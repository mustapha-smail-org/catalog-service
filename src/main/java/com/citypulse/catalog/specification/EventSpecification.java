package com.citypulse.catalog.specification;

import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.service.EventSearchCriteria;
import com.citypulse.catalog.utils.DateRange;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class EventSpecification {

    private static final List<String> FREE_PRICE_TYPES =
            List.of("free", "gratuit", "gratuite");

    private static final List<String> PARIS_ZIPCODES =
            IntStream.rangeClosed(1, 20)
                    .mapToObj(value -> "750%02d".formatted(value))
                    .toList();

    private EventSpecification() {
    }

    public static Specification<EventEntity> matching(
            EventSearchCriteria criteria
    ) {
        Specification<EventEntity> specification =
                (root, query, builder) -> builder.conjunction();

        if (criteria.dateRange() != null) {
            specification = specification.and(
                    overlaps(criteria.dateRange())
            );
        }

        if (hasText(criteria.category())) {
            specification = specification.and(
                    hasCategory(criteria.category())
            );
        }

        if (criteria.pricing() != null
                && criteria.pricing() != PricingFilter.ALL) {
            specification = specification.and(
                    hasPricing(criteria.pricing())
            );
        }

        if (hasText(criteria.arrondissement())) {
            specification = specification.and(
                    hasArrondissement(criteria.arrondissement())
            );
        }

        if (hasText(criteria.query())) {
            specification = specification.and(
                    containsText(criteria.query())
            );
        }

        if (criteria.cursor() != null) {
            specification = specification.and(
                    afterCursor(criteria.cursor())
            );
        }

        return specification;
    }

    public static Specification<EventEntity> hasCoordinates() {
        return (root, query, builder) -> builder.and(
                builder.isNotNull(
                        root.get("location").get("latitude")
                ),
                builder.isNotNull(
                        root.get("location").get("longitude")
                )
        );
    }

    private static Specification<EventEntity> overlaps(DateRange range) {
        return (root, query, builder) -> {
            Expression<Instant> effectiveEnd =
                    builder.<Instant>coalesce()
                            .value(root.get("endDate"))
                            .value(root.get("startDate"));

            return builder.and(
                    builder.lessThan(
                            root.get("startDate"),
                            range.endExclusive()
                    ),
                    builder.greaterThanOrEqualTo(
                            effectiveEnd,
                            range.startInclusive()
                    )
            );
        };
    }

    private static Specification<EventEntity> hasCategory(
            String category
    ) {
        return (root, query, builder) -> {
            query.distinct(true);

            return builder.equal(
                    builder.lower(
                            root.join("categories", JoinType.INNER)
                    ),
                    category.trim().toLowerCase(Locale.ROOT)
            );
        };
    }

    private static Specification<EventEntity> hasPricing(
            PricingFilter pricing
    ) {
        return (root, query, builder) -> {
            Expression<String> priceType = builder.lower(
                    root.get("pricing").get("priceType")
            );

            Predicate missing = builder.or(
                    builder.isNull(
                            root.get("pricing").get("priceType")
                    ),
                    builder.equal(builder.trim(priceType), "")
            );

            Predicate free = priceType.in(FREE_PRICE_TYPES);

            return switch (pricing) {
                case FREE -> free;
                case PAID -> builder.and(
                        builder.not(missing),
                        builder.not(free)
                );
                case NOT_SPECIFIED -> missing;
                case ALL -> builder.conjunction();
            };
        };
    }

    private static Specification<EventEntity> hasArrondissement(
            String value
    ) {
        return (root, query, builder) -> {
            Expression<String> zipcode =
                    root.get("location").get("zipcode");

            if ("UNKNOWN".equals(value)) {
                return builder.or(
                        builder.isNull(zipcode),
                        builder.equal(builder.trim(zipcode), "")
                );
            }

            if ("OUTSIDE_PARIS".equals(value)) {
                return builder.and(
                        builder.isNotNull(zipcode),
                        builder.notEqual(builder.trim(zipcode), ""),
                        builder.not(zipcode.in(PARIS_ZIPCODES))
                );
            }

            String expectedZipcode = "750%02d".formatted(
                    Integer.parseInt(value)
            );

            return builder.equal(zipcode, expectedZipcode);
        };
    }

    private static Specification<EventEntity> containsText(String value) {
        return (root, query, builder) -> {
            query.distinct(true);

            String pattern = "%"
                    + value.trim().toLowerCase(Locale.ROOT)
                    + "%";

            Expression<String> category = builder.lower(
                    root.join("categories", JoinType.LEFT)
            );

            return builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(
                            builder.lower(
                                    builder.coalesce(
                                            root.get("description"),
                                            ""
                                    )
                            ),
                            pattern
                    ),
                    builder.like(
                            builder.lower(
                                    builder.coalesce(
                                            root.get("location").get("name"),
                                            ""
                                    )
                            ),
                            pattern
                    ),
                    builder.like(
                            builder.lower(
                                    builder.coalesce(
                                            root.get("location").get("street"),
                                            ""
                                    )
                            ),
                            pattern
                    ),
                    builder.like(
                            builder.lower(
                                    builder.coalesce(
                                            root.get("location").get("city"),
                                            ""
                                    )
                            ),
                            pattern
                    ),
                    builder.like(category, pattern)
            );
        };
    }

    private static Specification<EventEntity> afterCursor(
            EventSearchCriteria.CursorPosition cursor
    ) {
        return (root, query, builder) -> builder.or(
                builder.greaterThan(
                        root.get("startDate"),
                        cursor.startDate()
                ),
                builder.and(
                        builder.equal(
                                root.get("startDate"),
                                cursor.startDate()
                        ),
                        builder.greaterThan(
                                root.get("id"),
                                cursor.eventId()
                        )
                )
        );
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}