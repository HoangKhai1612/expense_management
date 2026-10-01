# Project management plan

Audited against project evidence. **Only practices the project actually uses are
included.** Where a standard practice is absent, it is listed as a gap rather
than described as if it were in place.

---

## 1. Scope

### In scope

A personal finance tracker with a grounded AI assistant, comprising four
deployable components: a Spring Boot REST API, an Android client, an
administrator web console, and a PostgreSQL database in Docker Compose.

Functional scope, as implemented: authentication and account status, user
profiles, a category catalogue, income and expense transactions, budgets with
threshold alerts, statistics and dashboard aggregation, an AI assistant over the
user's own data, feedback submission, and an administration console with an
audit trail.

### Out of scope — explicitly

Recorded in [`known-limitations.md`](../known-limitations.md) and never claimed
as delivered:

- Automated UI testing
- Continuous integration
- Load and performance testing
- Penetration testing and SAST/DAST
- TLS termination and certificate management
- Production backup and point-in-time recovery
- Rate limiting
- Multi-currency support
- Horizontal scaling and orchestration
- E-mail verification and password reset (no mail transport)

### Scope control evidence

The project resisted scope growth during the final audit. Two candidate additions
were examined against the audit rule "implement only real gaps":

| Candidate | Decision | Reason |
|---|---|---|
| Android feedback screen | **Not implemented** | A real gap (D-03), but adding a screen is an owner scope decision, not an audit fix |
| Coverage reporting (JaCoCo) | **Not implemented** | No coverage figure is claimed anywhere, so there is no inconsistency to repair |

Only one code change was made during the audit: the healthcheck defect (D-01),
which was a genuine false-negative in deployment health signalling.

---

## 2. Schedule

### Honest position

**No baseline schedule exists.** There is no version control history, no issue
tracker and no dated plan. Milestone completion dates were never recorded.

What can be evidenced:

| Fact | Evidence |
|---|---|
| Work was organised into 16 phases | `project-plan.md`, and the code follows that structure |
| 6 milestones, all reached | Module layout matches; all components functional |
| The **verification** phase spanned ≈15 hours wall-clock | CSV timestamps, 2026-09-30 23:37 → 2026-10-01 14:48 |

The 15-hour figure measures testing and documentation only. It is **not** a
project duration and must not be presented as one. See
[`EFFORT_DATA_GAP.md`](EFFORT_DATA_GAP.md).

### Schedule management — GAP

No schedule variance can be computed because there is no plan to compare against.

---

## 3. Resources

| Resource | Actual |
|---|---|
| Toolchain | JDK 21 (backend), JDK 25 (Android daemon), Maven 3.9.9, Node 24.11.0, Gradle 9.6.0, Docker 29.6.2 |
| Database | PostgreSQL 16 in Docker, named volume |
| Developer | **UNKNOWN / NOT RECORDED** — no authorship evidence exists |
| Team size | **UNKNOWN / NOT RECORDED** |
| Budget/licensing | All components are open-source; no commercial licence identified |

---

## 4. Quality management

Fully specified in [`QUALITY_PLAN.md`](QUALITY_PLAN.md). Summary of what is
actually practised:

| Practice | In use | Evidence |
|---|---|---|
| Unit testing | Yes | 98 tests, 7 classes |
| Integration testing with real DB | Yes | 15 tests via Testcontainers |
| API contract testing | Yes | 102 assertions |
| End-to-end testing | Yes | 160 assertions |
| Android unit testing | Yes | 10 tests |
| Static typing (backend) | Yes | Java, compile-time |
| Static typing (frontend) | Yes | `tsc --noEmit` in build |
| Dependency scanning | Yes, frontend only | `npm audit` → 0 |
| Code coverage measurement | **No** | No plugin configured |
| Mutation testing | **No** | Not configured |
| Test lifecycle separation | Yes | Surefire/Failsafe split |

The Surefire/Failsafe split is a genuine quality decision: it lets a developer
without Docker run 98 tests instead of being blocked entirely.

---

## 5. Risk management

Fully specified in [`RISK_AUDIT.md`](RISK_AUDIT.md). Each of the 15 original
risks was re-verified against evidence, with no score changed without cause.

| Measure | Value |
|---|---|
| Risks in register | 17 (15 original + 2 added by audit) |
| High (≥15) open | **0** |
| Medium open | 6 |
| Occurred and fixed | 3 |
| Retired (resolved) | 5 |

The risk process is evidenced by real artefacts: a scored register, documented
mitigations, and recorded occurrences — including two defects found during the
project and one found by the audit itself.

---

## 6. Configuration management

