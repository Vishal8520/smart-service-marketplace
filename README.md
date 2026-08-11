# ServeNow — Smart Service Marketplace Platform

A modern, production-grade service booking marketplace platform (Amazon / UrbanClap inspired) built with **Spring Boot 3.2**, **Java 17/25**, **MySQL 8.0**, **Flyway**, **Spring Security**, and an **Amazon-Inspired Single-Page Web Frontend**.

---

## 🌟 Key Features

### 🛒 Amazon-Inspired E-Commerce Frontend
- **High-Contrast Design System**: Dark Navy (`#131921`) header, Amber yellow (`#febd69`) action buttons, price badges (`#f08804`), and responsive typography (`Inter`).
- **Interactive Browsing & Search**: Instant category filtering (Plumbing, Electrical, Cleaning, Tutoring, Design, Web Dev, Carpentry, Pest Control) and keyword search.
- **Multi-Step Checkout Stepper**: 4-step order progress UI (`1. Your Details → 2. Address & Schedule → 3. Payment → 4. Confirmation`) with instant 18% GST calculation.
- **Visual Feedback**: CSS shimmer skeleton loaders during data fetch and dynamic toast notifications.

### 🔒 Passwordless Onboarding & Server-Side Security
- **Frictionless Passwordless Flow**: Customers & Providers register instantly with basic contact details (Name, Email, Phone) — no login/password friction.
- **Server-Side Provider Contact Hiding**: Provider phone numbers and email addresses are strictly masked (`null`) in public service listings and pending bookings until payment is completed (`CONFIRMED` / `COMPLETED`).
- **Simulated Payment Gateway**: Simulated payment endpoint (`POST /api/bookings/{id}/pay`) that completes transactions and unlocks verified provider contact details on receipts.

### 🛢️ MySQL & Flyway Architecture
- **MySQL 8.0 Database**: Migrated and optimized schema with HikariCP connection pooling.
- **Automated Flyway Migrations**:
  - `V1__init_schema.sql`: Database schema definition with nullable user passwords and booking addresses.
  - `V2__seed_data.sql`: Seed data for 8 default service categories and system admin user.

### 📊 Health & Monitoring
- **Health Check Endpoint (`GET /api/health`)**: Surfacing real-time database connectivity, MySQL server version, HikariCP pool status, and live table record counts.

---

## 📁 Project Architecture

```
smart-service-marketplace/
├── src/
│   ├── main/
│   │   ├── java/com/example/marketplace/
│   │   │   ├── config/              # Security, CORS, Auditing, OpenAPI configs
│   │   │   ├── controller/          # REST Controllers (Auth, Service, Booking, Payment, User, Health)
│   │   │   ├── dto/                 # Request & Response DTOs
│   │   │   ├── entity/              # JPA Entities (User, ServiceListing, Booking, Payment, Category)
│   │   │   ├── exception/           # Global RFC 7807 Exception Handler
│   │   │   ├── repository/          # Spring Data JPA Repositories (Optimized with @EntityGraph)
│   │   │   ├── security/            # JwtService, JwtAuthFilter, UserDetailsServiceImpl
│   │   │   └── service/             # Business Logic Services
│   │   └── resources/
│   │       ├── application.yml      # Core config (MySQL, Flyway, Server Port 8081)
│   │       ├── static/index.html    # Amazon-style redesigned Single-Page Web App
│   │       └── db/migration/        # Flyway SQL migration scripts
├── pom.xml                          # Maven build pom configured for WAR deployment
└── README.md
```

---

## 🚀 Quick Start & Running Locally

### Prerequisites
- Java 17+ (or JDK 25)
- MySQL 8.0 running on `localhost:3306` (Credentials: `root` / `Vishal@8696`)
- Maven 3.8+

### 1. Database Setup
```sql
CREATE DATABASE IF NOT EXISTS marketplace_db;
```

### 2. Build & Package WAR
```bash
mvn clean package -DskipTests
```

### 3. Launch Spring Boot Application
```bash
java -jar target/smart-service-marketplace-1.0.0-SNAPSHOT.war --server.port=8081
```

### 4. Access App in Browser
Open [http://localhost:8081](http://localhost:8081) in your browser.

---

## 📡 Core API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/health` | DB connection health check & table record counts |
| `GET` | `/api/services` | Search active service listings with category & keyword filters |
| `GET` | `/api/services/{id}` | Detailed service view (provider contact masked) |
| `POST` | `/api/services` | Publish a new service listing (passwordless provider flow) |
| `POST` | `/api/users/quick-register` | Passwordless customer / provider quick registration |
| `POST` | `/api/bookings` | Create new customer service booking |
| `POST` | `/api/bookings/{id}/pay` | Process simulated payment & unlock provider contact info |
| `GET` | `/api/bookings/customer?email=...` | Retrieve customer booking history & receipts |
| `GET` | `/api/bookings/provider?email=...` | Retrieve provider incoming job requests |
| `PATCH` | `/api/bookings/{id}/status` | Update booking status (`CONFIRMED`, `COMPLETED`, `CANCELLED`) |

---

## 📜 License & Acknowledgements
Built for Smart Service Marketplace Major Project. Designed with Spring Boot 3.2 & MySQL 8.0.
