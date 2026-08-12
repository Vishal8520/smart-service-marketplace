-- V6: Add database indexes for fast customer booking lookups and status filtering (Idempotent for MySQL 8.x)

DROP PROCEDURE IF EXISTS migrate_v6;

DELIMITER //

CREATE PROCEDURE migrate_v6()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND INDEX_NAME = 'idx_bookings_customer_id'
    ) THEN
        CREATE INDEX idx_bookings_customer_id ON bookings(customer_id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND INDEX_NAME = 'idx_bookings_customer_status'
    ) THEN
        CREATE INDEX idx_bookings_customer_status ON bookings(customer_id, status);
    END IF;
END //

DELIMITER ;

CALL migrate_v6();
DROP PROCEDURE IF EXISTS migrate_v6;
