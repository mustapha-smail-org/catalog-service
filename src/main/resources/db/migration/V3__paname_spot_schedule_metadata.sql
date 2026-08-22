ALTER TABLE events
    ADD COLUMN lead_text TEXT,
    ADD COLUMN date_description TEXT,
    ADD COLUMN image_credit VARCHAR(1000),
    ADD COLUMN transport TEXT;
