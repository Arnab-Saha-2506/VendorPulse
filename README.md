# VendorPulse - Backend Platform

VendorPulse is an enterprise-grade Supplier Performance & Procurement Collaboration platform implemented as a modular monolith in Spring Boot. It streamlines the purchase order lifecycle, bilateral supplier-buyer negotiations, legally compliant agreement generation, delivery & quality tracking, and algorithmic supplier evaluation (DRS, CSS, DCS, QPS, CDI).

---

## 🛠️ Tech Stack & Architecture

- **Language & Runtime**: Java 22
- **Framework**: Spring Boot 4.1.2 (Web, Security, Data JPA, Validation, Mail)
- **Database**: PostgreSQL 18.6 with 12 PostgreSQL Named ENUM types and 20 relational tables (zero extensions).
- **ORM Configuration**: Hibernate 7 with strict schema validation (`spring.jpa.hibernate.ddl-auto=validate`).
- **Security & Identity**:
  - Stateless JWT Access Tokens (15-minute expiry) signed with HMAC-SHA256 (`sub`, `email`, `role`, `sid`, `supplierId`).
  - Rotating Refresh Tokens (7-day sliding expiry) with cryptographic session IDs (`sid`).
  - Active Session Registry with instant multi-device revocation (`/auth/logout-all`, `/auth/sessions/revoke/{sid}`).
  - In-memory sliding-window Rate Limiting (max 5 failed attempts per minute; 6th attempt triggers `429 RATE_LIMITED`).
- **PDF Generation**: OpenPDF for cryptographic and formal bilateral purchase order agreements.
- **API Documentation**: springdoc OpenAPI 3.0 / Swagger UI (`/swagger-ui.html`).

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java 22 JDK** installed and configured in `PATH`.
- **PostgreSQL 14+** running on `localhost:5432` with database `VendorPulseDB` (`postgres` / `root`).

### 2. Database Initialization
Execute the approved PostgreSQL schema in `src/main/resources/schema.sql` to initialize all 12 ENUM types, 20 relational tables, indexes, constraints, and sequences.

### 3. Build & Run Application
```bash
# Run all unit and integration tests (20 tests verifying Scoring, Status Machine, Rate Limiting, JWT, API flows)
.\mvnw.cmd test

# Run the VendorPulse application
.\mvnw.cmd spring-boot:run
```
The application starts up on port **2021** (or custom `${PORT}`).

---

## 🔑 Pre-Seeded Demo Credentials

All seeded users have the password: `Password123!`

| Role | Email | User Code | Scope & Description |
|---|---|---|---|
| **Procurement Manager** | `admin.pm@vendorpulse.com` | `PM001` | Full manufacturer procurement, PO creation, approval, and supplier management. |
| **Supplier Manager** | `supplier.sm@vendorpulse.com` | `SM001` | Supplier portal user for *Apex Engineering Ltd* (`SUP001`). Can negotiate, revise commitments, and mark delivery. |
| **System Admin** | `admin@vendorpulse.com` | `ADM001` | System administrator with full access to scoring settings, user provisioning, and background jobs. |

---

## 📊 Modules & Core Business Logic

### 1. Authentication & Session Management (`com.tcs.VendorPulse.auth`)
- **API-001**: `POST /api/v1/auth/login` (rate limited; enforces account status `ACTIVE`).
- **API-002**: `GET /api/v1/auth/me` (reads profile from active JWT claims).
- **API-003**: `POST /api/v1/auth/refresh` (single-use rotating refresh token).
- **API-004**: `POST /api/v1/auth/logout` (invalidates current session `sid`).
- **API-NEW-01**: `POST /api/v1/auth/logout-all` (revokes all active sessions for the user).
- **API-NEW-02**: `GET /api/v1/auth/sessions` (lists active user sessions with IP and user-agent).
- **API-NEW-03**: `DELETE /api/v1/auth/sessions/{sid}` (revokes specific session).
- **API-NEW-04**: `POST /api/v1/auth/forgot-password/request-otp` (sends 6-digit OTP).
- **API-NEW-05**: `POST /api/v1/auth/forgot-password/verify-otp` (validates OTP; returns reset authorization token).
- **API-NEW-06**: `POST /api/v1/auth/forgot-password/reset` (resets password using auth token).