Fully specified in [`CONFIGURATION_BASELINE.md`](CONFIGURATION_BASELINE.md).

| Element | State |
|---|---|
| Version control | **Not in use** — the single most significant PM gap |
| Schema versioning | Strong — 9 Flyway migrations, checksummed, drift-detected |
| Environment configuration | Strong — 17 documented variables, no secrets in code |
| Image versioning | Adequate — fixed tags, no `latest` |
| Release packaging | Absent — no versioned release artifacts |

---

## 7. Communication

**UNKNOWN / NOT RECORDED.** There are no meeting notes, no design-review
documents, no correspondence and no issue discussions in the project. The
documentation set in `docs/` is the only durable communication artefact.

No communication practice is claimed.

---

## 8. Monitoring and control

### Build-time monitoring — IN USE

| Control | Evidence |
|---|---|
| Database health gate | `depends_on: condition: service_healthy` |
| Container health checks | All 3 containers report health |
| Schema drift detection | `ddl-auto: validate` fails startup |
| Application health endpoint | `/actuator/health` |
| Actuator metrics | `health`, `info`, `metrics` exposed |

### Runtime monitoring — ABSENT

| Missing | Impact |
|---|---|
| Log aggregation | Logs go to stdout, readable only via `docker compose logs` |
| Metrics scraping | Actuator exposes metrics; nothing collects them |
| Alerting | No alerts on failure, saturation or anomaly |
| Tracing | None |
| Uptime measurement | Cannot be computed |

Monitoring exists only at container and startup level. This is sufficient for a
demonstration and insufficient for production, and is not claimed as more than
that.

### Progress monitoring — PARTIAL

| Measure | Available |
|---|---|
| Test pass/fail | Yes |
| Defects found and fixed | Yes, 3 |
| Container health | Yes |
| Build result | Yes |
| Requirement coverage | Yes, 164 traced rows |
| Task completion | **No** — no task system exists |
| Schedule adherence | **No** — no schedule exists |
| Effort consumption | **No** — not recorded |

---

## 9. Change management

### Controls actually in place

| Control | Evidence |
|---|---|
| Schema changes only via migration | `ddl-auto: validate` would reject drift |
| Migration ordering enforced | Numeric `V1`–`V9` prefixes, applied in order |
| Migration checksums detect tampering | `flyway_schema_history` |
| New data reuses an existing row rather than overwriting | Bootstrap admin not overwritten on restart |
| Configuration externalised | No hardcoded secrets or URLs |
| Defect fix accompanied by a regression test | 3 of 3 defects |

### Change controls absent

- No branch strategy (no repository)
- No code review (no pull requests possible)
- No CI gate
- No release approval process
- No change log or version history

The absence of branching and review is a direct consequence of the missing
repository and is the most consequential PM gap after the missing repository
itself.

---

## 10. Acceptance

Fully specified in
[`REQUIREMENT_TRACEABILITY.md`](REQUIREMENT_TRACEABILITY.md) and
[`FINAL_TEST_EVIDENCE.md`](FINAL_TEST_EVIDENCE.md).

| Measure | Value |
|---|---|
| Requirement rows traced | 164 |
| Verified | 148 |
| Partially verified | 8 |
| Gap | 7 |
| Out of scope | 1 |
| Acceptance tests executed | 385 checks, 0 failures |

Acceptance was evidence-driven: every verified row names the test that proves
it and the file that recorded the result.

---

## 11. Closure

Fully specified in [`PROJECT_CLOSURE.md`](PROJECT_CLOSURE.md).

Closure deliverables actually produced: final baseline, discrepancy report,
traceability matrix, quality plan, risk audit, metrics, configuration baseline,
test evidence, project health status and this plan.

---

## 12. Summary of PM maturity — factual

Assessed only against practices with evidence in this repository.

| Area | State |
|---|---|
| Scope definition | **Defined and evidenced** — in/out of scope documented |
| Schedule management | **Absent** — no plan or history |
| Effort estimation | **Absent** — not recorded |
| Quality assurance | **Strong** — 5 automated suites, 385 checks, all passing |
| Risk management | **Established** — scored register, verified, 3 real occurrences handled |
| Configuration management | **Partial** — strong schema/env control and a Git repository with a tagged baseline; no pre-commit history exists |
| Communication | **Not recorded** |
| Monitoring | **Build-time only** |
| Change management | **Partial** — schema/config strong, code change controls absent |
| Acceptance | **Evidence-based** — 164 rows traced |
| Closure | **This document set** |

No rating such as "excellent" or "mature" is assigned, because no objective
scoring rubric was defined before the fact and inventing one would produce a
number without a definition.