# Project closure

Closure record for the Personal Finance AI System. Prepared 2026-10-01 after the
final audit, in which every test suite was re-executed rather than inherited.

---

## 1. Final product status

### Backend — FUNCTIONAL
Spring Boot 3.5.16 on Java 21. 78 source files, 5,500 lines, 16 packages.
Exposes 39 REST paths. Stateless JWT authentication with BCrypt cost-12 hashing.
Runs as a non-root user in a 191 MB image. Verified by 113 automated tests.

### Database — FUNCTIONAL
PostgreSQL 16. Nine Flyway migrations (`V1__create_users` …
`V9__seed_reference_data`) produce 11 tables. Seed data: 2 roles, 14 categories.
`ddl-auto: validate` means the schema can only change through a migration, and
startup fails on drift. Verified on a fresh volume.

### AI assistant — FUNCTIONAL
Seven intents, deterministic classification supporting Vietnamese. Answers are
split into `FACT` (verified aggregates) and `SUGGESTION` (labelled judgement).
`grounded=false` whenever the assistant declines to produce a figure. Facts are
persisted with each message. **No text-to-SQL path exists**, so the assistant
cannot be steered toward another account. Verified by 41 tests across
`AiTextTest`, `AiAnalystTest` and `AiGroundingIT`, plus 19 E2E assertions.

### Android client — FUNCTIONAL, PARTIAL
Kotlin, Jetpack Compose. 22 files, 2,668 lines, 6 screens: login, dashboard,
transactions, budgets, AI chat, profile. Retrofit with token injection;
`Result` distinguishes loaded, failed and offline. 10 unit tests pass; debug APK
is 20.3 MB. Notifications render inside Profile rather than on a dedicated
screen. **No feedback UI** (limitation L-08).

### Admin console — FUNCTIONAL
React 18, TypeScript, Vite 8. 14 files, 1,635 lines. Six pages: dashboard,
users, categories, feedback, audit, login. Typechecks clean, bundles to 203 kB.
Served by Nginx from a 21.3 MB image. 0 npm vulnerabilities. 102 contract
assertions verify the console's types against live API responses.

### Docker — FUNCTIONAL
Three images build from a clean state and start as three healthy containers.
Startup is ordered by the PostgreSQL health check. Verified by a full
`down → build → up` cycle, after which the 160-assertion E2E suite passed
against the freshly built stack.

## 2. Acceptance

### Acceptance criteria mapped to evidence

| Criterion | Evidence | Result |
|---|---|---|
| Backend compiles and all tests pass | `mvn verify` → BUILD SUCCESS | PASS |
| Unit tests exist and pass | 98 tests, 7 classes, 0 failures | PASS |
| Integration tests against a real database | 15 tests via Testcontainers | PASS |
| API contract is stable | 102 contract assertions | PASS |
| All documented endpoints function | 160 E2E assertions | PASS |
| No cross-user data leakage | 7 E2E assertions, all 404 | PASS |
| Admin-only routes protected | 3 E2E + 3 contract assertions, all 403 | PASS |
| AI never states an ungrounded figure | 8 grounding IT + 5 E2E refusal assertions | PASS |
| Locking revokes existing tokens | E2E 401 assertion | PASS |
| Categories deactivate, history preserved | E2E assertion | PASS |
| Console builds and typechecks | `npm run build` | PASS |
| No known frontend vulnerabilities | `npm audit` → 0 | PASS |
| Android builds and tests pass | 10 tests, APK 20.3 MB | PASS |
| Stack deploys from clean | `down && build && up` → 3 healthy | PASS |
| Schema initialises automatically | 9 migrations on a fresh volume | PASS |
| No secret in tracked source | Value-level search, 0 matches | PASS |
| Startup is error-free | 0 ERROR lines in backend log | PASS |

### Aggregate

| Measure | Value |
|---|---|
| Automated checks executed | 385 |
| Failures | 0 |
| Requirement rows traced | 164 |
| Verified | 147 |
| Acceptance criteria met | 17 of 17 |

## 3. Remaining limitations — preserved

These are project limitations. They are stated here, in
[`../known-limitations.md`](../known-limitations.md), and in
[`FINAL_PROJECT_STATUS.md`](FINAL_PROJECT_STATUS.md). None is hidden.

### The five named production gaps — all OPEN

| # | Gap | Consequence |
|---|---|---|
| L-01 | **No CI pipeline** | Verification depends on discipline; a failing change can land unnoticed |
| L-02 | **No TLS termination** | Plain HTTP; cannot be exposed publicly without a proxy |
| L-03 | **No production backup strategy** | Data loss is unrecoverable |
| L-04 | **No rate limiting** | Brute-force and AI-abuse exposure |
| L-05 | **Log-redaction review incomplete** | Unquantified risk of credential exposure in logs |

### Further open limitations

