-- V9__phase1_enhancements.sql
-- Idempotent schema migration for payments state machine, security hardening, KYC, outbox, and refresh tokens

DROP PROCEDURE IF EXISTS migrate_v9;

DELIMITER //

CREATE PROCEDURE migrate_v9()
BEGIN
    -- 1. Add idempotency_key to payments if not exists
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND COLUMN_NAME = 'idempotency_key'
    ) THEN
        ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(100) NULL;
    END IF;

    -- 2. Add unique constraint on payments(idempotency_key)
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND INDEX_NAME = 'uq_payments_idempotency_key'
    ) THEN
        CREATE UNIQUE INDEX uq_payments_idempotency_key ON payments(idempotency_key);
    END IF;

    -- 3. Add unique constraint on payments(upi_reference_id) to prevent reference reuse
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'payments' 
          AND INDEX_NAME = 'uq_payments_upi_reference_id'
    ) THEN
        CREATE UNIQUE INDEX uq_payments_upi_reference_id ON payments(upi_reference_id);
    END IF;

    -- 4. Migrate any legacy CONFIRMED payment status to VERIFIED
    UPDATE payments SET status = 'VERIFIED' WHERE status = 'CONFIRMED';

    -- 5. Add KYC columns to users table
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'users' 
          AND COLUMN_NAME = 'kyc_status'
    ) THEN
        ALTER TABLE users ADD COLUMN kyc_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'users' 
          AND COLUMN_NAME = 'kyc_notes'
    ) THEN
        ALTER TABLE users ADD COLUMN kyc_notes TEXT NULL;
    END IF;

    -- 6. Add Account Lockout columns to users table
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'users' 
          AND COLUMN_NAME = 'failed_login_attempts'
    ) THEN
        ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'users' 
          AND COLUMN_NAME = 'account_locked_until'
    ) THEN
        ALTER TABLE users ADD COLUMN account_locked_until DATETIME(6) NULL;
    END IF;

    -- 7. Add review_count to service_listings
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'service_listings' 
          AND COLUMN_NAME = 'review_count'
    ) THEN
        ALTER TABLE service_listings ADD COLUMN review_count INT NOT NULL DEFAULT 0;
    END IF;

    -- 8. Create refresh_tokens table for secure token rotation & revocation
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'refresh_tokens'
    ) THEN
        CREATE TABLE refresh_tokens (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            token_hash VARCHAR(255) NOT NULL,
            expires_at DATETIME(6) NOT NULL,
            revoked TINYINT(1) NOT NULL DEFAULT 0,
            replaced_by_token VARCHAR(255) NULL,
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
            CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            CONSTRAINT uq_rt_token_hash UNIQUE (token_hash)
        );
        CREATE INDEX idx_refresh_token_user ON refresh_tokens(user_id);
    END IF;

    -- 9. Create email_outbox table for transactional email persistence and retries
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'email_outbox'
    ) THEN
        CREATE TABLE email_outbox (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            recipient VARCHAR(255) NOT NULL,
            subject VARCHAR(300) NOT NULL,
            body LONGTEXT NOT NULL,
            status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
            attempts INT NOT NULL DEFAULT 0,
            last_attempt_at DATETIME(6) NULL,
            error_message TEXT NULL,
            created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        );
        CREATE INDEX idx_email_outbox_status ON email_outbox(status);
    END IF;

END //

DELIMITER ;

CALL migrate_v9();

DROP PROCEDURE IF EXISTS migrate_v9;
