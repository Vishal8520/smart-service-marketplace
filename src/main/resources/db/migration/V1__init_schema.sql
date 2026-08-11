-- V1: Initial Schema (MySQL)
CREATE TABLE IF NOT EXISTS roles (
    id    BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50)  NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL,
    password    VARCHAR(255),
    phone       VARCHAR(20),
    role        VARCHAR(30)  NOT NULL DEFAULT 'CUSTOMER',
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS service_listings (
    id           BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    provider_id  BIGINT         NOT NULL,
    category_id  BIGINT,
    title        VARCHAR(200)   NOT NULL,
    description  TEXT,
    price        DECIMAL(10, 2) NOT NULL,
    active       TINYINT(1)     NOT NULL DEFAULT 1,
    average_rating DOUBLE       DEFAULT 0.0,
    created_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sl_provider FOREIGN KEY (provider_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sl_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS service_tags (
    service_id BIGINT      NOT NULL,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (service_id, tag),
    CONSTRAINT fk_st_service FOREIGN KEY (service_id) REFERENCES service_listings(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS bookings (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    customer_id   BIGINT      NOT NULL,
    service_id    BIGINT      NOT NULL,
    scheduled_at  DATETIME(6) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    address       TEXT,
    notes         TEXT,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_bk_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bk_service  FOREIGN KEY (service_id)  REFERENCES service_listings(id) ON DELETE CASCADE,
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED'))
);

CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id  BIGINT      NOT NULL,
    rating      INT         NOT NULL,
    comment     TEXT,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_reviews_booking UNIQUE (booking_id),
    CONSTRAINT fk_rv_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS payments (
    id                  BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    booking_id          BIGINT         NOT NULL,
    amount              DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(10)    NOT NULL DEFAULT 'INR',
    status              VARCHAR(20)    NOT NULL DEFAULT 'INITIATED',
    gateway_order_id    VARCHAR(100),
    gateway_payment_id  VARCHAR(100),
    gateway_signature   VARCHAR(300),
    created_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_payments_booking UNIQUE (booking_id),
    CONSTRAINT fk_py_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_status CHECK (status IN ('INITIATED','SUCCESS','FAILED','REFUNDED'))
);

-- Indexes
CREATE INDEX idx_service_listings_provider ON service_listings(provider_id);
CREATE INDEX idx_service_listings_category ON service_listings(category_id);
CREATE INDEX idx_service_listings_active   ON service_listings(active);
CREATE INDEX idx_service_tags_service      ON service_tags(service_id);
CREATE INDEX idx_bookings_customer         ON bookings(customer_id);
CREATE INDEX idx_bookings_service          ON bookings(service_id);
CREATE INDEX idx_bookings_status           ON bookings(status);
