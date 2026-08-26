-- Denormalised copy of event_enrichment.rank_score onto events, so the
-- RELEVANCE sort orders and keyset-paginates without joining the enrichment
-- child. Written by the enrichment worker; NULL while an event is unenriched.
ALTER TABLE events
    ADD COLUMN rank_score DOUBLE PRECISION;

CREATE INDEX idx_events_rank_score ON events (rank_score DESC NULLS LAST, id);
