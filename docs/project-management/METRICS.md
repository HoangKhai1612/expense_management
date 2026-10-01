# Metrics

Every number below is classified by how it was obtained. Nothing here is
estimated, projected or rounded up for effect.

- **MEASURED** — read directly from a tool, report or query. Reproducible.
- **DERIVED** — computed from measured values by arithmetic stated inline.
- **UNAVAILABLE** — cannot be obtained from the project. Not estimated.

---

## 1. Test metrics — MEASURED

### Backend unit tests (Surefire)

| Class | Tests | Failures | Errors |
|---|---|---|---|
| `AiAnalystTest` | 9 | 0 | 0 |
| `AiTextTest` | 24 | 0 | 0 |
| `AuthServiceTest` | 11 | 0 | 0 |
| `BudgetServiceTest` | 15 | 0 | 0 |
| `CategoryServiceTest` | 13 | 0 | 0 |
| `JwtServiceTest` | 7 | 0 | 0 |
| `TransactionServiceTest` | 19 | 0 | 0 |
| **Total** | **98** | **0** | **0** |

Source: `backend/target/surefire-reports/`, final audit run.

### Backend integration tests (Failsafe)

| Class | Tests | Failures | Errors |
|---|---|---|---|
| `AiGroundingIT` | 8 | 0 | 0 |
| `MigrationsAndBootstrapIT` | 7 | 0 | 0 |
| **Total** | **15** | **0** | **0** |

Source: `backend/target/failsafe-reports/`, final audit run.

### End-to-end API suite

| Metric | Value |
|---|---|
| Assertions executed | 160 |
| Passed | 160 |
| Failed | 0 |
| Pass rate | **100%** (DERIVED: 160 ÷ 160) |

Source: `tests/results/e2e-20261001-144857.csv`.

### Admin contract suite

| Metric | Value |
|---|---|
| Assertions executed | 102 |
| Passed | 102 |
| Failed | 0 |
| Pass rate | **100%** (DERIVED: 102 ÷ 102) |

Source: `tests/results/admin-web-contract-20261001-144700.csv`.

### Android unit tests

| Metric | Value |
|---|---|
| Tests | 10 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

Source: `android/app/build/test-results/testDebugUnitTest/TEST-com.finai.mobile.FormatTest.xml`.

## 2. Aggregate test metrics — DERIVED

| Metric | Value | Arithmetic |
|---|---|---|
| Total automated test cases | **123** | 98 + 15 + 10 |
| Total HTTP assertions | **262** | 160 + 102 |
| Grand total checks | **385** | 123 + 262 |
| Failures across everything | **0** | Sum of all suites |
| Backend pass rate | 100% | 113 ÷ 113 |
| Android pass rate | 100% | 10 ÷ 10 |
| HTTP assertion pass rate | 100% | 262 ÷ 262 |

Note: the E2E and contract suites are *assertions*, not JUnit cases, so the two
counts are not directly comparable. Both are listed to avoid conflating them.

## 3. Suite execution history — MEASURED

From the CSV filenames in `tests/results/`, showing repeatability:

| Suite | Runs recorded | Distinct result dates |
|---|---|---|
| E2E API | 8 | 2026-09-30 (×4), 2026-10-01 (×4) |
| Admin contract | 6 | 2026-10-01 |

The E2E suite passed on every recorded run, against both a host-run backend and
a containerised backend, and once against a stack rebuilt from scratch. That
demonstrates idempotency rather than a single lucky pass.

## 4. Build metrics — MEASURED

| Metric | Value | Source |
|---|---|---|
| Backend build | BUILD SUCCESS | `mvn verify` |
| Console typecheck | No errors | `tsc --noEmit` |
| Console bundle | 203.39 kB JS, 4.80 kB CSS, 65.37 kB gzipped JS | Vite output |
| Modules transformed | 33 | Vite output |
| npm vulnerabilities | 0 | `npm audit` |
| Android build | BUILD SUCCESSFUL | Gradle |
| Debug APK | 20,318,592 bytes | Filesystem |
| Images built | 3 | `docker compose build` |

## 5. Deployment metrics — MEASURED

