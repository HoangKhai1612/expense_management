# Risk register — audited

Each risk from the original register was re-checked against the final audit
evidence. **No risk was downgraded or upgraded without a specific test or
measurement**, as required.

Scoring: Likelihood (1–5) × Impact (1–5). Scores ≥ 15 are high.

---

## Verified risks

### R-01 — AI produces an ungrounded figure
**L3 × I5 = 15 (High)** · **Status: MITIGATED, controls verified**

| Field | Finding |
|---|---|
| Original mitigation | Snapshot service with no text-to-SQL; per-intent fact tests; `AiGroundingIT` asserts values not wording |
| Verification | `AiGroundingIT` 8 tests PASS. E2E asserts `aiFact=totalExpense=3600000.00` equals `dbTotalExpense=3600000.00`. Deliberate defect injection produced 4 failures, proving the test detects the real defect |
| Actual occurrence | **OCCURRED once and was fixed.** `AiAnalyst.spending()` mutated an immutable `List.of(...)`, throwing `UnsupportedOperationException`. The most-used intent was broken in production code |
| Response | Fixed with `new ArrayList<>(...)`; regression test added and proven to fail against the original code |
| Evidence | Failsafe report; `e2e-20261001-144857.csv`; defect section of `PROJECT_CLOSURE.md` |
| Residual risk | Acceptable for the implemented intents. The external provider path remains unexercised |

### R-02 — Cross-user data leakage
**L2 × I5 = 10 (Medium)** · **Status: MITIGATED**

| Field | Finding |
|---|---|
| Verification | 7 E2E assertions pass: read, update, delete, list scoping, dashboard totals, AI transcript, feedback — all return 404 |
| Actual occurrence | None detected |
| Evidence | `e2e-20261001-144857.csv` |
| Residual risk | 7 paths probed, not exhaustively enumerated. `CategoryServiceTest` additionally covers the category disclosure case |

### R-03 — Category deletion breaks transaction history
**L4 × I4 = 16 (High)** · **Status: AVOIDED BY DESIGN**

| Field | Finding |
|---|---|
| Verification | E2E "Existing transactions survive category deactivation" PASS — 200 and data readable after deactivation |
| Actual occurrence | None |
| Evidence | e2e CSV; `CategoryServiceTest` |
| Residual risk | None material. Soft-deleted rows are never purged, which is a separate accepted trade-off |

### R-04 — Locked user retains access via a live token
**L3 × I4 = 12 (Medium)** · **Status: MITIGATED — correction upheld**

| Field | Finding |
|---|---|
| Original correction | Previously downgraded from "no dedicated E2E case" to "covered" |
| Verification | E2E assertion "Pre-lock token is rejected after locking" returns **401**, and this assertion passed in the final run |
| Actual occurrence | None |
| Evidence | `e2e-20261001-144857.csv` |
| Note | The earlier documentation stating no such test existed was itself inaccurate. The test is present and passing; the correction stands |

### R-05 — Migration fails on a partially migrated database
**L2 × I5 = 10 (Medium)** · **Status: MITIGATED**

| Field | Finding |
|---|---|
| Verification | `MigrationsAndBootstrapIT` 7 tests PASS against a real PostgreSQL 16 container. Clean-state rebuild applied 9 migrations to a fresh volume with 11 tables |
| Actual occurrence | None |
| Evidence | Failsafe report; `flyway_schema_history` query |

### R-08 — Budget alert spam
**L3 × I3 = 9 (Medium)** · **Status: MITIGATED — correction upheld**

| Field | Finding |
|---|---|
| Original correction | Previously "partly mitigated — no test for a repeat alert" |
| Verification | E2E asserts "Warning alert is not duplicated" — count stays at **1** across a recomputation. Separately, exactly 1 critical notification at 120% |
| Actual occurrence | None |
| Evidence | `e2e-20261001-144857.csv` |
| Note | Correction upheld: the assertion exists and passes |

### R-12 — No backups, data loss unrecoverable
**L2 × I5 = 10 (Medium)** · **Status: OPEN, unchanged**

