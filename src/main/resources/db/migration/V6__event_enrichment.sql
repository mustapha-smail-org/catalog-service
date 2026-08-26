CREATE TABLE event_enrichment
(
    event_id                  VARCHAR(150)     NOT NULL,
    norm_categories           TEXT[]           NOT NULL DEFAULT '{}',
    mood_affinities           TEXT[]           NOT NULL DEFAULT '{}',
    social_contexts           TEXT[]           NOT NULL DEFAULT '{}',
    semantic_tags             TEXT[]           NOT NULL DEFAULT '{}',
    energy_level              VARCHAR(10),
    environment_fallback      VARCHAR(10),
    uniqueness_score          INTEGER,
    quality_score             INTEGER,
    rank_score                DOUBLE PRECISION,
    enrichment_model          VARCHAR(100)     NOT NULL,
    enrichment_version        INTEGER          NOT NULL,
    enrichment_source_version TIMESTAMP WITHOUT TIME ZONE,
    enriched_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_event_enrichment PRIMARY KEY (event_id),
    CONSTRAINT fk_event_enrichment_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE
);

CREATE INDEX idx_enrichment_moods ON event_enrichment USING GIN (mood_affinities);
CREATE INDEX idx_enrichment_social ON event_enrichment USING GIN (social_contexts);
CREATE INDEX idx_enrichment_normcat ON event_enrichment USING GIN (norm_categories);
CREATE INDEX idx_enrichment_rank ON event_enrichment (rank_score DESC);
