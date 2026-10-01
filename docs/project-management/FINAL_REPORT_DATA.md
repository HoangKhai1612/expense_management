# Final report dataset

Consolidated verified facts for report writing. Every quantitative statement
carries its evidence. Anything not measurable is listed in section 17 as
unavailable rather than estimated.

Audit date: 2026-10-01.

---

## 1. Project objectives

Build a personal finance tracker whose AI assistant **never states an ungrounded
figure**. Every number reported is a parameterised aggregate over the
authenticated user's own rows, and every answer separates verified facts from
judgement.

Secondary objective: an administrator console for accounts, the category
catalogue, feedback triage and an audit trail.

## 2. Scope

### Delivered

| Area | Capability |
|---|---|
| Authentication | Register, login by email or username, password change, account lock/unlock/deactivate |
| Profile | Read and update, notification list |
| Categories | System catalogue + personal, create/update/deactivate |
| Transactions | Full CRUD, filters, recent, validation |
| Budgets | Weekly/monthly/yearly, live usage, threshold alerts |
| Statistics | Overview, by-category, monthly, daily |
| Dashboard | Composed home payload |
| AI | 7 intents, grounded answers, transcript persistence |
| Feedback | Submit, list, admin triage with reply |
| Administration | User management, category management, feedback triage, dashboard, system metrics, audit trail |

### Not delivered

UI automation, CI, load testing, security scanning, orchestration, TLS,
backups, rate limiting, multi-currency, refresh tokens, e-mail flows. See
`../known-limitations.md`.

## 3. Architecture

| Element | Decision | Rationale |
|---|---|---|
| Layering | By business capability, not by layer | A rule change stays inside one package |
| AI data access | Typed snapshot, **no text-to-SQL** | Removes injection risk; user id cannot be influenced by the question |
| AI answers | `FACT` + `SUGGESTION`, `grounded` flag | Facts and judgement can never be confused |
| Category removal | Deactivate, never delete | Preserves transaction history |
| Account locking | Status re-checked per request | Revokes tokens immediately, not at expiry |
| Errors | One envelope with a machine code | Clients branch on a code, not prose |
| Pagination | One `PageResponse<T>` shape | Both clients share it |
| Null handling | Jackson `non_null` | Clients type nullable fields as optional |
| Test lifecycle | Surefire/Failsafe split | `mvn test` works without Docker |

## 4. Technologies

| Layer | Technology | Version |
|---|---|---|
| Backend | Spring Boot | 3.5.16 |
| Language | Java | 21 |
| Build | Maven | 3.9.9 |
| Database | PostgreSQL | 16-alpine |
| Migration | Flyway | 9 migrations |
| ORM | Spring Data JPA / Hibernate | `ddl-auto: validate` |
| Security | Spring Security + JJWT | BCrypt cost 12 |
| Docs | springdoc OpenAPI + Swagger UI | 39 paths |
| Frontend | React + TypeScript + Vite | 18 / 5.x / 8.3.1 |
| Routing | react-router-dom | 7.18.4 |
| Mobile | Kotlin + Jetpack Compose | AGP 9.4.1 |
| Networking | Retrofit + OkHttp + Gson | 2.11.0 / 4.12.0 |
| Storage | DataStore Preferences | 1.1.1 |
| Container | Docker + Compose v2 | Engine 29.6.2 |
| Web server | Nginx | 1.27-alpine |
| Android tests | JUnit | 4.13.2 |

## 5. Requirements

| Measure | Value |
|---|---|
| Requirement rows traced | 164 |
| Verified | 148 |
| Partial | 8 |
| Gap | 7 |
| Out of scope | 1 |

Traceability in [`REQUIREMENT_TRACEABILITY.md`](REQUIREMENT_TRACEABILITY.md).

## 6. WBS

16 phases, 6 milestones. Full detail in
[`project-plan.md`](project-plan.md).

