ALTER TABLE feedback_submissions
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN processed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN internal_note TEXT;

ALTER TABLE event_reports
    ADD COLUMN event_id VARCHAR(150),
    ADD COLUMN event_title VARCHAR(500),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ADD COLUMN processed_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN internal_note TEXT;

UPDATE event_reports reports
SET event_id = events.id,
    event_title = events.title
FROM events
WHERE events.slug = reports.event_slug;

ALTER TABLE event_reports
    ALTER COLUMN event_id SET NOT NULL,
    ALTER COLUMN event_title SET NOT NULL,
    ADD CONSTRAINT fk_event_reports_event FOREIGN KEY (event_id) REFERENCES events (id);

CREATE INDEX idx_event_reports_event_id ON event_reports (event_id);
CREATE INDEX idx_feedback_status_created_at ON feedback_submissions (status, created_at);
CREATE INDEX idx_event_reports_status_created_at ON event_reports (status, created_at);

UPDATE events
SET slug = COALESCE(
        NULLIF(
                regexp_replace(
                        regexp_replace(
                                replace(replace(replace(
                                        translate(lower(title),
                                            'àâäáãåçéèêëíìîïñóòôöõúùûüýÿ',
                                            'aaaaaaceeeeiiiinooooouuuuyy'),
                                        'œ', 'oe'), 'æ', 'ae'), 'ß', 'ss'),
                                '[^a-z0-9]+', '-', 'g'),
                        '(^-+|-+$)', '', 'g'),
                ''),
        'event'
    ) || '-' || substring(encode(sha256(convert_to(id, 'UTF8')), 'hex') from 1 for 8);
