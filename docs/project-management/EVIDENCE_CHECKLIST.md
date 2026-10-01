# Evidence checklist

Screenshots and evidence needed for the academic report. **No screenshot is
fabricated here.** Items are marked `AVAILABLE` only where a real artefact
exists that can be captured or is already machine-generated.

Legend: **AVAILABLE** — captureable now from a working system. **PARTIAL** —
machine-generated data exists but no visual capture. **NOT AVAILABLE** — cannot
be produced, with the reason stated.

---

## 1. Architecture and structure

| # | Item | Status | How to produce |
|---|---|---|---|
| 1.1 | Component architecture diagram | **AVAILABLE** | Draw from `docs/architecture.md`. Modules are in 16 `com.finai.*` packages |
| 1.2 | Repository structure | **AVAILABLE** | `admin-web/src`, `backend/src/main/java/com/finai`, `android/app/src/main/java/com/finai/mobile`, `docs/`, `tests/`, `scripts/` |
| 1.3 | Module dependency view | **AVAILABLE** | Package listing with responsibilities from `architecture.md` |
| 1.4 | Request lifecycle | **AVAILABLE** | `JwtAuthenticationFilter` → controller → service → repository → `PageResponse`/`ApiError` |

## 2. Database

| # | Item | Status | How to produce |
|---|---|---|---|
| 2.1 | ER diagram | **AVAILABLE** | 10 domain tables + `flyway_schema_history`. See `architecture.md` data model |
| 2.2 | Migration history | **PARTIAL** — already machine-generated | `docker compose exec -T postgres psql -U finai -d finai -c "select version, description, success from flyway_schema_history order by installed_rank;"` |
| 2.3 | Table list with row counts | **AVAILABLE** | `information_schema.tables` query |
| 2.4 | Sample data | **AVAILABLE** | 2 roles, 14 seed categories, 1 admin, plus E2E-created data |

## 3. Backend evidence

| # | Item | Status | How to produce |
|---|---|---|---|
| 3.1 | Swagger UI with all endpoints | **AVAILABLE** | <http://localhost:8081/swagger-ui.html> — 39 paths |
| 3.2 | OpenAPI document | **PARTIAL** | <http://localhost:8081/v3/api-docs> |
| 3.3 | Startup log showing migrations | **AVAILABLE** | `docker compose logs backend` |
| 3.4 | Error envelope example | **AVAILABLE** | Trigger a validation failure; see `docs/api-reference.md` |

## 4. Android client

| # | Item | Status | How to produce |
|---|---|---|---|
| 4.1 | Login screen | **AVAILABLE** | Emulator, `app-debug.apk` |
| 4.2 | Registration | **AVAILABLE** | Same session |
| 4.3 | Dashboard | **AVAILABLE** | After adding transactions |
| 4.4 | Transaction list | **AVAILABLE** | Transactions tab |
| 4.5 | Add/edit transaction | **AVAILABLE** | FAB on the transactions screen |
| 4.6 | Budgets with usage and status | **AVAILABLE** | Budgets tab; make one EXCEEDED first |
| 4.7 | **AI chat conversation** | **AVAILABLE** | Assistant tab — the most valuable screenshot in the report |
| 4.8 | AI refusal | **AVAILABLE** | Ask an off-topic question |
| 4.9 | Notifications | **PARTIAL** | Rendered inside Profile, not a dedicated screen |
| 4.10 | Profile | **AVAILABLE** | Profile tab |
| 4.11 | Feedback submission | **NOT AVAILABLE** | No feedback UI exists (discrepancy D-03) |

## 5. Admin console

| # | Item | Status | How to produce |
|---|---|---|---|
| 5.1 | Admin login | **AVAILABLE** | <http://localhost:5173> |
| 5.2 | Admin dashboard with metrics | **AVAILABLE** | After the E2E run populates data |
| 5.3 | User management list | **AVAILABLE** | Users page with search |
| 5.4 | User locked state | **AVAILABLE** | Lock a user |
| 5.5 | Category management | **AVAILABLE** | Categories page |
| 5.6 | Feedback triage with admin reply | **AVAILABLE** | Resolve a ticket |
| 5.7 | **Audit trail** | **AVAILABLE** | Audit page — strong evidence for the admin story |
| 5.8 | System metrics | **PARTIAL** | Shown inside the dashboard |

## 6. Docker deployment

| # | Item | Status | How to produce |
|---|---|---|---|
| 6.1 | `docker compose ps` with 3 healthy | **PARTIAL** — already machine-generated | Capture terminal output |
| 6.2 | `docker images` | **PARTIAL** | Capture terminal output |
| 6.3 | Container logs, error-free | **PARTIAL** | Capture terminal output |
| 6.4 | Dockerfile contents | **PARTIAL** | `backend/Dockerfile`, `admin-web/Dockerfile` |
| 6.5 | Compose file | **PARTIAL** | `docker-compose.yml`, 3 services |

## 7. Test evidence — strongest quantitative material

