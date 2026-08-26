package com.citypulse.catalog.specification;

import com.citypulse.catalog.dto.request.PricingFilter;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.entity.EventOccurrenceEntity;
import com.citypulse.catalog.service.EventSearchCriteria;
import com.citypulse.catalog.utils.DateRange;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

public final class EventSpecification {

    // Conditionally-free events (e.g. free with reservation) count as free for
    // the "Gratuit" filter and rail, but keep a distinct display category.
    private static final List<String> FREE_PRICE_TYPES =
            List.of("free", "gratuit", "gratuite", "gratuit sous condition");

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

        if (isNotEmpty(criteria.categories())) {
            specification = specification.and(
                    hasAnyCategory(criteria.categories())
            );
        }

        if (criteria.pricing() != null
                && criteria.pricing() != PricingFilter.ALL) {
            specification = specification.and(
                    hasPricing(criteria.pricing())
            );
        }

        if (isNotEmpty(criteria.arrondissements())) {
            specification = specification.and(
                    hasAnyArrondissement(criteria.arrondissements())
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

    /**
     * A period matches an event when one of its individual occurrences overlaps
     * the range (so a weekly event only shows on the days it actually runs).
     * Events with no stored occurrences fall back to the event-level
     * start/end envelope, since single-shot events carry their schedule there.
     */
    private static Specification<EventEntity> overlaps(DateRange range) {
        return (root, query, builder) -> {
            Subquery<Long> overlappingOccurrence =
                    query.subquery(Long.class);
            Root<EventOccurrenceEntity> occurrence =
                    overlappingOccurrence.from(EventOccurrenceEntity.class);
            Expression<Instant> occurrenceEnd =
                    builder.<Instant>coalesce()
                            .value(occurrence.get("end"))
                            .value(occurrence.get("start"));
            overlappingOccurrence.select(builder.literal(1L)).where(
                    builder.equal(
                            occurrence.get("event").get("id"),
                            root.get("id")
                    ),
                    builder.lessThan(
                            occurrence.get("start"),
                            range.endExclusive()
                    ),
                    builder.greaterThanOrEqualTo(
                            occurrenceEnd,
                            range.startInclusive()
                    )
            );

            Subquery<Long> anyOccurrence = query.subquery(Long.class);
            Root<EventOccurrenceEntity> existing =
                    anyOccurrence.from(EventOccurrenceEntity.class);
            anyOccurrence.select(builder.literal(1L)).where(
                    builder.equal(
                            existing.get("event").get("id"),
                            root.get("id")
                    )
            );

            Expression<Instant> envelopeEnd =
                    builder.<Instant>coalesce()
                            .value(root.get("endDate"))
                            .value(root.get("startDate"));
            Predicate envelopeOverlap = builder.and(
                    builder.lessThan(
                            root.get("startDate"),
                            range.endExclusive()
                    ),
                    builder.greaterThanOrEqualTo(
                            envelopeEnd,
                            range.startInclusive()
                    )
            );

            return builder.or(
                    builder.exists(overlappingOccurrence),
                    builder.and(
                            builder.not(builder.exists(anyOccurrence)),
                            envelopeOverlap
                    )
            );
        };
    }

    private static Specification<EventEntity> hasAnyCategory(
            List<String> categories
    ) {
        List<String> lowered = categories.stream()
                .map(category -> category.trim().toLowerCase(Locale.ROOT))
                .toList();

        return (root, query, builder) -> {
            query.distinct(true);

            return builder.lower(
                    root.join("categories", JoinType.INNER)
            ).in(lowered);
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

    private static Specification<EventEntity> hasAnyArrondissement(
            List<String> values
    ) {
        return (root, query, builder) -> {
            Expression<String> zipcode =
                    root.get("location").get("zipcode");

            Predicate[] predicates = values.stream()
                    .map(value -> arrondissementPredicate(builder, zipcode, value))
                    .toArray(Predicate[]::new);

            return builder.or(predicates);
        };
    }

    private static Predicate arrondissementPredicate(
            CriteriaBuilder builder,
            Expression<String> zipcode,
            String value
    ) {
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
        return switch (cursor.sort()) {
            case START_DATE -> afterStartDateCursor(cursor);
            case RELEVANCE -> afterRelevanceCursor(cursor);
        };
    }

    private static Specification<EventEntity> afterStartDateCursor(
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

    /**
     * Keyset for the RELEVANCE order {@code rank_score DESC NULLS LAST, id ASC}.
     * A row is "after" the cursor when it has a lower rank, sits in the NULL
     * tail, or shares the cursor's rank with a greater id. When the cursor is
     * already in the NULL tail, only later NULL-rank rows remain.
     */
    private static Specification<EventEntity> afterRelevanceCursor(
            EventSearchCriteria.CursorPosition cursor
    ) {
        return (root, query, builder) -> {
            Expression<Double> rank = root.get("rankScore");

            if (cursor.rankScore() == null) {
                return builder.and(
                        builder.isNull(rank),
                        builder.greaterThan(root.get("id"), cursor.eventId())
                );
            }

            return builder.or(
                    builder.lessThan(rank, cursor.rankScore()),
                    builder.isNull(rank),
                    builder.and(
                            builder.equal(rank, cursor.rankScore()),
                            builder.greaterThan(root.get("id"), cursor.eventId())
                    )
            );
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isNotEmpty(List<String> values) {
        return values != null && !values.isEmpty();
    }
}