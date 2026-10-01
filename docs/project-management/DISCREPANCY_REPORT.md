# Documentation versus implementation discrepancy list

Method: every documented claim was checked against the source tree, the running
containers, or the live API. Claims that could not be verified are marked
`UNVERIFIABLE` rather than assumed correct.

Severity scale:

- **HIGH** — a documented guarantee is false, or a stated defect exists in
  production code.
- **MEDIUM** — documentation is incomplete or inaccurate but the system behaves
  as intended.
- **LOW** — cosmetic or wording only.

---

## D-01 — `admin-web` healthcheck was permanently failing (FIXED)

| Field | Value |
|---|---|
| **Claim** | `docs/deployment.md` and the project status reported "3 containers healthy" |
| **Actual implementation** | `admin-web/Dockerfile` probed `http://localhost/`. nginx binds IPv4 only (`0.0.0.0:80`), and inside the container `localhost` resolves to `::1` first, so every probe returned `Connection refused` |
| **Evidence** | `docker compose ps` showed `finai-admin-web ... Up (unhealthy)`, `FailingStreak: 22`. Direct probe from inside the container: `wget http://localhost/` → FAIL, `wget http://127.0.0.1/` → OK. From the host, `GET http://localhost:5173` returned **200** |
| **Severity** | **HIGH** — the deployment was documented as healthy while reporting unhealthy, and the health signal was worthless for detecting a real outage |
| **Action** | **FIXED.** Probe changed to `http://127.0.0.1/` with a comment explaining why. Rebuilt; container now reports `healthy`, `FailingStreak: 0` |
| **Lesson** | A passing HTTP test against the host port did not detect this, because the service was genuinely serving traffic. Only `docker compose ps` health state exposed it. The earlier claim of "3 healthy" was wrong and is corrected in this baseline |

## D-02 — No source control repository existed *(RESOLVED with a preserved-history caveat)*

| Field | Value |
|---|---|
| **Claim** | Source control is expected as part of the course's planning-infrastructure requirement; the project previously had no stated position either way |
| **Actual implementation** | At the start of this finalisation pass there was no `.git` directory in the project or any parent. `git rev-parse` returned `fatal: not a git repository`. Git 2.55.0 was installed but not on `PATH`, and the repository had never been initialised |
| **Evidence** | `git rev-parse --is-inside-work-tree` → `fatal: not a git repository (or any of the parent directories): .git`. A `.gitignore` existed, showing intent, but nothing was under version control |
| **Severity** | **HIGH** — no commit history, no authorship, no branch or tag record, and no change traceability |
| **Action** | **RESOLVED 2026-10-01, with the historical gap permanently closed as unrecoverable.** See below |
| **Status** | Closed going forward; historical provenance remains `UNKNOWN / NOT RECORDED` |

### Resolution

A GitHub repository was provided: `HoangKhai1612/expense_management`. It was
probed **before** any local change:

```
git ls-remote https://github.com/HoangKhai1612/expense_management.git
2c4371be5e69446fdb26979e2eb4ae2b505bb308  HEAD
2c4371be5e69446fdb26979e2eb4ae2b505bb308  refs/heads/main
```

That commit is a single "Initial commit" dated 2026-10-01 15:09 local, created by
GitHub's repository template, containing only a 2-line `README.md`:

```
# expense_management
App Mobile Expense Management
```

The safe reconciliation sequence was used, with **no force push and no history
rewrite**:

1. `git init` — creates local repository only
2. `git remote add origin <url>`
3. `git fetch origin` — obtains the real remote history
4. `git symbolic-ref HEAD refs/heads/main`
5. `git update-ref refs/heads/main 2c4371be…` — points local `main` at the remote
   commit **without touching the working tree**
6. `git reset` — aligns the index with HEAD

The remote commit therefore became the **parent** of the baseline commit rather
than being replaced. The project was committed on top of it.

### What this does and does not establish

| Established | Not established |
|---|---|
| The project is now under version control | Any development history before today |
| The remote's original commit is preserved intact | When the code was written |
| A reproducible baseline exists going forward | How long development took |
| A tagged release point exists | Who wrote what, or when |
| Future change can be reviewed | Any historical productivity measure |

