# Personal Finance AI System

A personal finance tracker with a grounded AI assistant. Users record income and
expenses, set budgets, and ask questions about their own data. Administrators get a
separate console for account management, the category catalogue, feedback triage
and the audit trail.

The defining constraint of this project is that **the assistant never invents a
number**. Every figure it reports is a parameterised aggregate over the
authenticated user's own rows, and the answer is always split into verified facts
and judgement so the two can never be confused.

## Repository

```bash
git clone https://github.com/HoangKhai1612/expense_management.git
cd expense_management
```

| | |
|---|---|
| Branch | `main` |
| Verified baseline | `59cc2a0` — `chore: establish verified academic project baseline` |
| Release tag | `v1.0.0-academic-final` |

> **History notice.** Version control was established **after** the main
> development period. The baseline commit does **not** represent the complete
> development process, and historical development provenance is
> `UNKNOWN / NOT RECORDED` — no development-period commits, authorship or
> schedule exist or can be reconstructed. This notice is also recorded in the
> baseline commit message.

## Components

| Component | Stack | Port | Purpose |
|---|---|---|---|
| `backend/` | Java 21, Spring Boot 3.5, PostgreSQL 16 | 8081 | REST API, business rules, AI analyst |
| `admin-web/` | React 18, TypeScript, Vite | 5173 | Administrator console |
| `android/` | Kotlin, Jetpack Compose | – | Android client |
| `postgres/` | PostgreSQL 16 (Docker) | 5433 | System of record |

## Quick start

```bash
cp .env.example .env       # then edit the secrets; JWT_SECRET must be >= 32 bytes
docker compose up -d --build
```

That starts PostgreSQL, the backend and the admin console. The API is then on
<http://localhost:8081>, the console on <http://localhost:5173>.

The bootstrap administrator is created from `APP_ADMIN_EMAIL` and
`APP_ADMIN_PASSWORD`. If those are unset the application still starts; it simply
has no administrator, which is logged rather than papered over with a default
password.

> No password hash is stored in any migration file. The credentials come from the
> environment only.

### OpenAPI

- Swagger UI: <http://localhost:8081/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8081/v3/api-docs>

## Running the backend on the host

Faster iteration than a container rebuild:

```bash
docker compose up -d postgres
./scripts/run-backend-local.ps1
```

Output is teed to `logs/backend.log`. Host ports are offset from the defaults
(5433 / 8081 / 5173) so the stack can run beside an existing local PostgreSQL.

## Verification

Every claim in this repository is backed by a command that was actually executed.
Run them in this order:

```bash
# 1. Backend unit tests (98) - no Docker required
cd backend && mvn test

# 2. Backend integration tests (15) - Testcontainers, needs Docker
mvn verify

# 3. End-to-end API suite (160 assertions) - needs the backend running
./tests/e2e-api-tests.ps1

# 4. Admin Web API contract (102 assertions) - needs the backend running
./tests/admin-web-contract.ps1

# 5. Admin Web typecheck and bundle
cd admin-web && npm run build

# 6. Android unit tests (10) and debug APK
# The Gradle daemon JVM is pinned to 25 - see docs/testing.md
cd android && ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

`mvn verify` runs unit tests under Surefire and integration tests under
Failsafe, so a machine without Docker can still run step 1 on its own.

Results are written to `tests/results/*.csv`.

## Design decisions worth knowing

**The AI has no text-to-SQL path.** `AiDataSnapshotService` builds a typed
snapshot of the caller's rows and the analyst reasons only over that. This removes
the injection and accidental-write risks of generating queries, and it makes the
user id a required argument rather than something parsed out of the question, so
the assistant cannot be steered toward another account.

**Every numeric claim is persisted with the message.** Each fact the assistant
emits is written next to the answer, which gives an audit trail linking what the
user was told to what the database actually held.

**Deleting a category is not an option.** Categories are deactivated. A hard
delete would either break the foreign key from `transactions` or force a decision
about other users' history.

**Locking a user invalidates their tokens immediately.** The filter re-checks
account status on every request, so a pre-lock token stops working without waiting
for expiry.

**Runtime metrics are labelled as process-scoped.** The admin dashboard exposes
`uptimeSinceRestart` so counters that reset on restart cannot be mistaken for
all-time figures.

## Configuration

All settings come from the environment; see `.env.example` for the full list. The
ones most likely to need changing:

| Variable | Purpose | Default |
|---|---|---|
| `JWT_SECRET` | HMAC signing key, >= 32 bytes | none (required) |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | Database | `localhost:5433`, `finai` |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | Bootstrap administrator | none |
| `AI_PROVIDER` | `LOCAL` uses the built-in analyst; anything else needs an API key | `LOCAL` |
| `BUDGET_WARNING_THRESHOLD` / `BUDGET_EXCEEDED_THRESHOLD` | Alert thresholds | `80` / `100` |
| `CORS_ALLOWED_ORIGINS` | Allowed admin origins | localhost dev ports |

## Further reading

- [`docs/architecture.md`](docs/architecture.md) - module layout and data model
- [`docs/api-reference.md`](docs/api-reference.md) - endpoint catalogue
- [`docs/testing.md`](docs/testing.md) - test strategy and how to run it
- [`docs/deployment.md`](docs/deployment.md) - build and deploy
- [`docs/acceptance-audit.md`](docs/acceptance-audit.md) - what is verified and what is not
- [`docs/known-limitations.md`](docs/known-limitations.md) - open gaps

### Project management

- [`docs/project-management/BASELINE.md`](docs/project-management/BASELINE.md) - verified state at commit `59cc2a0`
- [`docs/project-management/REQUIREMENT_TRACEABILITY.md`](docs/project-management/REQUIREMENT_TRACEABILITY.md) - 164 requirements traced to evidence
- [`docs/project-management/QUALITY_PLAN.md`](docs/project-management/QUALITY_PLAN.md) - requirement → criterion → test → result
- [`docs/project-management/RISK_AUDIT.md`](docs/project-management/RISK_AUDIT.md) - scored risks, each verified against evidence
- [`docs/project-management/METRICS.md`](docs/project-management/METRICS.md) - measured vs derived vs unavailable
- [`docs/project-management/PROJECT_CLOSURE.md`](docs/project-management/PROJECT_CLOSURE.md) - closure decision and lessons learned
- [`docs/project-management/FINAL_PROJECT_STATUS.md`](docs/project-management/FINAL_PROJECT_STATUS.md) - final gate
- [`docs/project-management/EFFORT_DATA_GAP.md`](docs/project-management/EFFORT_DATA_GAP.md) - what history cannot be reconstructed
- [`docs/SECRET_SCAN_REPORT.md`](docs/SECRET_SCAN_REPORT.md) - pre-commit secret scan
- [`docs/LOGBACK_SECURITY_REVIEW.md`](docs/LOGBACK_SECURITY_REVIEW.md) - logging security audit