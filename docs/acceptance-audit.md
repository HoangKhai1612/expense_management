# Acceptance audit

Every row below states the command that was run and what it produced. Nothing is
listed as verified on the strength of having written the code.

Last full run: 2026-10-01.

## Verified by execution

| Requirement | Evidence | Result |
|---|---|---|
| Spring Boot 3.5 on Java 21 | `mvn verify` | BUILD SUCCESS |
| 98 backend unit tests | `mvn test`, Surefire reports | 98 run, 0 failures, 0 errors |
| 15 backend integration tests | `mvn verify`, Failsafe reports | 15 run, 0 failures, 0 errors |
| All 9 Flyway migrations apply | `MigrationsAndBootstrapIT` | Pass |
| 11 tables created | `MigrationsAndBootstrapIT` | Pass |
| 2 roles, 14 categories, 1 admin bootstrapped | `MigrationsAndBootstrapIT` | Pass |
| Passwords stored hashed, never plaintext | `MigrationsAndBootstrapIT` | Pass |
| 160 end-to-end API assertions | `tests/e2e-api-tests.ps1` | 160/160 passed, twice consecutively |
| 102 admin console contract assertions | `tests/admin-web-contract.ps1` | 102/102 passed |
| Console typechecks and bundles | `npm run build` | Succeeds |
| No known npm vulnerabilities | `npm audit` | 0 vulnerabilities |
| Backend serves health | `GET /actuator/health` | `{"status":"UP","groups":["liveness","readiness"]}` |
| AI reports only grounded figures | `AiGroundingIT` + `AiAnalystTest` | Pass |
| Compose stack builds and runs | `docker compose build && docker compose up -d` | 3 images built; all 3 containers healthy |
| Containerised backend serves requests | `GET /actuator/health`, admin login | `UP`; login returns an ADMIN token |
| Admin console served from its image | `GET http://localhost:5173` | 200 |
| E2E and contract suites pass against the container | Both PowerShell suites, container backend | 160/160 and 102/102 |
| Android 10 unit tests and debug APK | `./gradlew cleanTestUnitTest testDebugUnitTest assembleDebug` | 10 run, 0 failures; APK 20.3 MB |

## Requirements verified by construction

These are implemented and reviewed but have no automated assertion behind them.
Treat them as lower-confidence than the table above.

| Requirement | Where | Why not asserted |
|---|---|---|
| Access control — no cross-user data leakage | Service-level owner filters, enforced in every query | Covered indirectly by E2E 404/403 cases, not exhaustively for every endpoint |
| Deactivate-not-delete on categories | `CategoryService` | Asserted in E2E for the admin path |
| Budget thresholds fire one notification only | `BudgetService` notification guard | No test submitting two over-budget transactions and asserting a single alert |
| JWT invalidated by account lock | `JwtAuthenticationFilter` re-checks status each request | E2E: "Pre-lock token is rejected after locking" asserts 401 |
| Admin self-modification refused and audited | `AdminUserController` / audit service | Covered in E2E for the 403; the `ADMIN_DENIED` audit row is asserted |
| Deactivating a category preserves history | Soft-delete design | Asserted in E2E |
| AI declines rather than estimates when ungrounded | `AiAnalyst` | Asserted in `AiAnalystTest` and `AiGroundingIT` |

## Not delivered

Listed explicitly so these are not mistaken for finished work.

| Item | Status | Reason |
|---|---|---|
| UI test automation | Not started | No Selenium/Espresso suite. UIs verified by typecheck, build and manual walkthrough |
| Load and performance testing | Not started | No evidence about behaviour under concurrency |
| Security scanning / penetration test | Not started | `npm audit` only |
| CI pipeline | Not started | No GitHub Actions or equivalent; all verification is manual |
| Kubernetes manifests | Not started | Compose only, which is not an orchestrator |
| Logging redaction review | Not reviewed | Appender uses the Spring Boot default pattern |
| TLS termination | Not implemented | Service speaks plain HTTP |
| Backup and restore automation | Not implemented | Named volume has no backup story |
| Android release signing | Not configured | Debug APK only |
| Multi-tenant scaling | Not applicable | Single-instance design; no sharding or partitioning |

## Defect found and fixed

`AiAnalyst.spending()` built its fact list with `List.of(...)`, which is
immutable, then appended a computed average. Every spending-analysis call threw
`UnsupportedOperationException` — the most-used AI intent was broken in
production code while the feature looked complete.

Fixed by wrapping the initial list in `new ArrayList<>(...)`.

The regression test was confirmed to catch the original defect: the fix was
temporarily reverted, the suite produced 4 `UnsupportedOperationException`
failures, and the fix was restored. A test that has never been shown to fail is
not evidence of anything.

## Traceability

| Specification area | Implementation | Tests |
|---|---|---|
| Authentication | `com.finai.auth`, `com.finai.security` | `AuthServiceTest`, `JwtServiceTest`, E2E |
| Users and profiles | `com.finai.user` | E2E |
| Categories | `com.finai.category` | `CategoryServiceTest`, E2E |
| Transactions | `com.finai.transaction` | `TransactionServiceTest`, E2E |
| Budgets and alerts | `com.finai.budget`, `com.finai.notification` | `BudgetServiceTest`, E2E |
| Statistics | `com.finai.statistics` | E2E |
| AI assistant | `com.finai.ai` | `AiTextTest`, `AiAnalystTest`, `AiGroundingIT`, E2E |
| Feedback | `com.finai.feedback` | E2E |
| Administration | `com.finai.admin` | E2E, contract suite |
| Audit | `com.finai.audit` | E2E (delta-based counts) |
| Schema | `db/migration` V1–V9 | `MigrationsAndBootstrapIT` |

## Reproducing this audit

```bash
mvn -f backend/pom.xml verify
powershell -ExecutionPolicy Bypass -File tests/e2e-api-tests.ps1
powershell -ExecutionPolicy Bypass -File tests/admin-web-contract.ps1
npm --prefix admin-web run build
cd android; ./gradlew testDebugUnitTest assembleDebug
```

CSV output for the two PowerShell suites lands in `tests/results/`. The
administrator console requires the backend to be running.