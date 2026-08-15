# AI Tool Usage & Process Notes

## Tool

Claude Code (Sonnet 5), used agentically: reading the repo, writing files, running
`mvnw compile`/`mvnw test` after each phase, and committing incrementally — not a single
prompt-to-code dump.

## Inputs

- [`docs/ai-context/master-prompt.txt`](ai-context/master-prompt.txt) — a detailed master
  context document (48 sections) establishing the tech stack, database schema, package
  structure, and phased implementation order for the backend. Supplied as the working
  context at the start of the session and treated as the source of truth for backend
  architecture.
- The official assessment brief (goal, persona, technical constraints, deliverables).

## Process

1. **Inspected the existing repo before writing anything** — found a bare Spring
   Initializr scaffold (Spring Boot 4.1.0, `war` packaging, only `webmvc`/`tomcat`
   dependencies) and nine entity files that were committed but genuinely empty (0 bytes),
   despite a commit message claiming "added all entities." Verified this with `git log
   --stat` before treating it as a from-scratch build rather than assuming missing content
   was a read error.
2. **Built the backend in phases**, compiling and running the growing test suite after
   each one, with a commit per phase (project bootstrap → repositories/DTOs → security/
   services/controllers → seed data/OpenAPI/tests) rather than one large commit.
3. **The RBAC test suite caught a real bug**: unauthenticated requests were returning 403
   instead of 401, because Spring Security's default entry point
   (`Http403ForbiddenEntryPoint`) doesn't distinguish "not authenticated" from
   "authenticated but forbidden" unless you configure one explicitly. Fixed with a custom
   `AuthenticationEntryPoint`/`AccessDeniedHandler` pair — this is called out because it's
   a case where writing the test *before* declaring the work done surfaced something a
   manual smoke test likely would have missed.

## Deviations from the master prompt (and why)

- **Spring Boot version**: master prompt didn't pin a version; the existing scaffold had
  `4.1.0` (not a real released line as of this writing) with `war` packaging and an
  external-container setup. Switched to Spring Boot **3.2.5** (jar packaging, embedded
  Tomcat) — matches the "Spring Boot 3.x" direction and removes deployment complexity
  (`ServletInitializer`, provided-scope Tomcat) the project doesn't need.
- **Flyway migration location**: the existing `V1__create_initial_schema.sql` lived under
  `scipts/`, not on the classpath, so Flyway would never have picked it up. Moved (not
  edited) it to `src/main/resources/db/migration/`.
- **Entity package location**: the empty stub entities lived under
  `com.PaySphere.employee.entity` (package-by-feature); the master prompt's own suggested
  structure (§3) lists a flat `entity/` package. Since the stubs held no content, moved
  them to the documented flat location rather than keeping two conflicting conventions.
- **Frontend framework**: the master prompt says Angular; the official assessment brief
  explicitly requires React or Next.js as a technical constraint. Flagged this conflict
  and asked — the user chose to keep Angular, so the README and requirements doc reflect
  that as an intentional deviation from the assessment's stated constraint, not an
  oversight.
- **Dashboard summary compensation figures**: the master prompt's example response (§21)
  shows one blended `averageSalary`/`highestSalary`/`lowestSalary`, but §22 of the same
  document says not to treat different currencies as comparable — with employees spread
  across 8 currencies, blending them numerically would be actively misleading. Resolved by
  scoping the summary's compensation fields to the organization's single largest-headcount
  currency (labeled via `primaryCurrencyCode` in the response) while the per-department/
  per-country breakdown endpoints remain fully currency-disaggregated. See
  `docs/REQUIREMENTS.md` for the longer version of this reasoning.

## What wasn't verified

Live end-to-end verification against a real PostgreSQL instance (Flyway migration run +
10,000-employee seed) was intentionally skipped in this session — a local PostgreSQL 18
service was found running, but connecting required a password that wasn't provided, and
the user chose to skip live verification rather than share it. The full test suite (35
tests, unit + MockMvc integration) runs against H2 and passes; the setup steps for running
against real PostgreSQL are documented in the main README and have not been independently
re-run end-to-end in this session.
