-- V7__add_auto_confirm_and_audit_log.sql
-- Idempotent schema migration for Auto-Approve UPI Payments and Payment Audit Logs

DROP PROCEDURE IF EXISTS migrate_v7;

DELIMITER //

CREATE PROCEDURE migrate_v7()
BEGIN
    -- 1. Add auto_confirmed column to payments if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND COLUMN_NAME = 'auto_confirmed'
    ) THEN
        ALTER TABLE payments ADD COLUMN auto_confirmed BOOLEAN NOT NULL DEFAULT TRUE;
    END IF;

    -- 2. Add reversed_at column to payments if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND COLUMN_NAME = 'reversed_at'
    ) THEN
        ALTER TABLE payments ADD COLUMN reversed_at DATETIME(6) NULL;
    END IF;

    -- 3. Add reversed_by_admin_id column to payments if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND COLUMN_NAME = 'reversed_by_admin_id'
    ) THEN
        ALTER TABLE payments ADD COLUMN reversed_by_admin_id BIGINT NULL;
    END IF;

    -- 4. Add reversal_reason column to payments if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND COLUMN_NAME = 'reversal_reason'
    ) THEN
        ALTER TABLE payments ADD COLUMN reversal_reason VARCHAR(500) NULL;
    END IF;

    -- 5. Add foreign key constraint for reversed_by_admin_id if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND CONSTRAINT_NAME = 'fk_payment_reversed_by_admin'
    ) THEN
        ALTER TABLE payments ADD CONSTRAINT fk_payment_reversed_by_admin FOREIGN KEY (reversed_by_admin_id) REFERENCES users(id);
    END IF;

    -- 6. Create payment_audit_logs table if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payment_audit_logs'
    ) THEN
        CREATE TABLE payment_audit_logs (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            payment_id BIGINT NOT NULL,
            from_status VARCHAR(50) NULL,
            to_status VARCHAR(50) NOT NULL,
            action VARCHAR(100) NOT NULL,
            actor_email VARCHAR(255) NOT NULL,
            notes VARCHAR(500) NULL,
            created_at DATETIME(6) NOT NULL,
            CONSTRAINT fk_audit_log_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
        );
    END IF;

END //

DELIMITER ;

CALL migrate_v7();

DROP PROCEDURE IF EXISTS migrate_v7;
