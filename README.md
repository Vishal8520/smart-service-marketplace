# Smart Service Marketplace Platform — Backend

A role-based service-booking marketplace platform (UrbanClap/Fiverr style) built with **Spring Boot 3.x**, **Java 17**, **Spring Security 6**, **JWT**, **PostgreSQL**, **Flyway**, and **Razorpay Integration**.

---

## 🛠️ Architecture & Features

### Core Modules
1. **Authentication & Authorization**
   - JWT Access Token (15m expiry) + Refresh Token (7d expiry)
   - Role-Based Access Control (`ADMIN`, `SERVICE_PROVIDER`, `CUSTOMER`)
   - Password hashing with BCrypt
2. **Service Management**
   - Service Providers create/update/deactivate listings
   - Public paginated search by keyword, category, tag, and price sorting
   - Dynamic per-service average rating aggregation
3. **Booking System**
   - Customers create bookings with scheduling timestamps
   - Strict status transition workflow: `PENDING` ➔ `CONFIRMED` ➔ `COMPLETED` / `CANCELLED`
   - Customer self-cancellation & provider queue management
4. **Reviews & Ratings**
   - Customers rate (1–5 stars) & review completed bookings only
   - One review per booking guard constraint
5. **Razorpay Payments**
   - Checkout order creation in paise (INR)
   - Webhook processing with HMAC-SHA256 signature verification
6. **Async Email Notifications**
   - `@Async` booking confirmation and status update emails via Spring Mail (MailHog integration in Dev)
7. **Admin Analytics**
   - Platform-wide aggregate revenue, user count breakdown, booking status totals, and average platform rating
8. **REST Error Standard**
   - RFC 7807 `ProblemDetail` standard responses across all controller advice handlers

---

## 📁 Directory Structure

```
smart-service-marketplace/
├── src/
│   ├── main/
│   │   ├── java/com/example/marketplace/
│   │   │   ├── config/              # Security, CORS, Swagger, Auditing, JWT configs
│   │   │   ├── controller/          # REST Controllers (Auth, Service, Booking, Review, Payment, Admin)
│   │   │   ├── dto/                 # Request & Response DTOs
│   │   │   ├── entity/              # JPA Entities (User, ServiceListing, Booking, Review, Payment, Category)
│   │   │   ├── exception/           # Global RFC 7807 Exception Handler & Custom Exceptions
│   │   │   ├── repository/          # Spring Data JPA Repositories
│   │   │   ├── security/            # JwtService, JwtAuthFilter, UserDetailsServiceImpl
│   │   │   ├── service/             # Business Logic Services
│   │   │   └── util/                # DateTimeUtils
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/        # Flyway SQL schema & seed scripts
│   └── test/                        # JUnit 5 & Mockito test suites
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🚀 How to Run Locally

### Prerequisites
- JDK 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Run via Docker Compose (Recommended)

```bash
docker-compose up --build -d
```

This starts:
- **PostgreSQL Database** on `localhost:5432` (`marketplace_db`)
- **MailHog Web UI** on `http://localhost:8025` (SMTP port `1025`)
- **Spring Boot Backend** on `http://localhost:8080`

### 2. Run Manually (Local Dev Mode)

1. Start Postgres and MailHog:
   ```bash
   docker-compose up postgres mailhog -d
   ```
2. Run Spring Boot application:
   ```bash
   mvn clean spring-boot:run -Dspring-boot.run.profiles=dev
   ```

---

## 📖 API Documentation & Swagger UI

Once running, access interactive OpenAPI docs at:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`

---

## 🗝️ Default Admin Credentials (Seeded via Flyway V2)

- **Email:** `admin@marketplace.com`
- **Password:** `Admin@123`
- **Role:** `ADMIN`

---

## 🔑 Key API Endpoints

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register new customer or provider |
| `POST` | `/api/auth/login` | Public | Login and receive JWT access/refresh tokens |
| `POST` | `/api/auth/refresh` | Public | Obtain new access token using refresh token |
| `GET` | `/api/services` | Public | Search active service listings with filters |
| `POST` | `/api/services` | `SERVICE_PROVIDER` | Create new service listing |
| `PUT` | `/api/services/{id}` | `SERVICE_PROVIDER` (Owner) | Update existing service |
| `DELETE` | `/api/services/{id}` | `SERVICE_PROVIDER` (Owner) | Deactivate service listing |
| `POST` | `/api/bookings` | `CUSTOMER` | Book a service |
| `GET` | `/api/bookings/me` | `CUSTOMER` | Get logged-in customer's bookings |
| `GET` | `/api/bookings/provider` | `SERVICE_PROVIDER` | Get provider's incoming bookings |
| `PATCH` | `/api/bookings/{id}/status` | `SERVICE_PROVIDER` | Update booking status (`CONFIRMED`, `COMPLETED`, `CANCELLED`) |
| `POST` | `/api/reviews` | `CUSTOMER` | Submit review for completed booking |
| `POST` | `/api/payments/create-order` | `CUSTOMER` | Create Razorpay payment order |
| `POST` | `/api/payments/webhook` | Public | Razorpay signature verification webhook |
| `GET` | `/api/admin/analytics` | `ADMIN` | Platform analytics dashboard data |

---

## 🧪 Running Unit Tests

```bash
mvn clean test
```
