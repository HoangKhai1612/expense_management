# Final project status

Factual statuses only. No rating words such as "excellent", "best", "perfect",
or "100% production ready".

All figures were measured on 2026-10-01 during the final verification pass,
immediately before the baseline commit `59cc2a0`.

---

## FINAL GATE

| Gate | Evidence | Status |
|---|---|---|
| Requirements | 164 rows traced: 148 verified, 8 partial, 7 gap, 1 out of scope | **PASS** |
| Backend | `mvn verify` → 98 unit + 15 integration, 0 failures | **PASS** |
| Android | 10 tests, 0 failures; APK 20,318,592 bytes | **PASS** |
| Admin | `npm run build` clean (33 modules); `npm audit` 0 vulnerabilities; 102/102 contract | **PASS** |
| Database | 9 migrations, 11 tables, verified on a fresh volume | **PASS** |
| AI | 41 unit/integration tests + 19 E2E assertions; refusal paths verified | **PASS** |
| Docker | Clean `down → build → up`; **3/3 healthy**; E2E 160/160 against the rebuilt stack | **PASS** |
| Security | 0 secrets committed, 0 secrets in runtime log, 0 secrets logged by any statement; default Spring user non-exploitable | **GAP** — 4 production items open |
| Testing | **385 checks, 0 failures** across 5 suites | **PASS** |
| Documentation | 29 files; 8 discrepancies found, all resolved or classified | **PASS** |
| Git / Configuration | `main` at `59cc2a0`, remote synchronised `0 0`, tag `v1.0.0-academic-final`, 0 secrets, wrapper jar committed | **PASS** |
| Final Acceptance | 17 of 17 criteria met | **PASS** |

---

## Repository state

| Field | Value |
|---|---|
| Commit | `59cc2a039c310f7ae4ca09a74bdc250a4b6ea546` |
| Parent | `2c4371be` — GitHub template README, **preserved** |
| Branch | `main` |
| Remote | `https://github.com/HoangKhai1612/expense_management.git` |
| Sync | `0 0` — local matches remote |
| Push | Fast-forward `2c4371b..59cc2a0`. **No force push, no history rewrite** |
| Tag | `v1.0.0-academic-final` |
| Files | 232 in the baseline commit |
| Secrets | **0 committed** |

> **History notice.** Version control was established **after** development.
> Historical development provenance is `UNKNOWN / NOT RECORDED` and cannot be
> reconstructed. This is stated in the commit message, `BASELINE.md` and
> `EFFORT_DATA_GAP.md`.

---

## 1. Requirement status

| Measure | Value |
|---|---|
| Requirement rows traced | 164 |
| Verified by executed test | 148 (90.2%) |
| Partially verified | 8 (4.9%) |
| Gap | 7 (4.3%) |
| Out of scope | 1 (0.6%) |

The 7 gaps are missing *practices*, not missing specified behaviour: external AI
provider path, CI, UI test automation, load testing, rate limiting, TLS, and
image scanning. No gap was promoted to verified without an executed test or a
direct command result.

## 2. Implementation status

| Component | State | Evidence |
|---|---|---|
| Backend | Functional | 78 main source files, 5,500 lines, 16 packages |
| REST API | Functional | 39 paths in the live OpenAPI document |
| Database | Functional | 9 migrations, 11 tables, seed data verified |
| AI assistant | Functional | 7 intents, grounded, refusal paths tested |
| Admin console | Functional | 14 files, 1,635 lines, builds and bundles |
| Android client | Functional, partial | 22 files, 2,668 lines, 6 screens |
| Docker deployment | Functional | 3 images, 3 healthy containers |

No component is partially implemented in a way that breaks its specified
behaviour. The Android client lacks a feedback screen (D-03); every other
documented backend capability has a client path.

## 3. Test status

| Suite | Checks | Failures | Result |
|---|---|---|---|
| Backend unit (Surefire) | 98 | 0 | PASS |
| Backend integration (Failsafe) | 15 | 0 | PASS |
| E2E API assertions | 160 | 0 | PASS |
| Admin contract assertions | 102 | 0 | PASS |
| Android unit | 10 | 0 | PASS |
| **Total** | **385** | **0** | **PASS** |
| Console typecheck + build | 33 modules | 0 errors | PASS |
| `npm audit` | full scan | 0 vulnerabilities | PASS |
| Docker clean deployment | 11 checks | 0 failed | PASS |

The E2E suite passed 8 times across two days, against host and containerised
backends, including against a stack rebuilt from scratch.

**No coverage metric exists** — no JaCoCo or equivalent is configured, so none
is claimed.

## 4. Documentation status

| Document | State |
|---|---|
| `README.md` | Complete: components, quick start, verification, design decisions |
| `docs/architecture.md` | Complete |
| `docs/api-reference.md` | Complete — all 39 endpoints, including `/api/auth/me` |
| `docs/testing.md` | Complete, includes the Android toolchain caveat |
| `docs/deployment.md` | Complete, production checklist honest |
| `docs/acceptance-audit.md` | Complete |
| `docs/known-limitations.md` | Complete, resolved items separated from open ones |
| `docs/REPRODUCIBILITY.md` | Complete |
| `docs/security-audit.md` | Complete |
| `docs/LOGBACK_SECURITY_REVIEW.md` | Complete — closes L-05 |
| `docs/SECRET_SCAN_REPORT.md` | Complete — pre-commit scan |
| `docs/FINAL_DEMO_SCRIPT.md` | Complete, 25 steps |
| `docs/project-management/*` | 17 documents |

28 markdown files under `docs/` (11 top-level + 17 project-management), plus
`README.md`. All 8 documentation discrepancies found during the audits are now
resolved or explicitly classified.

