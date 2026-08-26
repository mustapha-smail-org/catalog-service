package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.EventEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends
        JpaRepository<EventEntity, String>,
        JpaSpecificationExecutor<EventEntity> {

    Optional<EventEntity> findBySourceEventId(Long sourceEventId);

    Optional<EventEntity> findBySlug(String slug);

    boolean existsBySourceEventId(Long sourceEventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM EventEntity event
            WHERE event.id = :id
            """)
    Optional<EventEntity> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event
            FROM EventEntity event
            WHERE event.sourceEventId = :sourceEventId
            """)
    Optional<EventEntity> findBySourceEventIdForUpdate(
            @Param("sourceEventId") Long sourceEventId
    );

    @Query("""
            SELECT DISTINCT category
            FROM EventEntity event
            JOIN event.categories category
            ORDER BY category
            """)
    List<String> findDistinctCategories();

    /**
     * Events whose enrichment is missing, older than the current prompt version,
     * or computed from a stale source revision. Freshest events first; native so
     * the null-safe {@code IS DISTINCT FROM} does the source-version comparison.
     */
    @Query(value = """
            SELECT e.id
            FROM events e
            LEFT JOIN event_enrichment en ON en.event_id = e.id
            WHERE en.event_id IS NULL
               OR en.enrichment_version < :promptVersion
               OR en.enrichment_source_version IS DISTINCT FROM e.source_updated_at
            ORDER BY e.source_updated_at DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<String> findIdsNeedingEnrichment(
            @Param("promptVersion") int promptVersion,
            @Param("limit") int limit
    );
}
