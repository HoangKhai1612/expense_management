# Final demo script

A realistic business scenario run against the final system. Every step was
verified to work during the final audit.

**Preparation (before the demo)**

```bash
docker compose up -d --build
docker compose ps          # expect 3 healthy
```

Have open: <http://localhost:8081/swagger-ui.html>, an Android emulator, and
terminal windows for the test commands in step 21.

**Accounts used in the demo**

| Role | Identifier | Password |
|---|---|---|
| Administrator | `admin@finai.local` | from `APP_ADMIN_PASSWORD` in `.env` |
| Demo user | registered in step 3 | chosen at registration |

---

## Part A — Infrastructure

### 1. Start the stack

```bash
docker compose up -d --build
```

### 2. Verify services

```bash
docker compose ps
```

**Expected:** `finai-postgres` healthy, `finai-backend` healthy,
`finai-admin-web` healthy, on ports 5433, 8081 and 5173.

```bash
curl http://localhost:8081/actuator/health
```

**Expected:** `{"status":"UP","groups":["liveness","readiness"]}`

---

## Part B — Android client

### 3. Launch and register

Open the app on the emulator. The base URL is already `10.0.2.2:8081`, which is
how the emulator reaches the host.

Tap **Create account**, enter an email, username and a password meeting the
policy (8+ chars, upper, lower, digit). Submit.

### 4. Log in

Tap **Sign in** with the same credentials. The dashboard appears.

### 5. Add income

**Add income** → category *Salary* → amount `15000000` → any past date → save.

### 6. Add expenses

Add several, so the AI has something to analyse:

| Category | Amount | Note |
|---|---|---|
| Food & Drink | `1200000` | Groceries |
| Food & Drink | `2400000` | Restaurant |
| Transport | `450000` | Fuel |
| Shopping | `900000` | Shoes |

### 7. Create a budget

**Budgets** → new → category *Food & Drink* → limit `3000000` → monthly →
save.

**Expected:** usage shows 3,600,000 of 3,000,000 — **120%**, status **EXCEEDED**,
remaining **−600,000**.

### 8. Check notifications

Open the notification list in Profile.

**Expected:** a critical budget alert for Food & Drink. This demonstrates the
threshold system firing on real data.

### 9. Verify the dashboard

**Expected:** month income 15,000,000; month expense 4,950,000 (1,200,000 +
2,400,000 + 450,000 + 900,000); month balance 10,050,000; recent transactions
listed.

*The figures above were computed from the demo's own data so they can be
cross-checked live against the dashboard.*

---

## Part C — The AI assistant

### 10. Ask about spending

**Assistant** → "This month, where did I spend the most?"

**Expected:** a grounded answer, split into two sections:

```
FACT - You spend the most on "Food & Drink" in <month>.
- Total expenses that month were 3.600.000 VND ...
SUGGESTION - ...
```

Cross-check the figure against step 9. They must match — this is the point of
the demonstration.

### 11. Ask something it cannot answer

**Assistant** → "What is the weather tomorrow?"

**Expected:** a refusal, not an invention. No fabricated number.

### 12. Show an empty-baseline refusal *(optional)*

**Assistant** → "Compare this month with last month."

**Expected:** it declines, because last month has no data.

**The narration worth delivering:** the assistant has no text-to-SQL path. It
computes a typed snapshot of your rows and reasons only over that. It cannot be
tricked into reading another account, because it cannot express a query at all.

---

## Part D — Feedback

### 13. Submit feedback

**Profile** → feedback → category *UI* → describe an observation → submit.

**Expected:** the ticket appears in your own list with status `OPEN`.

---

## Part E — Admin console

### 14. Open the console

<http://localhost:5173>

### 15. Log in as administrator

Use `admin@finai.local` and the admin password. The dashboard shows system
metrics: total users, transactions, AI question count, feedback by status, and
runtime figures labelled as since-restart.

### 16. Inspect the user

**Users** → search for the demo user.

**Expected:** the row shows role `USER`, status `ACTIVE` and the transaction
count from steps 5–6.

### 17. Inspect the feedback ticket

**Feedback** → open the ticket from step 13.

### 18. Triage it

Change the status to `RESOLVED` and write a reply.

**Expected:** the admin action is written to the audit trail immediately.

### 19. Lock the user

**Users** → the demo user → set status `LOCKED` with a reason.

**Expected:** status becomes `LOCKED`, and the action is audited.

### 20. Verify the lock

Switch to the Android app, which is still open and holding a token.

**Expected:** the app is rejected and returns you to the login screen. The
pre-lock token no longer works.

Try logging in with those credentials: rejected with **account locked**.

Then, as admin, **unlock** the user.

Log in on Android again: **it works**.

**The narration worth delivering:** locking invalidates existing tokens
immediately rather than waiting for them to expire, because the account status
is re-checked on every request.

---

## Part F — Evidence

### 21. Show the test results

```bash
cd backend && mvn verify
```

**Expected:** `Tests run: 98` then `Tests run: 15`, **BUILD SUCCESS**.

```bash
powershell -ExecutionPolicy Bypass -File tests/e2e-api-tests.ps1
```

**Expected:** `passed: 160`, `failed: 0`.

```bash
powershell -ExecutionPolicy Bypass -File tests/admin-web-contract.ps1
```

**Expected:** `passed: 102`, `failed: 0`.

### 22. Show the database

```bash
docker compose exec -T postgres psql -U finai -d finai \
  -c "select version, description, success from flyway_schema_history order by installed_rank;"
```

**Expected:** 9 rows, all `success = t`.

### 23. Show the deployment

```bash
docker compose ps
docker images | grep personal-finance
```

**Expected:** 3 containers healthy; 2 project images plus PostgreSQL.

### 24. Show the documentation

Point at:

- `README.md` — overview and quick start
- `docs/architecture.md` — module layout and why the AI has no text-to-SQL
- `docs/acceptance-audit.md` — what is verified, with evidence
- `docs/known-limitations.md` — what is **not** done
- `docs/security-audit.md` — findings by classification

### 25. Close honestly

State the production gaps plainly rather than skipping them:

- No CI pipeline
- No TLS termination
- No production backup strategy
- No rate limiting
- Log-redaction review not completed

These are documented in `docs/known-limitations.md`. Claiming production
readiness would be inaccurate.

---

## Demo failure recovery

| Problem | Fix |
|---|---|
| App shows "offline" | Emulator reaches the host at `10.0.2.2`; a physical device needs `-PapiBaseUrl=<lan-ip>:8081` |
| Admin console blank | Check `docker compose logs admin-web`; confirm the API base URL build arg |
| Login rejected | The bootstrap admin is created only if the env vars were set at **first** start. If they were added later, drop the volume and restart |
| Port already in use | Override `DB_HOST_PORT`, `BACKEND_HOST_PORT` or `ADMIN_HOST_PORT` in `.env` |
| E2E suite fails on re-run | It is idempotent; if a failure occurs, read `tests/results/e2e-<timestamp>.csv` for the assertion |
| Android build fails on toolchain | Set `JAVA_HOME` to the extracted JDK 25; see `docs/REPRODUCIBILITY.md` |

## Timing

Full run is roughly 15–20 minutes: infrastructure 1 min, Android 5 min, AI
2 min, admin console 3 min, evidence 5 min.