| Metric | Value | Source |
|---|---|---|
| Containers healthy | 3 of 3 | `docker compose ps` |
| PostgreSQL | healthy | Compose |
| Backend | healthy | Compose |
| Admin console | healthy | Compose |
| Health endpoint | `UP` | `/actuator/health` |
| Admin login via container | Role `ADMIN`, 226-char token | Manual API call |
| Console HTTP status | 200 | `GET localhost:5173` |
| Errors in backend log | 0 | `docker compose logs backend` |
| Migrations applied | 9 of 9, all `success=t` | `flyway_schema_history` |
| Tables created | 11 | `information_schema` |

## 6. Defect metrics — MEASURED

| Metric | Value |
|---|---|
| Defects found and fixed in this project | 3 |
| Of which found during the final audit | 1 (admin-web healthcheck, D-01) |
| Of which found by code review | 1 (`AiAnalyst` immutable list) |
| Of which found by unit test | 1 (`Format.kt` duplicate `formatInstant`) |
| Defects open at close | 2 (D-03, D-04 — both documentation/scope, no runtime impact) |
| Defects that reached a user-facing failure | 1 (AI spending analysis returned a server error) |

Detection breakdown is meaningful: one defect escaped code review and was
caught by testing, and one escaped testing and was caught by container health
inspection. Neither detection method alone would have found all three.

### Regression proof for each fix

| Defect | Proof the test detects it |
|---|---|
| `AiAnalyst` immutable list | Fix reverted → 4 `UnsupportedOperationException` failures observed → fix restored |
| `Format.kt` duplicate | 2 failing assertions observed on the shadowing implementation → duplicate removed, regex validation added |
| Healthcheck IPv6 | `wget localhost` FAIL vs `wget 127.0.0.1` OK inside the same container; `FailingStreak` 22 → 0 |

## 7. Requirement coverage — MEASURED

Counted programmatically from
[`REQUIREMENT_TRACEABILITY.md`](REQUIREMENT_TRACEABILITY.md):

| Status | Count | Share (DERIVED) |
|---|---|---|
| VERIFIED | 148 | 90.2% |
| PARTIAL | 8 | 4.9% |
| GAP | 7 | 4.3% |
| OUT OF SCOPE | 1 | 0.6% |
| Total | 164 | 100% |

The 7 gaps are all missing *practices* (CI, UI automation, load testing, rate
limiting, TLS, image scanning, external-provider path) rather than missing
specified behaviour. The out-of-scope row is the Android feedback UI, excluded
with a recorded rationale.

## 8. Code volume — MEASURED

| Component | Files | Lines |
|---|---|---|
| Backend main | 78 | 5,500 |
| Backend test | 11 | — |
| Admin console | 14 | 1,635 |
| Android | 22 | 2,668 |
| Database migrations | 9 | — |
| Documentation | 26 `.md` under `docs/` | — |

## 9. Efficiency metrics — MEASURED

| Metric | Value | Source |
|---|---|---|
| Backend tests (no Docker) | ~30 s | `mvn test` |
| `mvn verify` with Testcontainers | ~2 min | Build log |
| Console build | 307 ms | Vite |
| Compose clean rebuild | ~6 min | Observed |
| Clean stack to fully healthy | ~60 s | Observed |
| E2E suite (160 assertions) | ~25 s | Observed |
| Contract suite (102 assertions) | ~7 s | Observed |

## 10. UNAVAILABLE metrics

The following **cannot** be measured and are **not estimated**:

| Metric | Why unavailable |
|---|---|
| Effort in person-hours/days | No version control, no tracker, no time records |
| Project duration | No dated history; only the verification phase (≈15 h) is visible in CSV timestamps |
| Team size, roles | No records |
| Commit count, defect-introducing commits | No Git repository |
| Velocity, burndown, sprint count | No Agile artifacts |
| Code coverage percentage | No JaCoCo or equivalent configured — **not measured, so not claimed anywhere** |
| Mutation score | No mutation testing |
| Requirement count from the original specification | The specification is not stored in the repository; traceability is built from the implemented API surface |
| Defect escape rate to production | No production deployment |
| Performance, p95 latency, throughput | No load test performed |
| Mean time to fix | Only 3 defects, all fixed in-session; not a meaningful sample |
| Uptime, availability | No monitoring or alerting in place |

Two of these deserve emphasis. **Code coverage is not configured**, so no
coverage percentage appears in any document — claiming one would be fabrication.
**The original specification is not in the repository**, so the traceability
matrix traces the implemented API surface rather than a stored requirement list;
where the specification was available to the audit it was quoted as given.