| Milestone | Phases | Contents |
|---|---|---|
| M1 Data foundation | 0–1 | Skeleton, toolchain, 9 migrations |
| M2 Core finance | 2–8 | Security, users, categories, transactions, budgets, notifications, statistics |
| M3 AI assistant | 9–10 | Snapshot, analyst, intents, feedback |
| M4 Administration | 11–12 | Admin console API, audit trail |
| M5 Clients | 13–14 | Admin web, Android |
| M6 Verification | 15 | E2E, contract suite, docs, audit |

## 7. Schedule

**NOT RECORDED.** No baseline plan, no version control, no issue tracker, no
dated history.

The only defensible time measurement: the **verification phase** spanned
2026-09-30 23:37 → 2026-10-01 14:48 (≈15 hours wall-clock), from CSV timestamps.
This is not a project duration.

See [`EFFORT_DATA_GAP.md`](EFFORT_DATA_GAP.md).

## 8. Risks

| Measure | Value |
|---|---|
| Risks in register | 17 |
| High (score ≥15) open | **0** |
| Medium open | 6 |
| Occurred and fixed | 3 |
| Retired | 5 |

Top open risks: no CI (R-10), no backups (R-12), no rate limiting (R-13), no
TLS (R-16). Log redaction (R-06) and source control (R-15) are now **retired** —
both reviews were completed and the repository now exists.

Full audit in [`RISK_AUDIT.md`](RISK_AUDIT.md).

## 9. Quality

| Criterion | Result |
|---|---|
| Backend tests | 113 pass, 0 fail |
| E2E assertions | 160 pass, 0 fail |
| Contract assertions | 102 pass, 0 fail |
| Android tests | 10 pass, 0 fail |
| Typecheck | Clean |
| Dependency scan | 0 vulnerabilities (frontend) |
| Containers healthy | 3 of 3 |
| Startup errors | 0 |
| **Code coverage** | **Not configured — not claimed** |

Full mapping in [`QUALITY_PLAN.md`](QUALITY_PLAN.md).

## 10. Configuration

| Element | State |
|---|---|
| Source control | **Not in use** |
| Schema versioning | Strong — 9 migrations, checksummed, drift-detected |
| Environment config | 17 documented variables |
| Secrets in source | None |
| Image versioning | Fixed tags, no `latest` |
| Android build | Requires manual JDK 25 step |

See [`CONFIGURATION_BASELINE.md`](CONFIGURATION_BASELINE.md).

## 11. Tests

| Suite | Command | Checks | Failures |
|---|---|---|---|
| Backend unit + integration | `mvn verify` | 113 | 0 |
| Console build | `npm run build` | typecheck + bundle | 0 errors |
| Dependency scan | `npm audit` | full scan | 0 found |
| Android | `gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug` | 10 | 0 |
| E2E API | `tests/e2e-api-tests.ps1` | 160 | 0 |
| Contract | `tests/admin-web-contract.ps1` | 102 | 0 |
| **Total** | | **385** | **0** |

Full evidence with dates and environments in
[`FINAL_TEST_EVIDENCE.md`](FINAL_TEST_EVIDENCE.md).

Repeatability: the E2E suite passed 8 times across 2 days, against host and
containerised backends, including after a full rebuild.

## 12. Defects

| ID | Defect | Severity | Detection | Status |
|---|---|---|---|---|
| D-1 | `AiAnalyst` immutable-list mutation | High | Code review | Fixed |
| D-2 | Duplicate `formatInstant` | Medium | Unit test | Fixed |
| D-3 | Admin healthcheck IPv6 false negative | Medium | Container health | Fixed |
| D-4 | `/api/auth/me` undocumented | Low | Doc audit | Open |
| D-5 | Notification screen overstated | Low | Doc audit | Open |

Each closed defect has a regression test proven to detect the original fault.

## 13. Corrective actions

| Defect | Action | Verification |
|---|---|---|
| D-1 | `new ArrayList<>(...)` wrapper | 4 failures when reverted, then restored |
| D-2 | Removed duplicate; regex validation added | 2 failing assertions before fix |
| D-3 | Probe `127.0.0.1` instead of `localhost` | `FailingStreak` 22 → 0, status healthy |

## 14. Monitoring and control

Real control loops that operated during this project:

### Loop 1 — `AiAnalyst` defect