### 2. Supplier Lifecycle & Onboarding (`com.tcs.VendorPulse.supplier`)
- **API-011**: `POST /api/v1/public/suppliers/register` (public multipart supplier onboarding).
- **API-012 to API-015**: Document upload, preview, download, and review (`PENDING`, `APPROVED`, `REJECTED`).
- **API-016 to API-021**: Procurement approval queue, approve/reject supplier, and category mappings.
- **API-022 to API-027**: Supplier profile management and supplier directory with multi-criteria search.

### 3. Purchase Order Core & Status Engine (`com.tcs.VendorPulse.po`)
- **PO Status Transitions**: `DRAFT` ➔ `SUBMITTED` ➔ `CONFIRMED` ➔ `IN_PROGRESS` ➔ `PARTIALLY_DELIVERED` ➔ `DELIVERED`.
- Strict transition validation enforced by `PoStatusMachine` (terminal states `DELIVERED` and `CANCELLED` cannot transition).
- Automated sequence generation: `PO-YYYY-NNNN` using PostgreSQL sequence `po_number_seq`.
- Dynamic search using JPA Specifications (`searchPos`) with supplier company isolation.

### 4. Bilateral Negotiation & Agreement (`com.tcs.VendorPulse.negotiation`, `com.tcs.VendorPulse.agreement`)
- Alternate-turn enforcement: Manufacturers and Suppliers counter-propose price and dates.
- Accepting a proposal immediately creates a formal bilateral `Agreement`, generates an official PDF via OpenPDF, creates the initial `ORIGINAL` commitment (seq 0), and transitions the PO to `CONFIRMED`.

### 5. Commitments, Deliveries & Inspection (`com.tcs.VendorPulse.commitment`, `com.tcs.VendorPulse.delivery`)
- **BR-12 & BR-13**: Suppliers can revise committed dates before shipment with mandatory reasons (`REVISION` seq 1..n).
- **BR-14**: Marking dispatched transitions PO to `IN_PROGRESS`.
- **BR-15**: Delivery recording with accepted/rejected quantities and 1-5 quality ratings. Full delivery transitions PO to `DELIVERED`.

### 6. Scoring Engine (`com.tcs.VendorPulse.scoring`)
Algorithmic evaluation computing 5 core supplier performance metrics:
- **DRS (Delivery Reliability Score)**: Percentage of delivered POs completed on or before latest committed date.
- **CSS (Commitment Stability Score)**: $\max(0, 100 - 25 \times \text{avg revisions per PO})$.
- **DCS (Delivery Consistency Score)**: $\max(0, 100 - 10 \times \sigma_{\text{delay days}})$.
- **QPS (Quality Performance Score)**: $0.6 \times \text{Acceptance Rate} + 0.4 \times (\text{Avg Rating} / 5 \times 100)$.
- **CDI (Commitment Drift Index)**: Average drift percentage relative to original lead time.
- **Overall Score**: Configurable weighted composite ($35\%$ DRS, $25\%$ CSS, $20\%$ DCS, $20\%$ QPS).
- **Minimum Order Threshold**: Suppliers with $< 3$ delivered orders are designated as "Unrated".

### 7. Dashboards, Alerts & Reports (`dashboard`, `alert`, `report`, `inventory`, `jobs`)
- Real-time KPIs for both Procurement Managers and Supplier Portals.
- Automated alert triggers on commitment revisions, shipment delays, and low inventory stock.
- Exportable CSV and JSON reports for purchase orders, supplier performance, and quality logs.
- Background scheduled jobs with manual instant trigger endpoint: `POST /api/v1/admin/jobs/run-all`.

---

## 🧪 Testing

The test suite includes comprehensive unit and integration tests:
```bash
.\mvnw.cmd test
```
- `VendorPulseApplicationTests`: Validates Spring application context bootstrapping and Hibernate DDL validation.
- `JwtServiceTest`: Validates HMAC signing, claim extraction, and expiration verification.
- `RateLimitingServiceTest`: Tests sliding window threshold (5 allowed, 6th blocked with `429`).
- `PoStatusMachineTest`: Tests finite state machine transitions and terminal state enforcement.
- `ScoringEngineTest`: Tests mathematical formulas for DRS, CSS, DCS, QPS, CDI, and unrated threshold.
- `AuthControllerIntegrationTest`: Tests `/api/v1/auth/login` and `/api/v1/auth/me` with Spring Security.
- `PurchaseOrderIntegrationTest`: Tests end-to-end PO creation, submission, search specification, and background jobs.

