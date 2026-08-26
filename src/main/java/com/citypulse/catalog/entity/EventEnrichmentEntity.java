package com.citypulse.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-derived enrichment for an event. A 1:1 child of {@link EventEntity} keyed
 * by the event id, so (re)enrichment is an isolated write off the hot events
 * row. An event with no row here is simply "unenriched" — a first-class state.
 * Written only by the enrichment worker (see Track B), never on the ingest or
 * serving path.
 */
@Getter
@Setter
@Entity
@Table(name = "event_enrichment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEnrichmentEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 150)
    private String eventId;

    @MapsId
    @OneToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private EventEntity event;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "norm_categories", nullable = false)
    private List<String> normCategories = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "mood_affinities", nullable = false)
    private List<String> moodAffinities = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "social_contexts", nullable = false)
    private List<String> socialContexts = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "semantic_tags", nullable = false)
    private List<String> semanticTags = new ArrayList<>();

    @Column(name = "energy_level", length = 10)
    private String energyLevel;

    @Column(name = "environment_fallback", length = 10)
    private String environmentFallback;

    @Column(name = "uniqueness_score")
    private Integer uniquenessScore;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(name = "rank_score")
    private Double rankScore;

    @Column(name = "enrichment_model", nullable = false, length = 100)
    private String enrichmentModel;

    @Column(name = "enrichment_version", nullable = false)
    private Integer enrichmentVersion;

    @Column(name = "enrichment_source_version")
    private Instant enrichmentSourceVersion;

    @Column(name = "enriched_at", nullable = false)
    private Instant enrichedAt;

    public EventEnrichmentEntity(EventEntity event) {
        this.event = java.util.Objects.requireNonNull(event, "event");
    }
}
