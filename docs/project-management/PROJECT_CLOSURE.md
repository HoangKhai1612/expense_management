# Project closure

Closure record for the Personal Finance AI System. Updated 2026-10-01 after the
final audit and finalisation pass, in which every test suite was re-executed
rather than inherited.

> **History notice.** Version control was established **after** the development
> period. The repository baseline does not represent the complete development
> process, and historical provenance is `UNKNOWN / NOT RECORDED`. This notice is
> reproduced in the baseline commit message.

---

## 0. Finalisation pass — what changed

| Area | Change | Verified |
|---|---|---|
| Source control | Repository created, reconciled with the provided GitHub remote, baseline committed on top of the preserved template commit, pushed as a fast-forward | `rev-list --left-right --count` → `0 0` |
| Release tag | `v1.0.0-academic-final` created on the verified commit | `git ls-remote --tags` |
| Secret scan | Full pre-commit scan; `.gitignore` hardened | 0 secrets committed; `docs/SECRET_SCAN_REPORT.md` |
| **Defect: wrapper jar** | `gradle-wrapper.jar` was excluded by a blanket `*.jar` rule — a fresh clone could not build Android | `git check-ignore`; now committed |
| **Defect: agent tooling** | `.kilo/` would have been committed | Added to `.gitignore` |
| Logging review | All 20 log statements audited; runtime log scanned against live `.env` values | 0 secrets in logs. `docs/LOGBACK_SECURITY_REVIEW.md` |
| Spring default user | Tested for exploitability | 401 on two endpoints — non-exploitable |
| `/api/auth/me` | Fully documented; behaviour unchanged | 200 verified, 401 anonymous |
| Android feedback | Classified **Out of current academic scope** with rationale | `DISCREPANCY_REPORT.md` D-03 |
| Documentation | 29 files, all status claims reconciled | `docs/` |

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

### The five named production gaps — one now closed

| # | Gap | Status | Consequence |
|---|---|---|---|
| L-01 | **No CI pipeline** | **OPEN** | Verification depends on discipline; a failing change can land unnoticed |
| L-02 | **No TLS termination** | **OPEN** | Plain HTTP; cannot be exposed publicly without a proxy |
| L-03 | **No production backup strategy** | **OPEN** | Data loss is unrecoverable |
| L-04 | **No rate limiting** | **OPEN** | Brute-force and AI-abuse exposure |
| L-05 | ~~Log-redaction review incomplete~~ | **CLOSED 2026-10-01** | Review performed; no secret is logged. Residual recommendation: add a masking converter as future-proofing |

**Four of the five production gaps remain open.** They are stated as open in every
relevant document.

### Closed during the finalisation pass

| Limitation | Resolution |
|---|---|
| No source control | Repository, remote, baseline commit, tag. **Historical provenance permanently unavailable** |
| Log-redaction review incomplete | Completed — `docs/LOGBACK_SECURITY_REVIEW.md` |
| `/api/auth/me` undocumented | Fully documented |
| Android feedback UI | Classified out of scope with rationale |

### Further open limitations

| # | Gap | Consequence |
|---|---|---|
| L-06 | No UI test automation | Rendering regressions undetected |
| L-10 | No load or performance testing | No capacity evidence |
| L-11 | No security scanning or penetration test | Only `npm audit` performed |
| L-12 | Single currency | No multi-currency support |
| L-13 | No refresh tokens | Re-authentication after token expiry |
| L-14 | Android token stored unencrypted | Readable on a rooted device |
| L-15 | External AI provider unverified | Only `LOCAL` mode executed |
| L-16 | No orchestration manifests | Compose only |
| L-17 | No log aggregation or alerting | Production failures go unnoticed |
| L-18 | No e-mail verification or password reset | No mail transport configured |
| L-19 | Live admin password is the published demo credential | Configuration-only remedy; disclosed as SEC-01 |

### Why these are not being fixed now

The audit brief directs implementing changes only where a real defect,
requirement gap, security issue or reproducibility problem exists. These are
**scope decisions for the project owner**, and most are infrastructure
commitments rather than code changes. Closing them would require decisions about
deployment target, monitoring stack and hosting budget that this project has not
made.

The finalisation pass fixed the two items that met that bar — the wrapper-jar
reproducibility defect and the ignore-rule gaps — and left the rest disclosed.

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

### D-4 — Gradle wrapper jar excluded from version control *(finalisation pass)*

- **Detection:** `git check-ignore` during the pre-commit audit
- **Impact:** **a fresh `git clone` could not build the Android project** —
  `./gradlew` would fail with `Could not find or load main class
  org.gradle.wrapper.GradleWrapperMain`
