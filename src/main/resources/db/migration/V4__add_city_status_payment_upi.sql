-- V4: Add cities table, update service_listings with status + city_id, redesign payments for UPI reference flow (Idempotent for MySQL 8.x)

DROP PROCEDURE IF EXISTS migrate_v4;

DELIMITER //

CREATE PROCEDURE migrate_v4()
BEGIN
    -- 1. Create cities table if not exists
    CREATE TABLE IF NOT EXISTS cities (
        id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
        name       VARCHAR(100) NOT NULL,
        state      VARCHAR(100) NOT NULL,
        active     TINYINT(1)   NOT NULL DEFAULT 1,
        CONSTRAINT uq_cities_name UNIQUE (name)
    );

    -- 2. Add status to service_listings if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND COLUMN_NAME = 'status'
    ) THEN
        ALTER TABLE service_listings ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
    END IF;

    -- 3. Add city_id to service_listings if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND COLUMN_NAME = 'city_id'
    ) THEN
        ALTER TABLE service_listings ADD COLUMN city_id BIGINT DEFAULT NULL;
    END IF;

    -- 4. Add FK fk_sl_city if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND CONSTRAINT_NAME = 'fk_sl_city'
    ) THEN
        ALTER TABLE service_listings ADD CONSTRAINT fk_sl_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE SET NULL;
    END IF;

    -- 5. Add check constraint chk_service_status if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND CONSTRAINT_NAME = 'chk_service_status'
    ) THEN
        ALTER TABLE service_listings ADD CONSTRAINT chk_service_status CHECK (status IN ('PENDING_REVIEW','APPROVED','REJECTED'));
    END IF;

    -- 6. Drop old gateway columns from payments if they exist
    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'gateway_signature'
    ) THEN
        ALTER TABLE payments DROP COLUMN gateway_signature;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'gateway_order_id'
    ) THEN
        ALTER TABLE payments DROP COLUMN gateway_order_id;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'gateway_payment_id'
    ) THEN
        ALTER TABLE payments DROP COLUMN gateway_payment_id;
    END IF;

    -- 7. Add new payment columns if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'upi_reference_id'
    ) THEN
        ALTER TABLE payments ADD COLUMN upi_reference_id VARCHAR(150) DEFAULT NULL COMMENT 'Unverified UPI reference string';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'confirmed_at'
    ) THEN
        ALTER TABLE payments ADD COLUMN confirmed_at DATETIME(6) DEFAULT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'confirmed_by_admin_id'
    ) THEN
        ALTER TABLE payments ADD COLUMN confirmed_by_admin_id BIGINT DEFAULT NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND COLUMN_NAME = 'notes'
    ) THEN
        ALTER TABLE payments ADD COLUMN notes VARCHAR(500) DEFAULT NULL COMMENT 'Admin remarks';
    END IF;

    -- 8. Modify payment status column
    ALTER TABLE payments MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

    -- 9. Add payment FK fk_py_confirmed_by if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND CONSTRAINT_NAME = 'fk_py_confirmed_by'
    ) THEN
        ALTER TABLE payments ADD CONSTRAINT fk_py_confirmed_by FOREIGN KEY (confirmed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    -- 10. Add Indexes if missing
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND INDEX_NAME = 'idx_service_listings_status'
    ) THEN
        CREATE INDEX idx_service_listings_status ON service_listings(status);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_listings' AND INDEX_NAME = 'idx_service_listings_city'
    ) THEN
        CREATE INDEX idx_service_listings_city ON service_listings(city_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'payments' AND INDEX_NAME = 'idx_payments_status'
    ) THEN
        CREATE INDEX idx_payments_status ON payments(status);
    END IF;

END //

DELIMITER ;

CALL migrate_v4();
DROP PROCEDURE IF EXISTS migrate_v4;
