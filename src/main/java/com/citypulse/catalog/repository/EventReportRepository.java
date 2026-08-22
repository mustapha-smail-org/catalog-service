package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.EventReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventReportRepository extends JpaRepository<EventReportEntity, Long> {
}