**Historical development provenance is `UNKNOWN / NOT RECORDED` and cannot be
recovered.** This is permanent. The baseline commit states this explicitly so no
reader infers a development history that does not exist.

## D-03 — Android has no feedback screen (specification gap)

| Field | Value |
|---|---|
| **Claim** | The mobile client was described as covering feedback submission; `docs/api-reference.md` documents `POST /api/feedback` as part of the product |
| **Actual implementation** | `FinanceApi.kt` declares the three feedback endpoints, but `FinanceRepository` has **no** feedback method and **no** screen references feedback at all. The 6 screens are Login, Dashboard, Transactions, Budgets, Assistant, Profile |
| **Evidence** | Grep for `Feedback` across all screen files returned zero matches. Repository exposes 17 methods, none for feedback |
| **Severity** | **MEDIUM** — dead API declarations in the mobile client; the feedback feature is reachable only from the web/API side. Backend support is complete and E2E-tested |
| **Action** | **Open — deliberately not implemented.** Section 14 of the audit brief requires implementing only real requirement gaps; this is one, but adding a screen is a scope decision for the owner. Recorded as limitation L-08 |
| **Status** | Closed by classification, not by code |

### Why this is classified out of scope rather than implemented

The original specification document is **not present in this repository**, so
whether a mobile feedback screen is mandatory cannot be verified from evidence.
Three findings support the out-of-scope classification:

1. **The capability itself is delivered.** The backend implements submission,
   listing and detail retrieval with validation, and the admin console has a full
   triage UI (`FeedbackPage.tsx`). Feedback is demonstrable end to end without a
   mobile screen.
2. **The mobile client is complete for every financial feature** — dashboard,
   transactions, budgets, AI chat, notifications, profile. Feedback is the only
   omission and is not a core financial capability.
3. **The audit rule prohibits speculative additions.** Adding a screen to improve
   the appearance of a traceability matrix is precisely the kind of change that
   rule forbids, and it would require a full re-verification cycle.

The API declarations remain in `FinanceApi.kt`, so the mobile client is
pre-wired: adding the screen later is a UI plus repository-method task with no
backend change.

**Owner decision:** if the specification does mandate a mobile feedback screen,
this classification must be reversed and the feature implemented. Confirming
against the specification is recommended before the report is submitted.

## D-04 — `/api/auth/me` is undocumented

| Field | Value |
|---|---|
| **Claim** | `docs/api-reference.md` lists `GET /api/users/me` under Profile and the auth section lists only register and login |
| **Actual implementation** | `AuthController` also exposes `@GetMapping("/me")` returning `AuthResponse.UserSummary`, i.e. `GET /api/auth/me` |
| **Evidence** | Live `/v3/api-docs` lists `/api/auth/me`; `AuthController.java` declares `@GetMapping("/me")`; no mention in `docs/api-reference.md` |
| **Severity** | **LOW** — an undocumented but harmless endpoint, public within the authenticated surface |
| **Action** | **CLOSED 2026-10-01.** Fully documented in `docs/api-reference.md` under a dedicated "GET /api/auth/me" section, with method, path, authentication requirement, request shape, a 200 response captured from the running system, the error table, ownership/security behaviour, the distinction from `/api/users/me`, and a note that it has no dedicated test assertion. Behaviour was **not** changed — the endpoint was already correct |
| **Status** | CLOSED |

## D-05 — Android notification coverage is narrower than the docs implied

| Field | Value |
|---|---|
| **Claim** | Status report described a notifications screen |
| **Actual implementation** | Notifications are rendered inside `ProfileScreen` (`state.notifications.forEach`), not on a dedicated screen. The repository exposes only `unreadNotifications()` and `markAllNotificationsRead()`; there is no paged-list or mark-one-read method |
| **Evidence** | `ProfileScreen.kt` contains the notification list and mark-all button. `FinanceRepository` has 17 methods, 2 of them notification-related |
| **Severity** | **LOW** — the capability exists and is usable; the naming in earlier status text overstated it as a screen |
| **Action** | **CLOSED 2026-10-01.** Every document now describes notifications as rendered within Profile. The capability itself was always real; only the screen framing was wrong |
| **Status** | CLOSED |

