-- V8__database_indexes_and_constraints.sql
-- Idempotent schema migration for foreign key indexing, composite performance indexes, and DB constraints

DROP PROCEDURE IF EXISTS migrate_v8;

DELIMITER //

CREATE PROCEDURE migrate_v8()
BEGIN
    -- 1. Index on bookings.service_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'bookings' 
          AND INDEX_NAME = 'idx_bookings_service_id'
    ) THEN
        CREATE INDEX idx_bookings_service_id ON bookings(service_id);
    END IF;

    -- 2. Composite Index on bookings(customer_id, status)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'bookings' 
          AND INDEX_NAME = 'idx_bookings_customer_status'
    ) THEN
        CREATE INDEX idx_bookings_customer_status ON bookings(customer_id, status);
    END IF;

    -- 3. Index on payments.confirmed_by_admin_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND INDEX_NAME = 'idx_payments_confirmed_by_admin_id'
    ) THEN
        CREATE INDEX idx_payments_confirmed_by_admin_id ON payments(confirmed_by_admin_id);
    END IF;

    -- 4. Index on payments.reversed_by_admin_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND INDEX_NAME = 'idx_payments_reversed_by_admin_id'
    ) THEN
        CREATE INDEX idx_payments_reversed_by_admin_id ON payments(reversed_by_admin_id);
    END IF;

    -- 5. Index on service_listings.category_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'service_listings' 
          AND INDEX_NAME = 'idx_service_listings_category_id'
    ) THEN
        CREATE INDEX idx_service_listings_category_id ON service_listings(category_id);
    END IF;

    -- 6. Index on service_listings.provider_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'service_listings' 
          AND INDEX_NAME = 'idx_service_listings_provider_id'
    ) THEN
        CREATE INDEX idx_service_listings_provider_id ON service_listings(provider_id);
    END IF;

    -- 7. Index on service_listings.city_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'service_listings' 
          AND INDEX_NAME = 'idx_service_listings_city_id'
    ) THEN
        CREATE INDEX idx_service_listings_city_id ON service_listings(city_id);
    END IF;

    -- 8. Composite Index for Service Discovery on service_listings(city_id, active, status)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'service_listings' 
          AND INDEX_NAME = 'idx_services_city_active_status'
    ) THEN
        CREATE INDEX idx_services_city_active_status ON service_listings(city_id, active, status);
    END IF;

    -- 9. Index on payment_audit_logs.payment_id
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payment_audit_logs' 
          AND INDEX_NAME = 'idx_payment_audit_logs_payment_id'
    ) THEN
        CREATE INDEX idx_payment_audit_logs_payment_id ON payment_audit_logs(payment_id);
    END IF;

    -- 10. Unique Constraint on reviews.booking_id if not present
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'reviews' 
          AND CONSTRAINT_NAME = 'uk_reviews_booking_id'
    ) THEN
        ALTER TABLE reviews ADD CONSTRAINT uk_reviews_booking_id UNIQUE (booking_id);
    END IF;

END //

DELIMITER ;

CALL migrate_v8();

DROP PROCEDURE IF EXISTS migrate_v8;
