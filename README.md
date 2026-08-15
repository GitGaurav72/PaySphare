# PaySphere

Employee Salary Management System for an HR Manager to manage salary data for ~10,000
employees across multiple countries and answer questions about how the org pays people.

See [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) for the one-page requirements document
(goal, scope, and what was deliberately left out).

Both the backend and the Angular frontend are built (see [Status](#status) below).

## Tech Stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Data JPA, Spring Security, JWT (JJWT),
  PostgreSQL, Flyway, Maven
- **Frontend:** Angular 19 (standalone components), Angular Material, Chart.js (ng2-charts)
- **Testing:** JUnit 5, Mockito, MockMvc, H2 (in-memory, for fast deterministic tests)

## Architecture

```
Angular 19 SPA (src/paysphere-frontend)
    |
    | REST + JWT (Bearer token)
    v
Spring Security  ──►  JwtAuthenticationFilter ──► SecurityContext ──► @PreAuthorize (RBAC)
    |
    v
Controllers  ──►  DTOs (records)  ──►  Services  ──►  Repositories  ──►  PostgreSQL
```

Package layout (`com.PaySphere`): `entity`, `repository`, `dto/{auth,employee,salary,
dashboard,hruser,common}`, `security`, `service` + `service/impl`, `mapper`,
`specification`, `exception`, `controller`, `config`, `seed`.

## Getting Started

### Prerequisites

- JDK 17
- Maven (or use the bundled `./mvnw`)
- PostgreSQL 14+ running locally (or reachable over the network)

### 1. Create the database

```sql
CREATE DATABASE paysphere;
```

### 2. Configure environment variables

| Variable | Purpose | Default (dev profile) |
|---|---|---|
| `DB_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/paysphere` |
| `DB_USERNAME` | DB user | `postgres` |
| `DB_PASSWORD` | DB password | `postgres` |
| `JWT_SECRET` | HMAC signing key, **must be ≥ 32 bytes** | dev-only fallback (never use in prod) |
| `JWT_EXPIRATION` | Access token lifetime, ms | `3600000` (1 hour) |
| `FRONTEND_URL` | Allowed CORS origin | `http://localhost:4200` |
| `SEED_ENABLED` | Seed ~10,000 demo employees on startup | `false` |
| `SEED_EMPLOYEE_COUNT` | How many employees to seed | `10000` |
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` | `dev` |

`prod` falls back to the same `localhost` defaults as `dev` for local convenience, but a
real deployment should always set `JWT_SECRET`, `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`
explicitly via environment variables rather than relying on those fallbacks.

### 3. Run it

```bash
cd src/PaySphere
./mvnw spring-boot:run
```

Flyway runs the schema migration (`db/migration/V1__create_initial_schema.sql`)
automatically on startup, seeding master data (countries, departments, designations).

### 4. Seed ~10,000 demo employees (optional)

```bash
SEED_ENABLED=true ./mvnw spring-boot:run
```

This is idempotent — it's a no-op if the `employees` table already has rows — so it's
safe to leave the flag on. It also creates one demo HR user per role:

| Email | Password | Role |
|---|---|---|
| `admin@paysphere.com` | `Password@123` | HR_ADMIN |
| `manager@paysphere.com` | `Password@123` | HR_MANAGER |
| `viewer@paysphere.com` | `Password@123` | HR_VIEWER |

### 5. Explore the API

Swagger UI: `http://localhost:8080/swagger-ui.html`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@paysphere.com","password":"Password@123"}'
```

Use the returned `accessToken` as `Authorization: Bearer <token>` on subsequent requests.

### 6. Run the frontend

```bash
cd src/paysphere-frontend
npm install
npm start   # ng serve, http://localhost:4200
```

The dev server proxies nothing special — `environment.development.ts` points straight at
`http://localhost:8082/api`, matching the backend's CORS allowance for `FRONTEND_URL`
(default `http://localhost:4200`). Sign in with any of the seeded demo accounts above.

## API Overview

| Resource | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` (public) |
| Employees | `GET/POST /api/employees`, `GET/PUT /api/employees/{id}`, `PATCH /api/employees/{id}/status` |
| Salary | `GET /api/employees/{id}/salary`, `GET /api/employees/{id}/salary-history`, `POST /api/employees/{id}/salary` |
| Dashboard | `GET /api/dashboard/{summary, salary-by-department, salary-by-country, employee-count-by-department, salary-distribution, top-paid-employees}` |
| HR users (admin only) | `GET/POST /api/hr-users`, `PUT /api/hr-users/{id}` |
| Master data | `GET /api/countries`, `GET /api/departments`, `GET /api/designations` |

Full request/response schemas are in Swagger UI.

## RBAC

| Action | HR_ADMIN | HR_MANAGER | HR_VIEWER |
|---|---|---|---|
| View employees / salaries / dashboard | ✅ | ✅ | ✅ |
| Create / update employees, change status | ✅ | ✅ | ❌ |
| Create salary changes | ✅ | ✅ | ❌ |
| Manage HR users | ✅ | ❌ | ❌ |

Enforced with `@PreAuthorize` at the service/controller boundary (not just hidden in the
UI). Unauthenticated requests get `401`; authenticated-but-forbidden requests get `403`
(via a custom `AuthenticationEntryPoint`/`AccessDeniedHandler` — Spring Security's default
returns 403 for both cases, which the test suite caught).

## Key Design Decisions

- **Salary history is append-only.** Creating a new salary record closes the previous
  current record (`effective_to = new.effective_from - 1 day`) inside one `@Transactional`
  method rather than editing history in place.
- **Concurrency:** the employee row is pessimistically locked (`SELECT ... FOR UPDATE`,
  via `EmployeeRepository.findByIdForUpdate`) for the duration of a salary-change
  transaction. This serializes concurrent salary changes *per employee* — the simplest
  reliable way to guarantee at most one current (`effective_to IS NULL`) salary record
  exists, without locking the whole table or requiring optimistic-retry logic on the
  client.
- **Multi-currency:** salaries are never blended across currencies without saying so. The
  dashboard summary reports the org's single largest-headcount currency and labels it;
  per-department/per-country breakdowns are grouped by currency. See
  [`docs/REQUIREMENTS.md`](docs/REQUIREMENTS.md) for the full reasoning.
- **N+1 avoidance:** the employee search specification only fetch-joins
  country/department/designation on the actual data query — not the `COUNT(*)` query
  Spring Data issues alongside it for pagination, since `FETCH` + `COUNT` is invalid JPQL.
- **Testing without infrastructure:** tests run against H2 in-memory (schema generated
  from entities, Flyway disabled) rather than requiring PostgreSQL or Docker, so `mvn test`
  is fast and works in any environment. Integration tests log in through the real
  `/api/auth/login` endpoint rather than mocking Spring Security, so the JWT
  issue/validate path and RBAC rules are actually exercised.

## Running Tests

```bash
cd src/PaySphere
./mvnw test
```

35 tests: unit tests (Mockito) for `AuthenticationService`, `EmployeeService`, and
`SalaryService` — including the salary-history transition and its validation — plus
MockMvc integration tests covering employee CRUD/search/pagination, the end-to-end salary
flow, and RBAC across all three roles (401 unauthenticated, 403 wrong role, 200/201
correct role).

## Status

- [x] Backend: auth, RBAC, employee APIs, salary APIs (transactional, concurrency-safe),
      dashboard analytics, validation, global exception handling, seed data, tests
- [x] Frontend: Angular 19 + Angular Material — login, RBAC-aware dashboard (charts +
      stat cards), employee search/list/detail/create/edit, salary history + salary
      change form, HR user admin screen
- [ ] Deployment