| # | Item | Status | How to produce |
|---|---|---|---|
| 7.1 | `mvn verify` output | **PARTIAL** — already generated | 98 unit + 15 integration, BUILD SUCCESS |
| 7.2 | Surefire/Failsafe reports | **PARTIAL** | `backend/target/*-reports/` |
| 7.3 | E2E suite output | **PARTIAL** — already generated | 160 passed, 0 failed |
| 7.4 | **E2E CSV result file** | **AVAILABLE** | `tests/results/e2e-20261001-144857.csv` |
| 7.5 | **Contract suite output** | **PARTIAL** — already generated | 102 passed, 0 failed |
| 7.6 | Contract CSV result file | **AVAILABLE** | `tests/results/admin-web-contract-20261001-144700.csv` |
| 7.7 | Android test results | **PARTIAL** | `TEST-com.finai.mobile.FormatTest.xml`, 10 tests |
| 7.8 | Console build output | **PARTIAL** | 33 modules, 203.39 kB bundle |
| 7.9 | `npm audit` output | **PARTIAL** | 0 vulnerabilities |
| 7.10 | Repeated-run history | **PARTIAL** | 8 E2E CSVs, 6 contract CSVs in `tests/results/` |

## 8. Security evidence

| # | Item | Status | How to produce |
|---|---|---|---|
| 8.1 | `.gitignore` secrets block | **PARTIAL** | First section of the file |
| 8.2 | `.env.example` with placeholders | **PARTIAL** | No real values |
| 8.3 | BCrypt cost 12 in code | **PARTIAL** | `SecurityConfig.passwordEncoder()` |
| 8.4 | Admin route guard in code | **PARTIAL** | `SecurityConfig` matcher |
| 8.5 | 403 shown for non-admin | **AVAILABLE** | Call an admin endpoint with a user token |
| 8.6 | 404 shown for another user's data | **AVAILABLE** | E2E cross-user assertions |
| 8.7 | Password stored as hash | **PARTIAL** | Query `users.password_hash` |
| 8.8 | Security audit findings table | **AVAILABLE** | `docs/security-audit.md` |

## 9. Defect evidence

| # | Item | Status | How to produce |
|---|---|---|---|
| 9.1 | `AiAnalyst` defect before/after | **PARTIAL** | Documented; the code diff is the evidence |
| 9.2 | Proof the regression test detects it | **PARTIAL** | 4 observed failures, then fix restored |
| 9.3 | `Format.kt` duplicate `formatInstant` | **PARTIAL** | Removed; 2 failing assertions documented |
| 9.4 | Healthcheck defect before/after | **AVAILABLE** | `FailingStreak: 22` → `healthy`, streak 0 |
| 9.5 | Container health comparison | **AVAILABLE** | Reproducible in 5 minutes |

## 10. Project management evidence

| # | Item | Status | How to produce |
|---|---|---|---|
| 10.1 | Baseline record | **AVAILABLE** | `docs/project-management/BASELINE.md` |
| 10.2 | Requirement traceability | **AVAILABLE** | 164 rows, 147 verified |
| 10.3 | Quality plan | **AVAILABLE** | `docs/project-management/QUALITY_PLAN.md` |
| 10.4 | Risk register | **AVAILABLE** | 17 risks, scored, with occurrences |
| 10.5 | Metrics | **AVAILABLE** | Measured / derived / unavailable, clearly separated |
| 10.6 | Configuration baseline | **AVAILABLE** | Includes the missing-repository finding |
| 10.7 | Test evidence | **AVAILABLE** | Every suite with command, date, environment |
| 10.8 | **Git history** | **NOT AVAILABLE** | No repository exists. Do not fabricate a graph |
| 10.9 | **Burndown or Gantt chart** | **NOT AVAILABLE** | No schedule or effort data exists |
| 10.10 | **Sprint artefacts** | **NOT AVAILABLE** | No Agile process was used |

## 11. Screenshots still to be captured

A checklist for the person assembling the report. All are achievable in one
session with the stack running.

- [ ] Admin login
- [ ] Admin dashboard
- [ ] Users list with a locked user
- [ ] Categories management
- [ ] Feedback triage
- [ ] Audit trail
- [ ] Android login
- [ ] Android dashboard with populated figures
- [ ] Android transactions list
- [ ] Android budgets showing EXCEEDED
- [ ] **Android AI answer with FACT/SUGGESTION sections**
- [ ] **Android AI refusal for an off-topic question**
- [ ] `docker compose ps` showing 3 healthy
- [ ] `mvn verify` BUILD SUCCESS
- [ ] E2E suite summary (160/0)
- [ ] Contract suite summary (102/0)
- [ ] Flyway migration history query
- [ ] `docs/known-limitations.md` — proves gaps are disclosed

## 12. Items that must NOT appear in the report

- A Git commit graph, branch diagram or contribution chart
- A Gantt chart, burndown or sprint velocity figure
- Any statement of total effort in hours or days
- Any statement that the project took N weeks
- A code coverage percentage — no coverage tooling is configured
- A performance, p95 latency or throughput figure
- A claim of production readiness
- A description of an Agile process that was not used

Each is listed in
[`project-management/EFFORT_DATA_GAP.md`](project-management/EFFORT_DATA_GAP.md)
with the reason it cannot be supplied.