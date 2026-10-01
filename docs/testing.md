# Testing

Everything below was executed. Counts are taken from the reports in
`backend/target/*-reports`, not estimated.

| Suite | What it covers | Count | Needs |
|---|---|---|---|
| Backend unit (Surefire) | Services, JWT, intent parsing, analyst | 98 | JDK only |
| Backend integration (Failsafe) | Migrations, bootstrap, AI grounding against real PostgreSQL | 15 | Docker |
| End-to-end API | Every endpoint over HTTP | 160 | Running backend |
| Admin Web contract | Console types against live responses | 102 | Running backend |
| Admin Web build | `tsc` typecheck + Vite bundle | – | Node |
| Android unit | Currency and date formatting | 10 | Android SDK, JDK 25 |

## Running them

```bash
# Backend unit tests only - no Docker, ~30s
cd backend && mvn test

# Unit + integration - requires a working Docker daemon
mvn verify

# The two PowerShell suites - start the backend first
./tests/e2e-api-tests.ps1
./tests/admin-web-contract.ps1

# Admin console
cd admin-web && npm run build

# Android - the daemon JVM is pinned to 25, see "Toolchain note" below
cd android && ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

### Toolchain note

`android/gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`, so
Gradle wants a JDK 25 daemon regardless of `JAVA_HOME`. On this machine the
foojay-resolved JDK 25 archive was fully downloaded but its lock was held
openly by a long-running IDE language-server process, so auto-provisioning
failed with "Timeout waiting to lock".

The fix is to point `JAVA_HOME` at an extracted copy of that same archive:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-25\jdk-25.0.3+9"
cd android; ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

Passing `org.gradle.java.installations.paths` does **not** work as a workaround:
daemon-JVM criteria are resolved before project properties are applied. Use
`cleanTestDebugUnitTest` rather than a bare `testDebugUnitTest` when you want a
genuine re-execution, and do not run two Gradle invocations concurrently in the
same project — they collide over the daemon and the reported failure is
misleading.

On this machine the JDK is at `$env:USERPROFILE\tools\jdk-21.0.12.1+1` and Maven
at `$env:USERPROFILE\tools\apache-maven-3.9.9\bin\mvn.cmd`.

`mvn verify` runs `*Test` under Surefire and `*IT` under Failsafe, so
`mvn test` alone gives a meaningful result on a machine without Docker.

## Verified against both deployment modes

Both HTTP suites were run twice: once against the backend on the host and once
against the `finai-backend` container from `docker compose up`. Both passed
(160/160 and 102/102 each time). This confirms the containerised deployment is
behaviourally equivalent, not merely assumed to be.

The Compose path additionally verified: `docker compose build` produces all three
images, all three containers report healthy, the admin console image serves HTTP
200 on port 5173, and admin login against the container returns an ADMIN token.

## Why the suites are split

Surefire and Failsafe are configured separately in `backend/pom.xml`. The reason
is practical rather than stylistic: the integration tests need Testcontainers,
which needs a Docker daemon. If they shared a lifecycle phase, a developer
without Docker could not run any tests at all. Separating them means step 1 above
always works.

## Unit tests

| Class | Tests | Focus |
|---|---|---|
| `AuthServiceTest` | password policy, duplicate handling, lock/deactivate, unknown-account timing |
| `JwtServiceTest` | sign/parse round trip, expiry, tamper detection, subject extraction |
| `TransactionServiceTest` | amount and date validation, ownership, category-type matching, deletion |
| `BudgetServiceTest` | period windows, usage arithmetic, threshold transitions |
| `CategoryServiceTest` | personal vs system visibility, deactivate-not-delete, cross-user 403 |
| `AiTextTest` | intent classification, period parsing, precedence between intents |
| `AiAnalystTest` | fact generation per intent, ungrounded paths, empty-baseline handling |

`AiAnalystTest` is the regression net for a real defect: the analyst seeded its
fact list with `List.of(...)`, which is immutable, and then added a computed fact
to it. Every spending-analysis call threw `UnsupportedOperationException`. The
fix wraps the initial list in `new ArrayList<>(...)`. The test was confirmed to
still catch the original defect by temporarily reverting the fix and observing 4
failures, then restoring it.

## Integration tests

`MigrationsAndBootstrapIT` runs all 9 Flyway migrations against a real
PostgreSQL 16 container and asserts on the result: 11 tables including
`flyway_schema_history`, 2 roles, 14 categories, 1 bootstrap administrator. It
also asserts the passwords are stored as hashes.

`AiGroundingIT` inserts a known transaction set and asserts the assistant reports
exactly those aggregates — the assertion is on values, not on wording, so a
wording change does not break it.

## End-to-end API suite

`tests/e2e-api-tests.ps1` drives the real HTTP surface: authentication,
categories, transactions, budgets, statistics, notifications, AI chat, feedback,
and every admin endpoint including the audit trail.

It is idempotent and safe to re-run. Each run creates a uniquely named account
and uniquely coded categories, and audit-log assertions compare against a
baseline count captured at the start of the run rather than a hard-coded
absolute number. Two consecutive full runs passed 160/160.

Results are written to `tests/results/e2e-<timestamp>.csv`.

## Admin Web contract suite

`tests/admin-web-contract.ps1` checks that the live API responses match what
`admin-web/src/api/types.ts` declares: field presence and type shape for login,
user, category, feedback, audit, dashboard, and the `PageResponse<T>` envelope.

This exists because TypeScript types are erased at runtime. A field rename in the
backend compiles fine and fails only in the browser. This suite turns that class
of bug into a build failure. 102/102 assertions pass.

## What is not covered

Stated plainly so the gap is visible rather than assumed:

- **No UI test automation.** The admin console and Android app are verified by
  typecheck, build and manual walkthrough, not by Selenium/Espresso. The contract
  suite verifies the data layer the UI consumes, not the rendering.
- **No load or performance testing.** There is no evidence about behaviour under
  concurrent users, and no p95 figures.
- **No automated security scan.** `npm audit` reports 0 known vulnerabilities for
  the console, but there is no SAST/DAST pass and no penetration test.
- **Single database engine.** Only PostgreSQL is exercised. The schema uses no
  vendor-specific types, but that has not been proven on another engine.
- **No failure-injection tests.** There is no test that kills the database
  mid-transaction to confirm the service degrades rather than corrupting.