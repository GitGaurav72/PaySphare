# PaySphere — Requirements Document

## Goal

Give ACME's HR Manager a web-based tool to manage salary data for ~10,000 employees across
multiple countries, replacing spreadsheets, and to let them answer questions about how the
org pays people (by department, by country, by role, top earners, distribution) — with a
full audit trail of every salary change.

## User Persona & Roles

HR Manager is the primary persona. The app enforces three roles server-side (not just
hidden in the UI):

| Role | Can |
|---|---|
| **HR_ADMIN** | Everything HR_MANAGER can, plus manage HR user accounts |
| **HR_MANAGER** | View everything; create/edit employees; change salaries; bulk upload; mark salaries paid |
| **HR_VIEWER** | Read-only — view employees, salaries, dashboard, exports |

Single organization, internal tool, not multi-tenant.

## Scope & Features (delivered)

### Backend (Spring Boot 3, PostgreSQL, Flyway)

- **Auth & RBAC** — stateless JWT login, BCrypt password hashing, method-level
  `@PreAuthorize` per endpoint. Unauthenticated requests get 401, authenticated-but-
  forbidden requests get 403 (a distinct `AuthenticationEntryPoint`/`AccessDeniedHandler`
  pair — Spring Security's default conflates the two).
- **Employee management** — create, update, status changes (Active / Inactive / On Leave
  / Terminated — employees are never physically deleted), get-by-id, and a paginated,
  searchable, filterable listing (name/code/email search; country/department/designation/
  status filters). Filtering, sorting, and pagination all happen at the database level.
- **Salary management** — current salary, full salary history, and a transactional
  "create a new salary record" operation that closes the previous current record instead
  of overwriting it. A pessimistic lock on the employee row prevents two concurrent salary
  changes for the same employee from both creating a "current" record.
- **Salary payment tracking** — each salary record carries a Pending/Paid status.
  Marking a record paid timestamps it and emails the employee a payslip; email delivery
  failures are logged, not thrown — the payment status is the source of truth and must
  not roll back because a notification couldn't be sent.
- **Excel reporting** (Apache POI) — filtered employee list export, per-employee salary
  history export, and on-demand payslip download (the same generator used for the emailed
  attachment), all streamed rather than built fully in memory.
- **Bulk employee upload** — downloadable `.xlsx` template with real in-cell dropdown
  validation for Country/Department/Designation (backed by the actual master data, so a
  typo isn't possible), and an upload endpoint that validates and creates rows
  independently — one bad row is reported with a specific reason without blocking the
  rest of the file.
- **Dashboard analytics** — headcount summary, salary by department, salary by country,
  salary distribution buckets, top-paid employees — all computed with database
  aggregation (`GROUP BY`/`AVG`/`COUNT`), never by loading 10,000 rows into the JVM.
- **Multi-currency handling** — employees are paid in their country's native currency;
  the app never blends currencies into one number without saying so (see below).
- **Seed data** — a generator for ~10,000 realistic employees with country/seniority-
  scaled salaries plus one demo HR user per role, gated behind a flag so it's opt-in and
  idempotent (a no-op if the database already has employees).
- **Tests** — 43 automated tests: Mockito unit tests for the core services (auth,
  employee, salary — including the close-old/open-new salary transition and its
  validation, and the bulk-upload row-by-row validation), plus MockMvc integration tests
  that exercise the real JWT login flow and RBAC (401 vs 403, per-role access) against an
  in-memory H2 database rather than mocking security away.

### Frontend (Angular 19, standalone components, Angular Material)

- **Branded login** — split-screen page: sign-in form plus a hero panel (product pitch,
  feature highlights, scroll-revealed testimonials) that collapses away on mobile.
- **Dashboard** — headcount and compensation stat cards plus four charts (department
  comp, country comp, headcount by department, salary distribution with a currency
  switcher) and a top-paid-employees table, all fed by the backend's aggregation
  endpoints.
- **Employee management** — the same search/filter/paginate the backend exposes, plus
  an Excel export button, create/edit forms mirroring the backend's validation, and a
  dedicated status-change dialog.
- **Bulk upload UI** — a dialog to download the template or upload a filled one, with a
  results summary (created/failed counts) and a table of exactly which rows failed and
  why.
- **Employee detail** — profile, current salary with its payment-status chip, full
  salary history table, a "Mark as paid" action, per-record payslip download, and an
  Excel export of the whole history.
- **HR user administration** — admin-only screen to create/edit HR accounts and roles;
  hidden from other roles in the UI *and* rejected by the backend regardless of what the
  UI shows.
- **Responsive design** — the sidebar collapses into a hamburger-triggered overlay
  drawer below the mobile breakpoint, dialogs cap at 95% viewport size, and tables scroll
  horizontally instead of breaking page layout. Verified at an iPhone SE viewport across
  every page.
- **Amber brand identity** — a consistent flat-amber sidebar/accent theme and a matching
  favicon, applied without sacrificing text contrast/readability.

## Deliberately Left Out (and why)

- **Real-time FX conversion.** Blending INR and USD salaries into one number without
  conversion is misleading, and building a correct FX pipeline (rates, historical rates
  for past salary records, provider dependency) is a project of its own. Instead, every
  aggregate endpoint is explicit about which currency it's reporting, and the dashboard
  summary is scoped to the organization's single largest-headcount currency rather than a
  blended figure. A proper FX strategy is a natural v2 addition if a single reporting
  currency becomes a real requirement.
- **Employee self-service portal.** The persona is the HR Manager, not employees
  themselves; adding employee logins, password resets, and a second permission model
  roughly doubles the auth surface for a persona nobody asked for.
- **Payroll processing / tax / statutory compliance.** Salary *management* (what someone
  is paid, when it changed, and whether that payment has gone out) is a materially
  different, much smaller problem than running actual payroll (tax withholding, statutory
  filings, multi-country compliance). "Mark as paid" is a record-keeping flag, not a
  payment rail — it does not move money.
- **Production email delivery is not configured out of the box.** The payslip email
  *code path* is built and tested (SMTP config, MIME message with attachment, graceful
  failure handling), but no real mail provider credentials ship with this submission —
  `MAIL_HOST`/`MAIL_USERNAME`/`MAIL_PASSWORD` are meant to be supplied via environment
  variables in a real deployment. Marking a salary paid in a fresh environment without
  SMTP configured still works correctly; the email attempt just logs a warning instead of
  sending.
- **Microservices / message queues / caching layer.** 10,000 employees is a small dataset
  for a single well-indexed PostgreSQL instance; a monolith with proper pagination and
  indexes handles this comfortably. Introducing Kafka/Redis/service boundaries here would
  be complexity with no corresponding load to justify it.
- **Configurable per-tenant RBAC / custom roles.** Three fixed roles match the brief
  exactly; a role-builder UI would be speculative generality for a single-org tool.
- **Offline / PWA support.** This is an internal HR tool used at a desk or, per the
  responsive work, occasionally on a phone with a live connection — not a field app that
  needs to work offline.

## Non-Functional Notes

- **Performance:** indexed foreign keys, status, and employee code/email; no unbounded
  `findAll()` on employees; fetch-joins only on the paginated data query (not the count
  query) to avoid N+1 without breaking pagination; Excel exports stream rather than
  building the whole workbook in memory for the unfiltered ~10k case.
- **Concurrency:** two HR staff changing the same employee's salary at once could both
  observe "no current salary row" and each insert one, corrupting the current-salary
  invariant. Mitigated with a pessimistic write lock on the employee row for the duration
  of a salary-change transaction, serializing concurrent changes per employee without
  locking the whole table.
- **Auditability:** salary history is append-only; every change is attributed to the HR
  user who made it (`created_by`) and timestamped; payment status changes are similarly
  timestamped.
- **Security boundary:** every RBAC rule is enforced server-side via `@PreAuthorize`. The
  frontend hiding a button is a UX nicety, never the actual control.
