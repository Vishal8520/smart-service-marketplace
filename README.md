<div align="center">

# 🛠️ ServeNow
### *On-Demand Smart Service Marketplace Platform*

[![Java 17+](https://img.shields.io/badge/Java-17%2B-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Spring Boot 3.2.5](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-blue.svg?style=for-the-badge&logo=mysql)](https://www.mysql.com/)
[![Flyway Migrations](https://img.shields.io/badge/Flyway-10.x-red.svg?style=for-the-badge&logo=flyway)](https://flywaydb.org/)
[![Docker Ready](https://img.shields.io/badge/Docker-Enabled-2496ED.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

*An enterprise-grade, multi-city service marketplace connecting consumers with verified local service professionals (Plumbing, Electrical, Cleaning, Tutoring, Web Development).*

---

[Key Features](#-key-features) • [System Architecture](#-system-architecture) • [Key Decisions](#-key-engineering-decisions) • [API Specs](#-api-specification) • [Getting Started](#-getting-started) • [Docker Setup](#-docker-environment)

</div>

---

## 📖 Executive Summary

Modern service platforms require a balance between **low user friction**, **strict privacy controls**, **secure reference-based payments**, and **reliable background communication**. 

**ServeNow** is engineered to address these core challenges:
- **City-Based Service Discovery**: Multi-city filtering allowing users to discover active, approved services in their specific city.
- **Reference-and-Confirm UPI Payment Engine**: Structured manual UPI transaction ID flow with admin verification, designed for seamless future Razorpay/Cashfree PSP plug-and-play drop-in.
- **Asynchronous Gmail Notification Subsystem**: Background thread pool executing rich HTML email alerts via `JavaMailSender` without blocking HTTP requests.
- **Role-Secured Admin Management Console**: Full control over service listing approvals (`PENDING_REVIEW` → `APPROVED`), payment verification (`PENDING` → `CONFIRMED`), and city configuration.
- **Server-Side Data Privacy Guard**: Provider phone & email details are strictly masked (`null`) at the backend layer until payment is confirmed by an admin.

---

## ✨ Key Features

### 📍 City Discovery & Amazon-Inspired Frontend
- **High-Contrast Design System**: Dark Navy (`#131921`) header, Amber action triggers (`#febd69`), and price badges (`#f08804`).
- **City Selector Header**: Public city selection dropdown dynamically updating search scope.
- **Interactive Multi-Category Filtering**: Instant filter across Plumbing, Electrical, Cleaning, Tutoring, Design, Web Dev, Carpentry, and Pest Control.
- **Guided 4-Step Checkout**: `1. Details → 2. Location → 3. UPI Reference Payment → 4. Confirmation`.

### 💳 Reference-Based UPI Payment & PSP Integration Layer
- **Manual Reference-and-Confirm Flow**: Customers scan platform UPI VPA (`servenow@upi`) and submit 12-digit transaction UTR reference IDs.
- **PaymentGatewayPort Interface**: Clean architectural abstraction for future PSP gateway integration.
- **Contact Info Unlock**: Sensitive provider contact details unlock automatically upon admin payment confirmation.

### 📩 Asynchronous Gmail Notification System
- **Non-Blocking Execution**: `@EnableAsync` with custom thread pool executor.
- **Transactional HTML Templates**: Automatic background emails for booking creation, payment submission, confirmation (unlocking contact details), and rejection.

### 🛡️ Admin Dashboard & Security
- **Role-Based Access Control**: `/api/admin/**` endpoints secured with Spring Security (`ROLE_ADMIN`).
- **Service Approvals**: Approve or reject provider-submitted listings.
- **Payment Verification**: Verify customer UPI references against bank statements.
- **City Management**: Add operating cities and toggle city visibility.

---

## 🏗️ System Architecture

### 📂 Directory Structure

```text
smart-service-marketplace/
├── 📁 src/main/java/com/example/marketplace/
│   ├── 📁 config/          # Security, CORS, Async Thread Pool, Flyway, & OpenAPI Configs
│   ├── 📁 controller/      # REST Endpoints (Auth, Service, Booking, Payment, Admin, City, Health)
│   ├── 📁 dto/             # Data Transfer Objects (Request/Response Models)
│   ├── 📁 entity/          # JPA Domain Entities (User, ServiceListing, Booking, Payment, City)
│   ├── 📁 exception/       # Centralized RFC 7807 Global Exception Handling
│   ├── 📁 port/            # PaymentGatewayPort Abstraction Interface
│   ├── 📁 repository/      # Spring Data JPA Repositories (Optimized with @EntityGraph)
│   ├── 📁 security/        # Stateless Security & JWT Authentication Filters
│   └── 📁 service/         # Encapsulated Business Services (Payment, Email, Admin, City)
├── 📁 src/main/resources/
│   ├── 📁 db/migration/    # Flyway Versioned SQL Migration Scripts
│   ├── 📁 static/          # Single-Page Web App (index.html)
│   └── application.yml     # Application Configuration (MySQL, Mailer, JWT Secret)
├── docker-compose.yml      # Multi-container Docker Stack (MySQL 8.0 + MailHog + App)
├── Dockerfile              # Production Container Build Specification
└── pom.xml                 # Maven Build Dependencies
```

---

## 🔄 End-to-End Payment & Approval Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 👤 Customer
    actor Provider as 🧑‍🔧 Service Provider
    actor Admin as 🛡️ Admin
    participant Backend as ⚙️ Spring Boot API
    participant Mailer as 📧 Email Subsystem (Async)
    participant DB as 🛢️ MySQL Database

    Provider->>Backend: POST /api/services (Create Listing)
    Backend->>DB: Save Service (Status: PENDING_REVIEW)
    Admin->>Backend: PUT /api/admin/services/{id}/approve
    Backend->>DB: Update Service (Status: APPROVED)

    Customer->>Backend: POST /api/bookings (Create Order)
    Backend->>DB: Save Booking (Status: PENDING)
    Backend-->>Mailer: Trigger Booking Confirmation Email (Async)

    Customer->>Backend: POST /api/payments (Submit UPI Reference)
    Backend->>DB: Save Payment (Status: PENDING)
    Backend-->>Mailer: Trigger Payment Submitted Email (Async)

    Admin->>Backend: POST /api/admin/payments/{id}/confirm
    Backend->>DB: Update Payment -> CONFIRMED, Booking -> CONFIRMED
    Backend-->>Mailer: Trigger Payment Confirmed Email with Unlocked Contact Details
```

---

## ⚡ Key Engineering Decisions

> [!NOTE]
> **1. Architectural PSP Decoupling via `PaymentGatewayPort`**
> Rather than locking the business logic to a specific Payment Service Provider (PSP), we created `PaymentGatewayPort`. The manual UPI reference flow implements this port today, allowing future Razorpay/Cashfree integrations to be swapped in with zero changes to core service logic.

> [!IMPORTANT]
> **2. Defense-in-Depth Contact Masking**
> Masking provider contact info on the frontend alone is insecure. In ServeNow, `BookingService.toResponse()` dynamically checks payment status (`CONFIRMED` or `COMPLETED`). If unverified, email and phone attributes are stripped at the server layer before JSON response serialization.

> [!TIP]
> **3. Asynchronous Non-Blocking Email Dispatch**
> Sending SMTP emails synchronously adds 1–3 seconds of latency to HTTP requests. By configuring Spring `@Async` with a dedicated thread pool executor (`AsyncConfig.java`), HTTP API responses return instantly while email sending occurs in the background.

---

## 🧰 Tech Stack Matrix

| Layer | Technology | Details |
|---|---|---|
| **Language** | Java 17 / JDK 25 | Standard JDK Features, Modern Syntax |
| **Backend Framework** | Spring Boot 3.2.5 | Spring MVC, Spring Security, Spring Data JPA, Spring Async |
| **Database** | MySQL 8.0 | InnoDB Engine, ACID Compliant Transactions |
| **Schema Migration** | Flyway 10.x | Version-Controlled SQL Scripts |
| **Email Engine** | JavaMailSender + Async | HTML Templates, Non-blocking Thread Pool |
| **Frontend** | Vanilla HTML5 / CSS3 / ES6+ | Zero-Dependency Single Page Application with City Selector |
| **Containerization** | Docker & Docker Compose | Multi-container Orchestration |

---

## 📡 API Specification

### 📊 Health & Diagnostics
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/health` | Database connection status & live record metrics | Public |

### 📍 Cities & Service Discovery
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/cities` | List active platform cities | Public |
| `GET` | `/api/services` | Search approved services (`?cityId=1&keyword=...`) | Public |
| `GET` | `/api/services/{id}` | Get service details (Provider contact masked) | Public |
| `POST` | `/api/services` | Register service listing (Starts in `PENDING_REVIEW`) | Public |

### 💳 Payments & Bookings
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/bookings` | Create new service booking | Public |
| `POST` | `/api/payments` | Submit manual 12-digit UPI reference ID | Authenticated |
| `GET` | `/api/bookings/customer` | Customer booking history (`?email=...`) | Public |
| `GET` | `/api/bookings/provider` | Provider job assignments (`?email=...`) | Public |

### 🛡️ Admin Management Console
| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/admin/services` | List pending services for review (`?status=PENDING_REVIEW`) | `ROLE_ADMIN` |
| `PUT` | `/api/admin/services/{id}/approve` | Approve service listing | `ROLE_ADMIN` |
| `PUT` | `/api/admin/services/{id}/reject` | Reject service listing | `ROLE_ADMIN` |
| `GET` | `/api/admin/payments` | List pending UPI payment references (`?status=PENDING`) | `ROLE_ADMIN` |
| `POST` | `/api/admin/payments/{id}/confirm` | Confirm payment & unlock provider contact | `ROLE_ADMIN` |
| `POST` | `/api/admin/payments/{id}/reject` | Reject payment reference with reason note | `ROLE_ADMIN` |
| `POST` | `/api/admin/cities` | Add new operating city | `ROLE_ADMIN` |
| `PUT` | `/api/admin/cities/{id}/toggle` | Toggle active status of a city | `ROLE_ADMIN` |

---

## 🚀 Getting Started

### Prerequisites
- **Java 17+** (or JDK 25)
- **MySQL 8.0** running on `localhost:3306`
- **Maven 3.8+**

### 1️⃣ Database Setup
Create MySQL database:
```sql
CREATE DATABASE IF NOT EXISTS marketplace_db;
```

### 2️⃣ Configure Environment
Set mail credentials in `src/main/resources/application.yml` or environment variables:
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:vishalghasoliya22@gmail.com}
    password: ${MAIL_APP_PASSWORD:your_app_password}
```

### 3️⃣ Build & Package
```bash
mvn clean package -DskipTests
```

### 4️⃣ Launch Application
```bash
java -jar target/smart-service-marketplace-1.0.0-SNAPSHOT.jar --server.port=8081
```

Access portal at: **`http://localhost:8081`**

---

## 🐳 Docker Environment

Launch complete multi-container stack (MySQL 8.0 + MailHog SMTP + Application):

```bash
docker compose up -d
```

- **Web Application**: `http://localhost:8081`
- **MailHog Web UI**: `http://localhost:8025`
- **Database Port**: `3306`

---

## 📄 License & Contact

Distributed under the MIT License. Developed for the **Smart Service Marketplace Major Project**.
