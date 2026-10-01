# Final test evidence

Every suite below was **re-executed during the final audit on 2026-10-01**, not
inherited from earlier runs. Times are local. The environment is recorded for
each so a reader can tell whether a result is portable.

## Environment

| Component | Value |
|---|---|
| Host OS | Windows (win32), PowerShell 5.1 |
| JDK (backend) | Temurin 21.0.12.1 |
| JDK (Android daemon) | Temurin 25.0.3+9 |
| Maven | 3.9.9 |
| Node.js | 24.11.0 |
| npm | 11.7.0 |
| Gradle | 9.6.0 (daemon JVM pinned to 25) |
| Docker Engine | 29.6.2 |
| PostgreSQL | 16-alpine in container |
| Deployment under test | Docker Compose, containerised backend |

---

## Suite 1 — Backend unit and integration tests

| Field | Value |
|---|---|
| **Command** | `mvn verify` (from `backend/`) |
| **Date** | 2026-10-01, 14:39 local |
| **Environment** | JDK 21, Maven 3.9.9, Docker available for Testcontainers |
| **Expected** | 98 unit + 15 integration tests, 0 failures |
| **Actual** | `Tests run: 98, Failures: 0, Errors: 0, Skipped: 0` and `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`; **BUILD SUCCESS** |
| **Result** | **PASS** |
| **Evidence** | `backend/target/surefire-reports/`, `backend/target/failsafe-reports/` |

Per class:

| Class | Tests | Failures | Errors |
|---|---|---|---|
| `AiAnalystTest` | 9 | 0 | 0 |
| `AiTextTest` | 24 | 0 | 0 |
| `AuthServiceTest` | 11 | 0 | 0 |
| `BudgetServiceTest` | 15 | 0 | 0 |
| `CategoryServiceTest` | 13 | 0 | 0 |
| `JwtServiceTest` | 7 | 0 | 0 |
| `TransactionServiceTest` | 19 | 0 | 0 |
| `AiGroundingIT` | 8 | 0 | 0 |
| `MigrationsAndBootstrapIT` | 7 | 0 | 0 |

Environment sensitivity: the 15 integration tests **require a running Docker
daemon**. Without Docker, run `mvn test` for the 98 unit tests, which need only
a JDK.

---

## Suite 2 — Admin console build and typecheck

| Field | Value |
|---|---|
| **Command** | `npm run build` (from `admin-web/`) |
| **Date** | 2026-10-01, 14:41 local |
| **Environment** | Node 24.11.0, npm 11.7.0 |
| **Expected** | `tsc --noEmit` clean, Vite bundle produced |
| **Actual** | 33 modules transformed. `dist/assets/index-DPb4APc3.js` 203.39 kB (65.37 kB gzip), `index-tYXuqTTp.css` 4.80 kB. Built in 307 ms. No type errors |
| **Result** | **PASS** |
| **Evidence** | Build output; `admin-web/dist/` |

---

## Suite 3 — Dependency vulnerability scan

| Field | Value |
|---|---|
| **Command** | `npm audit` (from `admin-web/`) |
| **Date** | 2026-10-01, 14:41 local |
| **Environment** | npm 11.7.0, registry audit database |
| **Expected** | 0 known vulnerabilities |
| **Actual** | `found 0 vulnerabilities` |
| **Result** | **PASS** |
| **Scope limit** | Frontend dependencies only. No equivalent scan exists for backend or Android dependencies, and no SAST/DAST was run |

---

## Suite 4 — Android unit tests and debug build

| Field | Value |
|---|---|
| **Command** | `./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug` |
| **Date** | 2026-10-01, 14:42 local |
| **Environment** | JDK 25 as `JAVA_HOME` (`~/tools/jdk-25/jdk-25.0.3+9`), Android SDK 37, Gradle 9.6.0 |
| **Expected** | 10 tests pass, debug APK produced |
| **Actual** | `tests=10 failures=0 errors=0 skipped=0`; **BUILD SUCCESSFUL**; `app-debug.apk` = 20,318,592 bytes |
| **Result** | **PASS** |
| **Evidence** | `android/app/build/test-results/testDebugUnitTest/TEST-com.finai.mobile.FormatTest.xml` |

Test cases executed:

1. `formats whole dong amounts with thousands separators`
2. `shows a placeholder when an amount is absent`
3. `keeps the minus sign out of the grouped digits when signing`
4. `signs positive amounts with a plus`
5. `renders percentages to one decimal place`
6. `strips the time part off an instant for compact display`
7. `renders a date-only instant without a trailing time`
8. `renders a dash for missing or unparsable timestamps`
9. `renders iso dates day first`
10. `passes through a date it cannot decompose`

Environment sensitivity: **requires JDK 25** because
`android/gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`.
Automatic provisioning fails on this machine while an IDE language-server
process holds the JDK archive lock. See
[`../REPRODUCIBILITY.md`](../REPRODUCIBILITY.md).

Also note: only `FormatTest` exists. There are no Android repository, navigation
or UI tests, and no instrumented tests were executed (no emulator).

---

## Suite 5 — End-to-end API suite

