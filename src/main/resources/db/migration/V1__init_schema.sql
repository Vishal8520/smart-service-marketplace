-- V1: Initial Schema
CREATE TABLE roles (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(50)   NOT NULL UNIQUE
);

CREATE TABLE users (
    id          BIGSERIAL     PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(150)  NOT NULL UNIQUE,
    password    VARCHAR(255)  NOT NULL,
    phone       VARCHAR(20),
    role        VARCHAR(30)   NOT NULL DEFAULT 'CUSTOMER',
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE service_listings (
    id           BIGSERIAL      PRIMARY KEY,
    provider_id  BIGINT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id  BIGINT         REFERENCES categories(id) ON DELETE SET NULL,
    title        VARCHAR(200)   NOT NULL,
    description  TEXT,
    tags         TEXT[],
    price        NUMERIC(10, 2) NOT NULL,
    active       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE TABLE bookings (
    id            BIGSERIAL    PRIMARY KEY,
    customer_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_id    BIGINT       NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
    scheduled_at  TIMESTAMPTZ  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    notes         TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_booking_status CHECK (status IN ('PENDING','CONFIRMED','COMPLETED','CANCELLED'))
);

CREATE TABLE reviews (
    id          BIGSERIAL   PRIMARY KEY,
    booking_id  BIGINT      NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    rating      SMALLINT    NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE payments (
    id              BIGSERIAL      PRIMARY KEY,
    booking_id      BIGINT         NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    amount          NUMERIC(10, 2) NOT NULL,
    currency        VARCHAR(10)    NOT NULL DEFAULT 'INR',
    status          VARCHAR(20)    NOT NULL DEFAULT 'INITIATED',
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    gateway_signature VARCHAR(300),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_payment_status CHECK (status IN ('INITIATED','SUCCESS','FAILED','REFUNDED'))
);

-- Indexes
CREATE INDEX idx_service_listings_provider  ON service_listings(provider_id);
CREATE INDEX idx_service_listings_category  ON service_listings(category_id);
CREATE INDEX idx_service_listings_active    ON service_listings(active);
CREATE INDEX idx_bookings_customer          ON bookings(customer_id);
CREATE INDEX idx_bookings_service           ON bookings(service_id);
CREATE INDEX idx_bookings_status            ON bookings(status);