## 5. Deployment status

| Aspect | State |
|---|---|
| Deployment mode | Docker Compose, containerised |
| Startup | Single command, verified from clean state |
| Health | 3 of 3 containers healthy |
| Startup errors | 0 in the backend log |
| Schema initialisation | Automatic, verified on a fresh volume |
| TLS | **Not implemented** |
| Backups | **Not implemented** |
| Orchestration | **Not implemented** — Compose only |
| CI/CD | **Not implemented** |

The deployment works for demonstration and local use. It does not meet
production requirements, and no document claims otherwise.

## 6. Security status

| Area | State |
|---|---|
| Password hashing | BCrypt cost 12 |
| Token security | HS256, required secret, tamper and expiry detection |
| Authorisation | Stateless, admin routes role-protected |
| Data isolation | Owner-scoped queries, non-disclosing 404s |
| SQL injection | Parameterised throughout |
| AI injection | No text-to-SQL path exists |
| Error leakage | Stack traces suppressed |
| Secret management | 17 documented variables, none hardcoded |
| Live secrets in tracked source | **None found** — verified by value search |
| TLS | **Not implemented** |
| Rate limiting | **Not implemented** |
| Log redaction review | **Not performed** |
| Backend/Android dependency scanning | **Not implemented** |
| Penetration testing | **Not performed** |

14 findings in `docs/security-audit.md`. No high-severity finding involves
running code; the two high items are both consequences of the missing repository.

## 7. Known limitations — preserved verbatim in intent

These are **not** hidden anywhere in the documentation and are restated here so
the closure record is complete.

| # | Limitation | Impact |
|---|---|---|
| L-01 | **No CI pipeline** | No automatic verification; depends on discipline |
| L-02 | **No TLS termination** | Blocks public deployment |
| L-03 | **No production backup strategy** | Data loss is unrecoverable |
| L-04 | **No rate limiting** | Brute-force and abuse exposure |
| L-05 | ~~Log-redaction review incomplete~~ | **CLOSED** — review performed, no secret is logged |
| L-06 | No UI test automation | Rendering regressions undetected |
| L-07 | ~~No source control~~ | **CLOSED going forward** — history permanently unavailable |
| L-10 | No load or performance testing | No capacity evidence |
| L-11 | No security scanning or penetration test | Only `npm audit` performed |
| L-12 | Single currency | No multi-currency support |
| L-13 | No refresh tokens | Re-authentication after 12h |
| L-14 | Android token stored unencrypted | Readable on a rooted device |
| L-15 | External AI provider unverified | Only `LOCAL` executed |
| L-16 | No Kubernetes manifests | Compose only |
| L-17 | No log aggregation or alerting | Failures go unnoticed |
| L-18 | No e-mail verification or password reset | No mail transport |
| L-19 | Live admin password is the published demo credential | Configuration-only remedy; disclosed as SEC-01 |

**L-01 through L-04 are the production gaps that remain open.** L-05 was closed
during the finalisation pass. All are stated as open in every relevant document.

## 8. Project management status

| Area | State |
|---|---|
| Scope | Defined and evidenced |
| Schedule | **Not recorded** — no baseline plan exists |
| Effort | **Not recorded** — not reconstructible |
| Quality | 385 checks, all passing |
| Risk | 18 risks scored; 0 high open; 5 occurrences handled |
| Configuration | Strong schema/env control; **source control now established** |
| Communication | **Not recorded** |
| Monitoring | Build-time only |
| Change management | Partial — no code review history possible |
| Acceptance | Evidence-based, 164 rows |
| Closure | This document set |

## 9. Defects

| Defect | Severity | Detection | Status |
|---|---|---|---|
| `AiAnalyst` immutable-list mutation | High — most-used AI intent returned a server error | Code review | Fixed, regression test proven to detect it |
| `Format.kt` duplicate `formatInstant` | Medium — unparsable input rendered as garbage | Unit test | Fixed, 2 assertions added |
| Admin-web healthcheck IPv6 | Medium — health signal could not detect an outage | Container health inspection | Fixed, verified streak 22 → 0 |
| `gradle-wrapper.jar` excluded | **High — fresh clone could not build Android** | `git check-ignore` pre-commit audit | Fixed, jar committed |
| Agent tooling would be committed | Low | Pre-commit enumeration | Fixed, `.gitignore` updated |
| `/api/auth/me` undocumented | Low | Documentation audit | Closed, documented |
| Spring default-user warning | Informational | Security review | Closed, verified non-exploitable |
| Android notification framing | Low | Documentation audit | Closed, corrected |
| Android feedback UI | Low | Documentation audit | Closed by classification, out of scope |
| Android app cannot reach the backend on Android 16+ | Medium — platform blocks traffic to private-range hosts | Final verification pass, 2026-10-02 | Fixed in source; verified by build and unit tests. **Not yet verified on an Android 16 device** |

**5 code defects fixed, 4 documentation/config items closed, 1 further defect found
and fixed during the 2026-10-02 verification pass.**

The tenth item was added after the tagged baseline was established. It is
recorded here so the defect count in the academic report stays traceable to this
document. It could not surface in unit testing, because unit tests do not run on
a real device.

---

## 10. Summary position

The system **functions as specified**, is **verified by 385 automated checks with
zero failures**, is **deployable with a single command from a clean state**, is
**under version control with a tagged verified baseline**, and is **documented
against evidence** rather than intent.

It is **not production-ready**, and four production gaps remain open — CI, TLS,
backups, rate limiting — documented as open in every relevant file. One further
gap, the log-redaction review, was closed during the finalisation pass with no
code change required.