| Field | Value |
|---|---|
| **Command** | `powershell -ExecutionPolicy Bypass -File tests/e2e-api-tests.ps1` |
| **Date** | 2026-10-01, 14:48 local (final run, against the freshly rebuilt container stack) |
| **Environment** | Containerised backend on `localhost:8081`, PostgreSQL 16 container |
| **Expected** | 160 assertions, 0 failures |
| **Actual** | `passed: 160`, `failed: 0` |
| **Result** | **PASS** |
| **Evidence** | `tests/results/e2e-20261001-144857.csv` |

Coverage by flow, as reported by the suite:

| Flow | Assertions |
|---|---|
| Preconditions (health) | 2 |
| Register/login/dashboard/statistics | 30 |
| Transaction validation (negative) | 9 |
| Authentication and authorisation (negative) | 4 |
| Cross-user isolation | 7 |
| AI refuses without data | 3 |
| Budget usage and thresholds | 23 |
| AI grounded in real data | 19 |
| Feedback round trip | 13 |
| Admin dashboard and categories | 17 |
| Lock / unlock / token revocation | 13 |
| Transaction edit and delete | 6 |
| Profile and empty states | 7 |
| **Total** | **160** |

### Repeatability

This suite has been run **8 times** across 2026-09-30 and 2026-10-01, against
both a host-run backend and a containerised backend, including once against a
stack torn down and rebuilt from scratch. All runs passed 160/160. The suite is
idempotent by construction: unique account and category identifiers per run, and
audit-log assertions compared against a baseline captured at run start.

---

## Suite 6 — Admin console API contract suite

| Field | Value |
|---|---|
| **Command** | `powershell -ExecutionPolicy Bypass -File tests/admin-web-contract.ps1` |
| **Date** | 2026-10-01, 14:47 local |
| **Environment** | Containerised backend on `localhost:8081` |
| **Expected** | 102 assertions, 0 failures |
| **Actual** | `passed: 102`, `failed: 0` |
| **Result** | **PASS** |
| **Evidence** | `tests/results/admin-web-contract-20261001-144700.csv` |

Coverage: admin login (5), dashboard (16), users (17), categories (18),
feedback (20), audit trail (17), system metrics (7), authorisation (3).

This suite exists because TypeScript types are erased at runtime: a backend
field rename would compile cleanly and fail only in a browser. It converts that
class of defect into a build failure.

---

## Suite 7 — Docker clean-state deployment

| Field | Value |
|---|---|
| **Command** | `docker compose down` → `docker compose build` → `docker compose up -d` |
| **Date** | 2026-10-01, 14:47 local |
| **Environment** | Docker Engine 29.6.2 |
| **Expected** | 3 images build, 3 containers healthy, schema initialised |
| **Actual** | `down` removed all containers and the network. `build` produced all 3 images. `up -d` started all 3. `docker compose ps` shows **3 healthy** |
| **Result** | **PASS** |

Post-deployment verification:

| Check | Expected | Actual | Result |
|---|---|---|---|
| PostgreSQL health | healthy | healthy | PASS |
| Backend health | healthy | healthy | PASS |
| Admin console health | healthy | healthy | PASS |
| Flyway migrations | 9 successful | 9 successful | PASS |
| Tables created | 11 | 11 | PASS |
| Backend→database connectivity | Working | `psql` via `exec` returned data | PASS |
| `/actuator/health` | `UP` | `UP` | PASS |
| Admin login via container | ADMIN + token | Role `ADMIN`, 226-char token | PASS |
| Console HTTP status | 200 | 200 | PASS |
| Errors in backend log | 0 | 0 ERROR/Exception lines | PASS |
| **E2E suite against this instance** | 160/160 | **160/160** | PASS |

The healthcheck fix (D-01) was verified as part of this: before the fix the
admin container reported `unhealthy` with `FailingStreak: 22`; after the fix it
reports `healthy` with streak 0.

---

## Aggregate

| Suite | Checks | Failures | Result |
|---|---|---|---|
| Backend unit | 98 | 0 | PASS |
| Backend integration | 15 | 0 | PASS |
| Admin contract assertions | 102 | 0 | PASS |
| E2E assertions | 160 | 0 | PASS |
| Android unit | 10 | 0 | PASS |
| **Total** | **385** | **0** | **PASS** |
| Admin console build | typecheck + bundle | 0 errors | PASS |
| npm audit | vulnerability scan | 0 found | PASS |
| Docker clean deployment | 11 checks | 0 failed | PASS |

## Not executed, and therefore not claimed

| Activity | Reason |
|---|---|
| Code coverage | No plugin configured. No coverage figure is claimed anywhere |
| Load/performance testing | No tooling; no latency or throughput figure claimed |
| SAST/DAST, penetration test | Not performed; `npm audit` is the only scan |
| UI test automation | No framework; console verified by build + contract suite |
| Android instrumented tests | No emulator available |
| Fault injection | Database-failure behaviour untested |
| Backup/restore verification | No backup capability implemented |
| Multi-browser testing | Console verified in one browser only |
| External AI provider | Only `AI_PROVIDER=LOCAL` executed |

Every claim in the project documentation is traceable to a row in this file.
Where an activity was not run, no result is asserted for it.