No mitigation exists. No dump, restore or point-in-time-recovery capability is
implemented or tested. Confirmed by inspection: no backup script, no cron job,
no backup section in the Compose file.

### R-13 — No rate limiting on login or AI chat
**L3 × I4 = 12 (Medium)** · **Status: OPEN, unchanged**

No rate-limiting dependency, filter or configuration exists. Acceptable for
local and demo use; blocks public deployment.

### R-06 — Secret leakage via logs
**L3 × I4 = 12 (Medium)** · **Status: RETIRED 2026-10-01 — review performed, no secret logged**

Originally opened as OPEN, confirmed unreviewed: scanning the backend log for
ERROR lines is not a redaction review, because it says nothing about whether a
request body or token could appear at INFO level.

The review has now been carried out. All 20 backend log statements were
enumerated and inspected; a search confirmed that no log call references a
password, token, secret, API key or `Authorization` header; and the live runtime
log was searched for the actual `.env` secret values with zero matches. Spring
Boot's generated default security user was tested and returns 401 on two
endpoints, so it is not exploitable.

Full analysis, method and five residual hardening recommendations:
[`../LOGBACK_SECURITY_REVIEW.md`](../LOGBACK_SECURITY_REVIEW.md).

### R-07 — Secrets committed to the repository
**L1 × I5 = 5 (Low)** · **Status: CONTROLLED**

The risk was that a `.env` file or a live credential would be committed and
become part of the permanent history. It is controlled by exclusion rules plus a
pre-commit scan, not by care.

Control: `.gitignore` excludes `.env` and the environment files; a scan of every
tracked file for credential patterns returns 0 hits. The baseline commit carries
0 secrets.

This entry was absent from this file during the pre-submission audit of
2026-10-02: the register ran R-01 to R-06 and then R-08, so R-07 was never
written up even though the risk was tracked in the report's own register. It is
restored here with the same likelihood, impact and status already recorded there.
The gap is a documentation omission, not a change in the risk position.

### R-10 — No CI, so a failing change can land
**L3 × I4 = 12 (Medium)** · **Status: OPEN, confirmed**

No `.github/workflows`, no Jenkinsfile, no GitLab CI configuration exists.
Verified by inspection. All five suites are scripted and re-runnable, which
makes CI straightforward to add, but it does not exist.

---

## Risks added by this audit

### R-15 — Historical development provenance unavailable
**L3 × I4 = 12 (Medium)** · **Status: MITIGATED FOR FUTURE DEVELOPMENT**

| Field | Finding |
|---|---|
| Original risk | No version control, so no commit history, authorship, branch or tag record |
| Mitigation applied | Repository initialised and reconciled with the provided GitHub remote. Baseline commit `59cc2a0` on `main`, remote synchronised (fast-forward), release tag `v1.0.0-academic-final` created. Parent commit `2c4371b` preserved unmodified — no force push, no history rewrite |
| Verification | `git rev-list --left-right --count origin/main...main` → `0 0`. `git ls-remote origin` shows `59cc2a0`. Commit graph: `59cc2a0` → `2c4371b` |
| Actual occurrence | Confirmed — the project genuinely was never versioned during development |
| Response | Version control established and the absence of history disclosed explicitly in the commit message, `BASELINE.md` and `EFFORT_DATA_GAP.md` |
| **Status** | **Mitigated for future development** |
| **Explicit caveat** | **Historical provenance is NOT restored.** Development-period commits, authorship, schedule and effort remain `UNKNOWN / NOT RECORDED` and cannot be reconstructed. This risk is closed going forward only |
| Evidence | `CONFIGURATION_BASELINE.md` section 1; `docs/SECRET_SCAN_REPORT.md`; the baseline commit message itself |

### R-18 — Gradle wrapper jar excluded from version control *(new, FIXED)*
**L2 × I5 = 10 (Medium)** · **Status: OCCURRED AND FIXED**

