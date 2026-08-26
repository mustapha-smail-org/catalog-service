package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.EventEnrichmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEnrichmentRepository
        extends JpaRepository<EventEnrichmentEntity, String> {
}
