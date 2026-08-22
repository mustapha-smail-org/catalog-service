package com.citypulse.catalog.repository;

import com.citypulse.catalog.entity.FeedbackSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmissionEntity, Long> {
}