```
Defect observed (spending intent returned a server error)
→ Investigated (UnsupportedOperationException in stack trace)
→ Root cause (List.of(...) is immutable; list was appended to)
→ Fix (wrap in new ArrayList<>(...))
→ Regression test (confirmed to fail against the original code: 4 failures)
→ Verification (mvn verify 113/113; E2E 160/160)
→ Documentation (acceptance audit, risk register R-01)
```

### Loop 2 — `Format.kt` duplicate

```
Defect observed (2 failing assertions)
→ Investigated (two formatInstant definitions in one file)
→ Root cause (a shadowing implementation rendered garbage verbatim)
→ Fix (removed the duplicate; regex validation; date-only case)
→ Regression test (10 assertions covering null, empty, malformed, date-only)
→ Verification (gradle BUILD SUCCESSFUL)
→ Documentation (testing.md)
```

### Loop 3 — Admin healthcheck

```
Defect observed (docker compose ps reported unhealthy, streak 22, while HTTP 200)
→ Investigated (health log showed "Connection refused")
→ Root cause (localhost → ::1 IPv6; nginx binds IPv4 0.0.0.0:80)
→ Fix (probe 127.0.0.1)
→ Verification (container healthy, streak 0, clean rebuild passes)
→ Documentation (discrepancy D-01, risk R-16, security audit)
```

### What monitoring does not exist

CI, log aggregation, metrics scraping, alerting, tracing, uptime measurement,
progress tracking.

## 15. Acceptance

| Measure | Value |
|---|---|
| Acceptance criteria defined | 17 |
| Criteria met | 17 |
| Automated checks | 385, 0 failures |
| Requirements verified | 147 of 164 |

See [`PROJECT_CLOSURE.md`](PROJECT_CLOSURE.md) section 2.

## 16. Limitations

18 limitations recorded. Four production gaps remain **open**: **CI (L-01),
TLS (L-02), backups (L-03), rate limiting (L-04)**. L-05, log-redaction review,
is **closed** — the review was performed and no secret is logged. Full list in
[`FINAL_PROJECT_STATUS.md`](FINAL_PROJECT_STATUS.md) section 7.

## 17. Project closure

| Item | Status |
|---|---|
| Functionality | Complete |
| Testing | Complete — 385 checks, 0 failures |
| Deployment | Verified from clean state |
| Documentation | Complete — 28 files, 8 discrepancies all resolved or classified |
| Defects | 5 code defects found, 5 fixed |
| Risks | 0 high open |
| Effort/schedule history | **Not reconstructible** |
| Source control | **Established** — baseline `59cc2a0`, tag `v1.0.0-academic-final`. History permanently unavailable |
| Production hardening | **Partly undertaken** — 4 gaps open, 1 closed |

## 18. Statements that must NOT be made

| Do not claim | Reason |
|---|---|
| "The project took N weeks" | No schedule data exists |
| "N person-hours of effort" | No time records exist |
| "N developers" | No authorship evidence exists |
| Any Git history or commit count | No repository exists |
| Sprint, velocity or burndown figures | No Agile process was used |
| A code coverage percentage | No coverage tooling configured |
| p95 latency or throughput | No load test performed |
| "Production ready" | 5 production gaps open |
| "Sprint planning was followed" | No such artefacts exist |

---

## Quantities quick reference

| Quantity | Value | How obtained |
|---|---|---|
| Backend source | 78 files, 5,500 lines | Filesystem |
| Admin source | 14 files, 1,635 lines | Filesystem |
| Android source | 22 files, 2,668 lines | Filesystem |
| Backend packages | 16 | Filesystem |
| REST paths | 39 | Live OpenAPI |
| Migrations | 9 | `flyway_schema_history` |
| Tables | 11 | `information_schema` |
| Android screens | 6 | `ui/screens` |
| Automated checks | 385 | Test reports |
| Documentation | 26 `.md` | Filesystem |
| Defects fixed | 3 | This audit + prior review |
| Risks | 17 | Risk register |
| Docker images | 3 | `docker compose images` |
| APK size | 20,318,592 bytes | Filesystem |
| Console bundle | 203.39 kB JS | Vite output |