## D-06 — `/api/auth/me` had no test assertion

| Field | Value |
|---|---|
| **Claim** | Traced as `PARTIAL` — implemented and manually verified, but no automated assertion |
| **Actual implementation** | The endpoint is reachable, returns the caller's `UserSummary`, and rejects anonymous callers with 401. All three verified manually against the live container |
| **Evidence** | `GET /api/auth/me` returned the admin `UserSummary`; unauthenticated call returned **401** |
| **Severity** | **LOW** — the endpoint is correct; the gap is test coverage, not behaviour |
| **Action** | **Open.** Documented and manually verified, but no E2E or contract assertion was added. Adding one would change the assertion count, which this audit is instructed to report as actually measured rather than to inflate. Traced as `PARTIAL` |
| **Status** | Open (test coverage only) |

## D-07 — Generated Spring Security password warning

| Field | Value |
|---|---|
| **Claim** | Prior audits did not examine the startup warning `Using generated security password: <uuid>` |
| **Actual implementation** | Spring Boot's `UserDetailsServiceAutoConfiguration` creates a default in-memory `user`. Whether that is exploitable depends on whether the security chain accepts basic or form credentials |
| **Evidence** | `GET /api/admin/dashboard` with Basic `user:<generated-password>` → **401**. `GET /api/transactions` likewise → **401** |
| **Severity** | **Informational** — the default account exists but cannot authenticate, because `SecurityConfig` never enables `httpBasic()` or `formLogin()` |
| **Action** | **CLOSED as verified non-exploitable.** No code change. Suppressing the warning would require excluding an auto-configuration, which is a behavioural change with more risk than the cosmetic warning carries. Recorded in `docs/LOGBACK_SECURITY_REVIEW.md` section 6 |
| **Status** | CLOSED |

## D-08 — Live admin password equals the published demo credential

| Field | Value |
|---|---|
| **Claim** | Prior audits recorded development credentials as documented (S-09) but did not test the live `.env` against the documented value |
| **Actual implementation** | The `APP_ADMIN_PASSWORD` in the live `.env` is byte-identical to the demo credential published in `docs/api-reference.md` and the two test scripts |
| **Evidence** | Secret scan compared live `.env` values against the commit set; `APP_ADMIN_PASSWORD` matched in 7 files, all documentation or test fixtures |
| **Severity** | **MEDIUM — WARNING** |
| **Action** | **Accepted and disclosed.** Rotating it would break the reproducibility the documentation promises. Remedy is configuration-only for any non-demo deployment: set a unique `APP_ADMIN_PASSWORD` and `JWT_SECRET`. Recorded as SEC-01 in `docs/SECRET_SCAN_REPORT.md` |
| **Status** | Accepted, disclosed |

---

## Claims checked and found accurate

| Claim | Verification method | Result |
|---|---|---|
| 9 Flyway migrations, 11 tables | `psql` against the running container | Accurate |
| BCrypt hashing with cost 12 | `SecurityConfig.passwordEncoder()` | Accurate |
| Stateless JWT, `/api/admin/**` requires `ROLE_ADMIN` | `SecurityConfig` matchers | Accurate |
| Nulls omitted, not serialised as `null` | `spring.jackson.default-property-inclusion: non_null`; contract suite asserts absence of explicit nulls | Accurate |
| 7 AI intents | `AiIntent` enum, matches documented list exactly | Accurate |
| Actuator exposes only health, info, metrics | `application.yml` exposure include list | Accurate |
| Swagger UI reachable unauthenticated | `GET /swagger-ui.html` → 200; matcher present | Accurate |
| No password hash in any migration file | Grep across V1–V9 | Accurate |
| No `@Scheduled` jobs | Count returned 0 | Accurate |

## Claims that could not be verified

| Claim | Reason |
|---|---|
| Any statement about development timeline, effort, or team activity | No version control, no issue tracker, no dated artifacts. Marked `UNKNOWN / NOT RECORDED` |
| Sprint/iteration count | No Agile artifacts exist in the repository |
| Code review history | No pull requests, review comments or peer-review records exist |