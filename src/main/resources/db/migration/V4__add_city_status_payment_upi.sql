-- V4: Add cities table, update service_listings with status + city_id, redesign payments for UPI reference flow

-- ── Cities Table ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS cities (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    state      VARCHAR(100) NOT NULL,
    active     TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT uq_cities_name UNIQUE (name)
);

-- ── Service Listings: add status + city_id ────────────────────
ALTER TABLE service_listings
    ADD COLUMN status  VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN city_id BIGINT      DEFAULT NULL;

ALTER TABLE service_listings
    ADD CONSTRAINT fk_sl_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE SET NULL;

ALTER TABLE service_listings
    ADD CONSTRAINT chk_service_status CHECK (status IN ('PENDING_REVIEW','APPROVED','REJECTED'));

-- ── Redesign payments table for UPI reference-and-confirm flow ─
-- Drop old payment constraints and gateway columns
ALTER TABLE payments DROP FOREIGN KEY fk_py_booking;
ALTER TABLE payments DROP INDEX uq_payments_booking;

ALTER TABLE payments
    DROP COLUMN gateway_signature,
    DROP COLUMN gateway_order_id,
    DROP COLUMN gateway_payment_id,
    ADD COLUMN upi_reference_id       VARCHAR(150) DEFAULT NULL COMMENT 'Unverified UPI reference string provided by customer — NOT the debited account',
    ADD COLUMN confirmed_at            DATETIME(6)  DEFAULT NULL,
    ADD COLUMN confirmed_by_admin_id   BIGINT       DEFAULT NULL,
    ADD COLUMN notes                   VARCHAR(500) DEFAULT NULL COMMENT 'Admin remarks on manual payment verification';

-- Update payment status constraint to match new enum
ALTER TABLE payments DROP CHECK chk_payment_status;
ALTER TABLE payments
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE payments
    ADD CONSTRAINT chk_payment_status CHECK (status IN ('PENDING','CONFIRMED','REJECTED'));

-- Re-add FK + unique constraint
ALTER TABLE payments
    ADD CONSTRAINT uq_payments_booking UNIQUE (booking_id),
    ADD CONSTRAINT fk_py_booking       FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_py_confirmed_by  FOREIGN KEY (confirmed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL;

-- ── New Indexes ─────────────────────────────────────────────────
CREATE INDEX idx_service_listings_status ON service_listings(status);
CREATE INDEX idx_service_listings_city   ON service_listings(city_id);
CREATE INDEX idx_payments_status         ON payments(status);
