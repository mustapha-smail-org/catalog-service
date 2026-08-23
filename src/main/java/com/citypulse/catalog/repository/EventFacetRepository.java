package com.citypulse.catalog.repository;

import com.citypulse.catalog.dto.response.FacetCountResponse;
import com.citypulse.catalog.entity.EventEntity;
import com.citypulse.catalog.service.EventSearchCriteria;
import com.citypulse.catalog.specification.EventSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Aggregation queries backing the discovery facet counts. Built directly on the
 * {@link EntityManager} because grouped {@code COUNT} projections are outside the
 * reach of {@link org.springframework.data.jpa.repository.JpaSpecificationExecutor},
 * while reusing {@link EventSpecification} so the counts honour the same filters as
 * the event search.
 */
@Repository
public class EventFacetRepository {

    private static final String OUTSIDE_PARIS = "OUTSIDE_PARIS";
    private static final String UNKNOWN = "UNKNOWN";

    private static final Set<String> PARIS_ZIPCODES =
            IntStream.rangeClosed(1, 20)
                    .mapToObj("750%02d"::formatted)
                    .collect(Collectors.toUnmodifiableSet());

    private final EntityManager entityManager;

    public EventFacetRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Counts distinct events per category. The supplied criteria must already
     * exclude the category dimension so the counts stay usable as drill-down
     * options.
     */
    public List<FacetCountResponse> countByCategory(EventSearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<EventEntity> root = query.from(EventEntity.class);

        Predicate where = predicate(criteria, root, query, builder);

        Join<EventEntity, String> category = root.join("categories", JoinType.INNER);
        Expression<Long> count = builder.countDistinct(root.get("id"));

        query.multiselect(category, count).distinct(false);
        if (where != null) {
            query.where(where);
        }
        query.groupBy(category);
        query.orderBy(builder.desc(count), builder.asc(category));

        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new FacetCountResponse(row.get(0, String.class), row.get(1, Long.class)))
                .toList();
    }

    /**
     * Counts distinct events per arrondissement bucket (1-20, OUTSIDE_PARIS,
     * UNKNOWN). The supplied criteria must already exclude the arrondissement
     * dimension. Ordered 1 → 20 → outside → unknown.
     */
    public List<FacetCountResponse> countByArrondissement(EventSearchCriteria criteria) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = builder.createTupleQuery();
        Root<EventEntity> root = query.from(EventEntity.class);

        Predicate where = predicate(criteria, root, query, builder);

        Expression<String> zipcode = root.get("location").get("zipcode");
        Expression<Long> count = builder.countDistinct(root.get("id"));

        query.multiselect(zipcode, count).distinct(false);
        if (where != null) {
            query.where(where);
        }
        query.groupBy(zipcode);

        Map<String, Long> byBucket = new LinkedHashMap<>();
        for (Tuple row : entityManager.createQuery(query).getResultList()) {
            String bucket = bucketOf(row.get(0, String.class));
            byBucket.merge(bucket, row.get(1, Long.class), Long::sum);
        }

        return orderBuckets(byBucket);
    }

    private Predicate predicate(
            EventSearchCriteria criteria,
            Root<EventEntity> root,
            CriteriaQuery<?> query,
            CriteriaBuilder builder
    ) {
        Specification<EventEntity> specification = EventSpecification.matching(criteria);
        return specification.toPredicate(root, query, builder);
    }

    private static String bucketOf(String zipcode) {
        if (zipcode == null || zipcode.isBlank()) {
            return UNKNOWN;
        }

        String trimmed = zipcode.trim();

        if (PARIS_ZIPCODES.contains(trimmed)) {
            return String.valueOf(Integer.parseInt(trimmed.substring(3)));
        }

        return OUTSIDE_PARIS;
    }

    private static List<FacetCountResponse> orderBuckets(Map<String, Long> byBucket) {
        List<FacetCountResponse> ordered = new ArrayList<>();

        for (int arrondissement = 1; arrondissement <= 20; arrondissement++) {
            Long count = byBucket.get(String.valueOf(arrondissement));
            if (count != null) {
                ordered.add(new FacetCountResponse(String.valueOf(arrondissement), count));
            }
        }

        addIfPresent(ordered, byBucket, OUTSIDE_PARIS);
        addIfPresent(ordered, byBucket, UNKNOWN);

        return ordered;
    }

    private static void addIfPresent(
            List<FacetCountResponse> ordered,
            Map<String, Long> byBucket,
            String bucket
    ) {
        Long count = byBucket.get(bucket);
        if (count != null) {
            ordered.add(new FacetCountResponse(bucket, count));
        }
    }
}
