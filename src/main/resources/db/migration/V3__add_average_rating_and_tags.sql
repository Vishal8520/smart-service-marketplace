-- V3: Add average_rating column and service_tags element collection table

ALTER TABLE service_listings 
    ADD COLUMN IF NOT EXISTS average_rating DOUBLE PRECISION DEFAULT 0.0;

CREATE TABLE IF NOT EXISTS service_tags (
    service_id BIGINT NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
    tag VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_service_tags_service ON service_tags(service_id);
