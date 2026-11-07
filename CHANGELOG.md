# CHANGELOG - ServeNow Marketplace Platform

All notable changes to the ServeNow project are documented in this file.

---

## [2.0.0] - 2026-11-07 (Phase 1, Phase 2 & Phase 3 Overhaul)

### 🚀 Phase 1: Core Backend & Security Hardening
- **JDK 21 LTS Standardization:**
  - Standardized `pom.xml`, `Dockerfile`, and CI workflow strictly on Java 21 LTS (`eclipse-temurin:21-jre-alpine`).
  - Removed all outdated "JDK 25" claims while maintaining clean, explicit Java code without Lombok.
- **Payment Security & State Machine:**
  - Enforced 12-digit numeric UTR validation regex (`^[0-9]{12}$`) with database unique constraints (`V9__phase1_enhancements.sql`).
  - Implemented formal payment state machine: `PENDING -> AUTO_CONFIRMED -> VERIFIED | REVERSED`. Invalid transitions throw `IllegalStateException`.
  - Added idempotency key deduplication on payment endpoints via `Idempotency-Key` header and database unique constraint.
  - Locked provider contact details until payment is `VERIFIED`.
  - Made `PaymentAuditLog` immutable (`@Column(updatable = false)` and read-only repository methods).
- **Authentication & Security Hardening:**
  - Implemented single-use refresh token rotation with SHA-256 token hashing (`RefreshToken` entity).
  - Added token family reuse detection with automatic revocation upon compromised token replay.
  - Enforced strong password policy (uppercase, lowercase, numeric, special character, min 8 chars).
  - Added Bucket4j login rate limiting (5 attempts/min per IP/key) and automatic account lockout after 5 consecutive failed attempts.
  - Added strict CSP, HSTS, X-Frame-Options (DENY), and X-Content-Type-Options headers.
  - Closed unauthenticated public hole on `/api/payments/**` and added method-level `@PreAuthorize("hasRole('ADMIN')")` on all admin endpoints.
- **Transactional Email Outbox:**
  - Replaced fire-and-forget email dispatch with an immutable, transactional `email_outbox` table.
  - Implemented `ThreadPoolExecutor.CallerRunsPolicy` to eliminate silent task drops under heavy load.
  - Added automated scheduled retry dispatcher with exponential backoff and `PERMANENTLY_FAILED` threshold at 5 attempts.
- **Performance & N+1 Query Elimination:**
  - Stored `averageRating` and `reviewCount` directly on `ServiceListing`, completely eliminating $N+1$ review aggregate queries.
  - Added Caffeine in-memory caching for active cities and categories with write eviction.
  - Added pagination and sorting across list endpoints.
- **Provider KYC & Review Lifecycle:**
  - Added provider KYC approval workflow (`PENDING`, `APPROVED`, `REJECTED`) required before listings can be approved.
  - Enforced completed-booking-only reviews, rating 1-5 validation, and single review per booking.
- **DevOps, Observability & Frontend:**
  - Added MDC `requestId` filter with `X-Request-ID` and structured JSON logging (`logstash-logback-encoder`).
  - Exposed Prometheus metrics at `/actuator/prometheus`.
  - Multi-stage Dockerfile with non-root user (`appuser:appgroup`) and container healthchecks.
  - Added GitHub Actions CI workflow (`.github/workflows/ci.yml`).
  - Split 57KB monolithic `index.html` into modular `index.html`, `css/style.css`, and `js/app.js` with `sessionStorage` token management.

---

### 🤖 Phase 2: AI Intelligence Microservice (`/ai-service`)
- **FastAPI Microservice Architecture:**
  - Created standalone Python FastAPI service (`/ai-service`) containerized with its own `Dockerfile` and `requirements.txt`.
  - Integrated into `docker-compose.yml` with healthchecks and network bridging.
  - Secured inter-service communication with `X-Internal-API-Key` authentication.
- **Resilience4j Circuit Breaker & Fallback:**
  - Implemented `AiServicePort` and `AiServiceClient` with connect (2000ms) and read (4000ms) timeouts.
  - Configured Resilience4j circuit breaker with graceful fallbacks so the marketplace remains 100% operational if `ai-service` is unavailable.
- **AI Feature Suite:**
  1. **Smart Semantic Search:** Natural-language query mapping ("my tap is leaking") to category and services with cosine similarity and keyword boosting.
  2. **Multi-Factor Recommendation Ranking:** Ranks candidate services by rating (0.45), provider response rate (0.25), city match (0.20), and price competitiveness (0.10) with humanized "why recommended" rationales.
  3. **Payment Fraud Scoring:** Combined heuristic rules (reference reuse, velocity spikes, amount mismatch, account age) and `IsolationForest` multivariate anomaly detection.
  4. **Review Intelligence:** Sentiment analysis (-1.0 to 1.0), spam/fake pattern detection (contact leaks, rating contradictions), and automated service review summarization.
  5. **Provider Listing Assistant:** Generates professional descriptions from keywords, calculates fair price ranges via percentile statistics, and screens content against platform policy violations.
  6. **Booking Chatbot:** Conversational concierge with tool-calling capabilities (`search_services`, `check_availability`, `create_booking_draft`, `explain_booking_status`), strict authenticated user permission boundaries, prompt-injection defense, and user confirmation guards.
  7. **AI Observability:** Structured call logging with PII masking, Prometheus metrics (`/metrics`), and admin evaluation feedback loop (`POST /api/v1/feedback`).

---

### 🧪 Phase 3: Verification & Deliverables
- **Unit & Integration Test Suite:**
  - 57 Spring Boot unit and service tests passing with 0 failures and 0 errors.
  - 17 Python FastAPI unit tests with fixtures passing in 0.32s with 0 failures and 0 errors.
  - Total automated test suite: 74 tests.
- **End-to-End Smoke Test Suite:**
  - Automated `smoke_test.py` script verifying all AI microservice and Spring Boot capabilities.
