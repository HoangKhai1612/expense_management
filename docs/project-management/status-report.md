# Status report

Reporting date: 2026-10-01. All 16 phases complete.

## Summary

All six milestones are delivered and verified by executed commands. The system
runs as a Docker Compose stack: PostgreSQL 16, a Spring Boot 3.5 backend on
Java 21, a React administrator console, and a Jetpack Compose Android client.

## Verification results

| Suite | Result |
|---|---|
| `mvn test` (unit) | 98 run, 0 failures, 0 errors |
| `mvn verify` (integration) | 15 run, 0 failures, 0 errors |
| `tests/e2e-api-tests.ps1` | 160/160 passed, twice consecutively |
| `tests/admin-web-contract.ps1` | 102/102 passed |
| `npm run build` | Succeeds |
| `npm audit` | 0 vulnerabilities |
| `./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug` | 10/10 passed; APK 20.3 MB |
| `GET /actuator/health` | `{"status":"UP","groups":["liveness","readiness"]}` |
| `docker compose build` | 3 images built; all 3 containers healthy |
| Admin console from its image | HTTP 200 on port 5173 |

Both HTTP suites were additionally run against the containerised backend, not
only against the host-run process. Both passed again (160/160, 102/102), which
confirms the Compose deployment behaves identically rather than merely being
assumed to.

Schema verified: 9 migrations, 11 tables, 2 roles, 14 categories, 1 bootstrap
administrator.

## Work completed in the final stretch

**Admin console.** The console was an empty directory and is now a working React
18 / TypeScript / Vite application: authentication, dashboard, users, categories,
feedback and audit pages, an API client with typed responses, an authenticated
shell with navigation, Nginx configuration and a multi-stage Docker image.
Dependencies were moved to `react-router-dom ^7.18.4`, `vite ^8.3.1` and
`@vitejs/plugin-react ^5.1.0`.

**Contract suite.** TypeScript types are erased at runtime, so a backend field
rename compiles cleanly and fails only in the browser. `tests/admin-web-contract.ps1`
checks live responses against the declared types, turning that class of bug into
a build failure. First run: 86/102. After correcting the type mismatches: 102/102.

**Android client.** Replaced the default scaffold with seven screens — login,
dashboard, transactions, budgets, notifications, feedback, profile and AI chat —
plus an HTTP client, DTOs, repository, session store, two view models and
Compose theming.

**Documentation.** `README.md`, `docs/architecture.md`, `docs/api-reference.md`,
`docs/testing.md`, `docs/deployment.md`, `docs/acceptance-audit.md`,
`docs/known-limitations.md`, and the project-management set.

## Defect found and fixed

`AiAnalyst.spending()` seeded its fact list with `List.of(...)`, which is
immutable, then appended a computed average — so every spending-analysis call
threw `UnsupportedOperationException`. The most-used AI intent was broken in
production code.

Fixed by wrapping the initial list in `new ArrayList<>(...)`. The regression test
was validated by temporarily reverting the fix and observing 4
`UnsupportedOperationException` failures, then restoring it.

## Quality issues found and corrected

| Issue | Resolution |
|---|---|
| E2E suite not re-runnable | Made idempotent with unique identifiers and baseline-relative audit counts |
| Contract suite 86/102 | Corrected type mismatches; now 102/102 |
| `run-backend-local.ps1` discarded output | Now tees to `logs/backend.log` |
| Surefire/Failsafe not separated | Split so `mvn test` works without Docker |

## Outstanding

Ten items are not delivered and are documented rather than glossed over: UI test
automation, CI, load testing, security scanning, Kubernetes manifests, TLS,
backup automation, log aggregation, log redaction review, and Android release
signing.

Top risks are no CI (R10), unverified log redaction (R6), no backups (R12) and
no rate limiting (R13). Full analysis in
[`risk-register.md`](risk-register.md).

None of these block local use or demonstration. They block production
deployment.

## Recommendation

The next increment with the highest value per unit of effort is a CI pipeline.
It closes the top-ranked risk and makes every other guarantee in this report
enforceable rather than dependent on discipline. Log redaction review and a
`pg_dump`-based backup script are the sensible follow-ups.