| Field | Finding |
|---|---|
| Defect | `.gitignore` had a blanket `*.jar` rule that excluded `android/gradle/wrapper/gradle-wrapper.jar` |
| Impact | A fresh `git clone` could not build the Android project — `./gradlew` would fail with `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` |
| Detection | `git check-ignore -v` during the pre-commit audit. **No test in the project could have caught this**, because local builds used the jar already present on disk |
| Fix | Added `!android/gradle/wrapper/gradle-wrapper.jar` to `.gitignore` |
| Verification | Jar confirmed staged and present in the baseline commit |
| Status | **CLOSED** |
| Lesson | A working local directory can mask a repository defect. Reproducibility must be proven from a clean clone |

### R-16 — Deployment health signal was false-negative *(new, now closed)*
**L2 × I3 = 6 (Low)** · **Status: OCCURRED AND FIXED**

The `admin-web` healthcheck probed `http://localhost/`, which resolves to IPv6
`::1` inside the container while nginx binds IPv4 only. Every probe failed, so
the container reported `unhealthy` with a failing streak of 22 while serving
traffic normally. This meant the health signal could not distinguish a healthy
service from a broken one.

Fixed by probing `127.0.0.1`. Container now reports `healthy`, streak 0. The
incident is preserved because it demonstrates why `docker compose ps` health
state must be inspected directly rather than inferred from passing HTTP tests.

### R-17 — Documentation drift *(new, partially open)*
**L3 × I2 = 6 (Low)** · **Status: OPEN**

Three discrepancies found: `/api/auth/me` undocumented (D-04), Android
notification coverage overstated as a screen (D-05), and Android feedback UI
missing (D-03). Two remain open. Documentation was written against intent
rather than generated from the running system, which is the root cause.

---

## Accepted, with rationale

| Risk | Score | Rationale for acceptance |
|---|---|---|
| R-09 Unbounded page size | 6 | Page size is validated. Acceptable at this scale |
| R-14 UTC period boundary | 6 | A single-user system; documented behaviour |
| R-11 External provider unverified | 6 | `LOCAL` is the supported mode; the alternative is documented as unverified |
| R-15b Android token in plain storage | 9 | Debug build only; release hardening is out of academic scope |

---

## Retired risks

| Risk | Resolution |
|---|---|
| Immutable-list mutation broke the spending analyst | Fixed; regression test proven to fail against the original defect |
| E2E suite not re-runnable | Made idempotent; passed 5+ times consecutively including against a clean rebuild |
| Admin console types could drift from the API | Contract suite added; 102/102 |
| Admin console did not exist | Built; typechecks, bundles, containerises |
| Healthcheck could not detect a real outage (R-16) | Probe corrected to IPv4 loopback |
| Secret leakage via logs (R-06) | Review performed: 0 secrets logged. `docs/LOGBACK_SECURITY_REVIEW.md` |
| Agent tooling would be committed | `.kilo/`, `.claude/`, `.cursor/` added to `.gitignore` |
| Fresh clone could not build Android (R-18) | Wrapper jar explicitly un-ignored and committed |
| No version control (R-15) | Repository, remote, baseline commit, tag. History permanently unavailable |

---

## Current position

| Status | Count |
|---|---|
| High (≥15) open | **0** |
| Medium open | 4 (R-06 closed this pass, R-10, R-12, R-13, R-17) |
| Occurred and fixed | 5 (R-01, R-16, R-18, plus the `Format.kt` duplicate) |
| Closed this pass | R-05 previously verified; **R-06 (log redaction) and R-15 (source control) now closed** |

No risk score was changed in this pass. Status changes are R-06 (closed by
`docs/LOGBACK_SECURITY_REVIEW.md`) and R-15 (closed going forward by the
repository baseline, with the historical caveat stated explicitly).

### Still open

| Risk | Score | Why still open |
|---|---|---|
| R-10 No CI | 12 | No pipeline exists; all verification is manual |
| R-12 No backups | 10 | No dump, restore or recovery capability |
| R-13 No rate limiting | 12 | No implementation |
| R-17 Documentation drift | 6 | Two minor items remain (D-05 wording is fixed; D-06 test coverage open) |

The four **production** gaps — CI, backups, rate limiting and TLS — remain open
and are stated as open in every relevant document.