# 🧠 MindMap — Psychological Profiling Application

MindMap is a microservice-based web application for self-awareness psychological profiling. It guides users through three progressive assessment levels covering personality traits, ambiguity tolerance, and temperament under pressure. Results are presented as gentle, non-clinical summaries for users, while administrators have access to full analytical profiles with audit logging.

> **Disclaimer:** MindMap is for self-awareness only — it is **not** a clinical diagnosis tool.

---

## Architecture Overview

```
                    ┌──────────────┐
 Browser ────────►  │ API Gateway  │ :8080
                    │ (Spring Cloud│
                    │   Gateway)   │
                    └───┬──┬──┬────┘
                        │  │  │
              ┌─────────┘  │  └─────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │   Auth   │ │Assessment│ │Profiling │
        │ Service  │ │ Service  │ │ Service  │
        │  :8081   │ │  :8082   │ │  :8083   │
        └────┬─────┘ └────┬─────┘ └────┬─────┘
             │            │            │
             ▼            ▼            ▼
        ┌──────────────────────────────────┐
        │   PostgreSQL   │   Redis   │ Kafka │
        │ (3 schemas)    │ +Bloom    │       │
        └──────────────────────────────────┘
```

| Component | Technology | Port |
|-----------|-----------|------|
| Frontend | Angular 17 + Material | 4200 |
| API Gateway | Spring Cloud Gateway | 8080 |
| Eureka Discovery | Spring Cloud Netflix | 8761 |
| Auth Service | Spring Boot 3.2 | 8081 |
| Assessment Service | Spring Boot 3.2 | 8082 |
| Profiling Service | Spring Boot 3.2 | 8083 |
| PostgreSQL | 16-alpine | 5432 |
| Redis + RedisBloom | redis-stack-server | 6379 |
| Kafka | Confluent 7.5 | 29092 |

---

## Features

### User-Facing
- **Instagram-style user IDs** with real-time availability checking (RedisBloom + DB fallback)
- **Recovery paragraph** instead of email/OTP — a random ~50-word paragraph shown once, stored as Argon2id hash
- **3 progressive assessment levels:**
  - **Level 1:** Basic personality (Big Five traits: Openness, Conscientiousness, Extraversion, Agreeableness, Emotional Stability)
  - **Level 2:** Ambiguity tolerance & dark-theme tolerance (with content warnings and skip options)
  - **Level 3:** Temperament under pressure (timed scenario-based questions)
- **Gentle result summaries** — no raw scores, no clinical labels
- **Data export & account deletion** (GDPR-friendly)

### Admin-Facing
- **Full profile viewer** with scores, temperament breakdowns, and confidence flags
- **ABAC-enforced access** — every admin profile view is audit-logged
- **Safety flag dashboard** — flagged responses surface for review
- **Configurable scoring rules** — thresholds and formulas stored in DB, not hardcoded

### Security
- **JWT authentication** with shared secret across services
- **Argon2id** password hashing
- **Rate-limited login** with account lockout after 5 failures
- **AES-256-GCM** encryption for answers and results at rest
- **Header sanitization** at the gateway to prevent header injection
- **RBAC + ABAC** enforced at API and UI levels

---

## Prerequisites

- **Docker** ≥ 24.0 and **Docker Compose** ≥ 2.20
- (For local dev) **Java 17+**, **Maven 3.9+**, **Node.js 18+**, **npm 9+**

---

## Quick Start (Docker Compose)

### 1. Clone and configure

```bash
git clone <your-repo-url> mindmap
cd mindmap

# Create environment file from template
cp .env.template .env
```

### 2. Edit `.env` with real secrets

```dotenv
# Generate a JWT secret (64+ chars):
#   openssl rand -base64 64
JWT_SECRET=<paste-here>

# Generate an AES-256 encryption key:
#   openssl rand -hex 32
ENCRYPTION_KEY=<paste-here>

# Set database password
DB_USER=mindmap
DB_PASSWORD=<strong-password>

# Admin initial password (must be changed on first login)
ADMIN_INITIAL_PASSWORD=<strong-password>
```

### 3. Build and start everything

```bash
docker compose up --build -d
```

### 4. Wait for health checks

```bash
# Watch all services come up (takes 2-3 minutes on first build)
docker compose ps

# Check a specific service
docker compose logs -f auth-service
```

### 5. Access the application

| URL | Description |
|-----|-------------|
| http://localhost:4200 | Frontend application |
| http://localhost:8080 | API Gateway |
| http://localhost:8761 | Eureka Dashboard |

### 6. Login as admin

The system seeds a default admin account on first startup:

- **User ID:** `admin.master`
- **Password:** value of `ADMIN_INITIAL_PASSWORD` from `.env`
- You will be prompted to change the password on first login.

---

## Build Order (from scratch)

If you want to build services individually:

```bash
# 1. Start infrastructure
docker compose up -d postgres redis zookeeper kafka

# 2. Wait for infra health checks to pass
docker compose ps  # all should show "healthy"

# 3. Start Eureka
docker compose up -d eureka-server
# Wait ~30s for it to be healthy

# 4. Start the 3 microservices
docker compose up -d auth-service assessment-service profiling-service
# Wait ~60s for all to register with Eureka

# 5. Start the gateway
docker compose up -d api-gateway

# 6. Start the frontend
docker compose up -d frontend
```

---

## Local Development (without Docker)

### Backend services

Each service is a standard Maven project:

```bash
cd auth-service
mvn clean compile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Repeat for `assessment-service` and `profiling-service`.

> **Note:** You need PostgreSQL, Redis (with RedisBloom), and Kafka running locally. The `dev` profile in each `application.yml` expects `localhost` connections.

### Frontend

```bash
cd frontend
npm ci
ng serve
```

The dev server runs on http://localhost:4200 and proxies API calls to `http://localhost:8080`.

---

## Project Structure

```
mindmap/
├── api-gateway/              # Spring Cloud Gateway (JWT filter, rate limiting, CORS)
├── assessment-service/       # Assessment sessions, questions, answers, Kafka producer
├── auth-service/             # Registration, login, recovery, RedisBloom, admin seeding
├── eureka-server/            # Service discovery
├── frontend/                 # Angular 17 standalone components + Material
│   └── src/app/
│       ├── core/             # Services, guards, interceptors, models
│       ├── pages/            # 13 page components (register, login, dashboard, admin/*)
│       └── shared/           # Reusable components (disclaimer, dialogs, strength meter)
├── infra/
│   └── init-schemas.sql      # Creates the 3 PostgreSQL schemas
├── profiling-service/        # Scoring engine, temperament classification, Kafka consumer
├── .env.template             # Environment variable template
├── .gitignore
├── docker-compose.yml        # Full stack orchestration
└── README.md                 # ← You are here
```

---

## Database Schemas

One PostgreSQL instance, three isolated schemas:

| Schema | Service | Key Tables |
|--------|---------|-----------|
| `auth_schema` | Auth Service | users, user_roles, reserved_usernames, login_attempts, recovery_attempts, audit_log |
| `assessment_schema` | Assessment Service | questions, answer_options, assessment_sessions, user_answers, safety_flags, trait_definitions |
| `profiling_schema` | Profiling Service | user_profiles, profile_scores, temperament_profiles, scoring_rules, profile_access_audit, user_summaries |

Each service manages its own schema via Flyway migrations (`V1__init_*.sql`).

---

## API Endpoints (via Gateway)

All API calls go through the gateway at `:8080`. Key routes:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register new user |
| POST | `/api/auth/login` | Public | Login |
| GET | `/api/auth/userid/check?userId=...` | Public | Check ID availability |
| GET | `/api/auth/userid/suggestions?name=...` | Public | Get 5 ID suggestions |
| POST | `/api/auth/recovery/initiate` | Public | Start recovery flow |
| POST | `/api/assessment/sessions` | USER | Create assessment session |
| POST | `/api/assessment/sessions/{id}/answers` | USER | Submit an answer |
| GET | `/api/assessment/levels/status` | USER | Get level unlock status |
| GET | `/api/profiling/me` | USER | Get own profile summary |
| GET | `/api/profiling/admin/users/{id}/full` | ADMIN | Get full user profile |
| GET | `/api/profiling/admin/users/{id}/temperament` | ADMIN | Get temperament analysis |

See [Deliverable 03 — REST Endpoints](docs/03-rest-endpoints.md) for the full list of 38 endpoints.

---

## Event-Driven Flow

```
Assessment Service                              Profiling Service
     │                                               │
     │  assessment.completed                         │
     │  (Kafka topic)                                │
     ├──────────────────────────────────────────────►│
     │                                               │
     │                                               ├─ Score calculation
     │                                               ├─ Confidence analysis
     │                                               ├─ Temperament classification (L3)
     │                                               ├─ Summary generation
     │                                               │
     │                    profile.generated           │
     │◄──────────────────────────────────────────────┤
     │                    (Kafka topic)               │
```

---

## Safety

- **Self-harm detection:** If an answer is flagged (`is_safety_flag = true`), a calm support message with helpline information is shown. The flag is recorded for admin review.
- **Content warnings:** Level 2 Part B (dark-fantasy scenarios) shows a content warning before each section. Every question is skippable without scoring penalty.
- **Disclaimer:** Every result page includes: *"For self-awareness only, not a clinical diagnosis."*
- **No raw scores for users:** Users only see gentle summaries. Full analytical data is visible only to admins.

---

## Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_USER` | PostgreSQL user | `mindmap` |
| `DB_PASSWORD` | PostgreSQL password | `<strong-password>` |
| `JWT_SECRET` | JWT signing secret (64+ chars) | `openssl rand -base64 64` |
| `ADMIN_INITIAL_PASSWORD` | Default admin password | `<strong-password>` |
| `ENCRYPTION_KEY` | AES-256 key for at-rest encryption | `openssl rand -hex 32` |

---

## Stopping & Cleaning Up

```bash
# Stop all containers
docker compose down

# Stop and remove volumes (deletes all data)
docker compose down -v

# Remove built images
docker compose down --rmi all -v
```

---

## Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Backend Framework | Spring Boot 3.2.x |
| API Gateway | Spring Cloud Gateway |
| Service Discovery | Netflix Eureka |
| Database | PostgreSQL 16 |
| Cache & Bloom Filter | Redis Stack (RedisBloom) |
| Message Broker | Apache Kafka (Confluent 7.5) |
| Password Hashing | Argon2id (Bouncy Castle) |
| Auth Tokens | JWT (jjwt 0.12.x) |
| Encryption at Rest | AES-256-GCM |
| Frontend | Angular 17 (standalone components) |
| UI Library | Angular Material |
| Containerization | Docker + Docker Compose |

---

## License

This project is for educational and self-awareness purposes only. Not intended for clinical use.