| # | Gap | Consequence |
|---|---|---|
| L-06 | No UI test automation | Rendering regressions undetected |
| L-07 | **No source control repository** | No history, authorship or provenance; no code review possible |
| L-08 | No Android feedback UI | Feature unreachable from mobile |
| L-09 | `/api/auth/me` undocumented | Documentation gap, no runtime impact |
| L-10 | No load or performance testing | No capacity evidence |
| L-11 | No security scanning or penetration test | Only `npm audit` performed |
| L-12 | Single currency | No multi-currency support |
| L-13 | No refresh tokens | Re-authentication after token expiry |
| L-14 | Android token stored unencrypted | Readable on a rooted device |
| L-15 | External AI provider unverified | Only `LOCAL` mode executed |
| L-16 | No orchestration manifests | Compose only |
| L-17 | No log aggregation or alerting | Production failures go unnoticed |
| L-18 | No e-mail verification or password reset | No mail transport configured |

### Why these are not being fixed now

The audit brief directs implementing changes only where a real defect,
requirement gap, security issue or reproducibility problem exists. These five are
**scope decisions for the project owner**, and four of the five (CI, backups,
TLS, alerting) are infrastructure commitments rather than code changes. Closing
them would require a decision about deployment target, monitoring stack and
hosting budget that this project has not made.

One of them, L-05, *is* actionable without such a decision: reviewing the Logback
configuration for token and password leakage is a bounded task. It remains open
and is recorded as such.

## 4. Defects found and closed

### D-1 — `AiAnalyst.spending()` mutated an immutable list

- **Detection:** code review
- **Impact:** `UnsupportedOperationException` on every spending-analysis call —
  the most-used AI intent returned a server error
- **Root cause:** the fact list was seeded with `List.of(...)`, which is
  immutable, then appended to
- **Fix:** wrap the initial list in `new ArrayList<>(...)`
- **Regression proof:** the fix was temporarily reverted; the suite produced 4
  `UnsupportedOperationException` failures; the fix was restored
- **Status:** CLOSED

### D-2 — Duplicate `formatInstant` in `Format.kt`

- **Detection:** unit test (`renders a dash for missing or unparsable timestamps`)
- **Impact:** one implementation shadowed the correct one, rendering arbitrary
  text verbatim on screen
- **Root cause:** two definitions of the same function in one file
- **Fix:** removed the duplicate; added regex validation and date-only handling
- **Regression proof:** 2 failing assertions observed before the fix
- **Status:** CLOSED

### D-3 — Admin console healthcheck false negative

- **Detection:** `docker compose ps` reported `unhealthy` with
  `FailingStreak: 22` while the service served traffic normally
- **Impact:** the deployment health signal could not distinguish a healthy
  service from a broken one; documentation had claimed "3 healthy" while the
  container reported otherwise
- **Root cause:** the healthcheck probed `http://localhost/`, which resolves to
  IPv6 `::1` inside the container, while nginx binds IPv4 `0.0.0.0:80`
- **Fix:** probe `http://127.0.0.1/`
- **Verification:** container now reports `healthy`, streak 0
- **Status:** CLOSED

### Open, documentation only

- **D-4:** `/api/auth/me` implemented but undocumented
- **D-5:** Android notification coverage described as a screen; it renders
  inside Profile

Neither has runtime impact. Both are recorded rather than fixed, to avoid
editing verified documentation during the audit freeze.

## 5. Closure checklist

| Item | Status |
|---|---|
| All specified functionality implemented | Complete |
| All test suites executed and passing | Complete |
| Deployment verified from clean state | Complete |
| Documentation written and audited | Complete, 2 minor open items |
| Discrepancies identified | 5 found, 3 fixed, 2 open |
| Defects found and fixed | 3 of 3 |
| Risks identified and scored | 17, 0 high open |
| Metrics recorded with provenance | Complete |
| Effort and schedule history | **Not reconstructible** — documented as a data gap |
| Source control | **Absent** — documented as the principal PM gap |
| Production hardening | **Not undertaken** — 5 gaps open |

## 6. Final position

**Academic project acceptance: verified.**

Every specified capability is implemented and demonstrated, 385 automated checks
pass with zero failures, the system deploys from clean state in one command, and
the documentation is traceable to executed evidence.

**Production readiness: not claimed.** Five production gaps remain open — CI,
TLS, backups, rate limiting, and the log-redaction review — and the project is
not under version control. These are stated as open in every relevant document
rather than presented as resolved.

## 7. Recommended next actions

In descending value per unit of effort:

1. **Initialise a Git repository** and begin versioning. This closes the
   highest-impact finding, enables code review, and makes future history
   reconstructible. It will not reconstruct past history, and is not intended to.
2. **Review the Logback configuration** for token and password leakage. Bounded,
   self-contained, closes L-05.
3. **Add a GitHub Actions workflow** running the five existing suites. Closes
   L-01; the suites are already scripted, so this is configuration rather than
   development.
4. **Add a `pg_dump` backup script** with documented restore steps. Closes the
   data-loss portion of L-03.
5. **Decide the deployment target**, then add TLS and rate limiting to match.

Steps 1–3 are achievable without new architectural decisions and would move the
project substantially closer to production readiness.