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
**L3 × I4 = 12 (Medium)** · **Status: OPEN, confirmed unreviewed**

The appender uses the Spring Boot default pattern. The final audit scanned the
backend log for ERROR lines and found none, but **that is not a redaction
review**: it says nothing about whether a request body or token could appear in
a log line at INFO level.

### R-10 — No CI, so a failing change can land
**L3 × I4 = 12 (Medium)** · **Status: OPEN, confirmed**

No `.github/workflows`, no Jenkinsfile, no GitLab CI configuration exists.
Verified by inspection. All five suites are scripted and re-runnable, which
makes CI straightforward to add, but it does not exist.

---

## Risks added by this audit

### R-15 — No source control repository *(new)*
**L3 × I4 = 12 (Medium)** · **Status: OPEN**

Discovered by this audit. No `.git` directory exists, so there is no commit
history, no authorship, no branch or tag, and no regression history proving the
three fixed defects were the only ones. Evidence: `git rev-parse` →
`fatal: not a git repository`.

Deliberately **not** remediated: initialising Git now would create a single
initial commit representing the finished system, which would misrepresent
history rather than record it.

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

---

## Current position

| Status | Count |
|---|---|
| High (≥15) open | **0** |
| Medium open | 6 (R-06, R-10, R-12, R-13, R-15, R-17) |
| Low open | 2 (R-16 is closed; R-17 counted above) |
| Occurred and fixed | 3 (R-01, R-16, plus the `Format.kt` duplicate) |

No risk scores were changed in this audit. The only status changes are R-04 and
R-08, whose corrections were made earlier and are upheld here on the strength of
passing E2E assertions.