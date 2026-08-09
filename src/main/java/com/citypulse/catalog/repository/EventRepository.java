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
}