- **Root cause:** a blanket `*.jar` rule in `.gitignore` with no exception for the
  wrapper
- **Fix:** added `!android/gradle/wrapper/gradle-wrapper.jar`
- **Verification:** jar confirmed present in the baseline commit
- **Status:** CLOSED
- **Lesson:** no test could have caught this, because local builds used the jar
  already on disk. A working directory can mask a repository defect

### D-5 — Agent tooling would have been committed *(finalisation pass)*

- **Detection:** file enumeration before staging
- **Impact:** `.kilo/` (11 files of agent configuration, including a bundled
  third-party Jupyter skill) would have entered the repository as project source
- **Root cause:** `.gitignore` covered IDE directories but not agent tooling
- **Fix:** added `.kilo/`, `.claude/`, `.cursor/`
- **Verification:** commit set reduced from 238 to 232 files
- **Status:** CLOSED

### Closed, documentation only

- **D-6:** `/api/auth/me` implemented but undocumented → fully documented
- **D-7:** Android notification coverage described as a screen → corrected to
  "rendered within Profile"
- **D-8:** Generated Spring Security password warning → tested and confirmed
  non-exploitable

## 5. Closure checklist

| Item | Status |
|---|---|
| All specified functionality implemented | Complete |
| All test suites executed and passing | Complete — 385 checks, 0 failures |
| Deployment verified from clean state | Complete — 3/3 healthy |
| Documentation written and audited | Complete — 29 files |
| Discrepancies identified | 8 found, 8 resolved or classified |
| Defects found and fixed | **5 of 5** |
| Source control | **Established** — baseline + tag. History permanently unavailable |
| Secret scan | **Complete** — 0 secrets committed, 0 in logs |
| Logging security review | **Complete** — 0 secrets logged |
| Risks identified and scored | 18, 0 high open |
| Metrics recorded with provenance | Complete |
| Effort and schedule history | **Not reconstructible** — documented as a data gap |
| CI pipeline | **Not implemented** |
| TLS / backups / rate limiting | **Not implemented** |

## 6. Final position

**ACADEMIC PROJECT: ACCEPTED**

Every specified capability is implemented and demonstrated, 385 automated checks
pass with zero failures, the system deploys from clean state in one command, the
repository is version-controlled with a tagged verified baseline, and the
documentation is traceable to executed evidence.

**PRODUCTION DEPLOYMENT: NOT CLAIMED**

Four production gaps remain open — CI, TLS, backups, rate limiting — and are
stated as open in every relevant document.

## 7. Lessons learned

Derived from events that actually occurred in this project. No invented lessons.

### Lesson 1 — Documentation claims must be verified against execution

Three documented claims turned out to be wrong or incomplete:

- "3 containers healthy" was documented while the admin container reported
  `unhealthy` with a failing streak of 22
- `/actuator/metrics` was assumed public; it actually returns 401
- Android notification coverage was described as a screen; it renders inside
  Profile

None was caught by re-reading the documentation. All three were caught by
inspecting the running system. **A claim that has not been executed is not
evidence.**

### Lesson 2 — Regression tests must fail against the original defect

For the `AiAnalyst` bug, the fix was deliberately reverted to confirm the test
produces 4 `UnsupportedOperationException` failures, then restored. A test that
has never been observed failing proves nothing. The same discipline was applied
to `Format.kt`, where 2 failing assertions preceded the fix.

### Lesson 3 — Container health status is not the same as application availability

The admin container served HTTP 200 to every host request while its healthcheck
failed continuously. Only the container's own health state revealed the defect —
and only *after* probing from inside the container could the IPv4/IPv6 mismatch
be diagnosed. Passing end-to-end tests did not catch it.

### Lesson 4 — A working directory can mask a repository defect

`gradle-wrapper.jar` was excluded by `.gitignore` for the entire project. Every
local build succeeded, all 385 checks passed, and the omission would only have
surfaced for someone cloning the repository — at which point the Android project
could not be built at all. **Reproducibility must be tested from a clean clone,
not from the working tree.**

### Lesson 5 — Project management evidence must be captured during development

There is no commit history, issue tracker, schedule or effort record, because
version control was not used during development. Effort, duration, team size and
velocity are permanently `UNKNOWN / NOT RECORDED` and cannot be reconstructed
now, however carefully. This is the single most consequential lesson of the
project: **evidence that is not captured at the time it exists cannot be
recovered afterwards.**

### Lesson 6 — Version control should exist from project initiation

Version control was established only in the final pass. The result is a verified
baseline with no development history behind it. The technical risk was removed —
code is now reviewable and diffable — but the historical risk was created and
cannot now be repaired.