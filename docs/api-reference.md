# API reference

Base URL in development: `http://localhost:8081`

Authentication is a bearer token. Obtain one from `POST /api/auth/login` or
`POST /api/auth/register` and send it on every subsequent request:

```
Authorization: Bearer <accessToken>
```

Routes marked **admin** require `ROLE_ADMIN`. A non-admin caller receives 403
before the controller runs.

## Error envelope

Every failure uses one shape. See [`architecture.md`](architecture.md#error-envelope).

```json
{
  "timestamp": "2026-09-30T16:39:59.734292Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "path": "/api/auth/register",
  "violations": [{ "field": "password", "message": "Password must contain an uppercase letter" }]
}
```

## Pagination

List endpoints accept `page` (0-based) and `size`, and return the envelope
documented in [`architecture.md`](architecture.md#pagination-envelope).

---

## Authentication

| Method | Path | Notes |
|---|---|---|
| POST | `/api/auth/register` | 201 with token. 409 `EMAIL_ALREADY_EXISTS` / `USERNAME_ALREADY_EXISTS`, 400 `VALIDATION_ERROR` |
| POST | `/api/auth/login` | 200 with token. 401 `INVALID_CREDENTIALS`, 403 `ACCOUNT_LOCKED` / `ACCOUNT_DEACTIVATED` |

Login accepts either identifier:

```json
{ "identifier": "admin@finai.local", "password": "Admin#12345" }
```

Password policy: 8–72 characters with at least one lowercase letter, one
uppercase letter and one digit.

An unknown account and a wrong password both return `INVALID_CREDENTIALS`, and
the service still performs a hash comparison for a missing account so the two are
not distinguishable by response time.

### GET /api/auth/me

Returns the currently authenticated account. Added after the final audit found it
implemented but undocumented (discrepancy D-04).

| | |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/auth/me` |
| **Authentication** | **Required** — bearer token |
| **Request** | None. No body, no query parameters |
| **Authorisation** | Any authenticated account. Any `USER` or `ADMIN` role |
| **Implementation** | `AuthController.me()` — resolves the caller via `CurrentUserService`, so the identity comes from the token, never from a request parameter |

**200 response** — captured from the running system:

```json
{
  "id": 1,
  "email": "admin@finai.local",
  "username": "admin",
  "fullName": "System Administrator",
  "role": "ADMIN",
  "status": "ACTIVE",
  "lastLoginAt": "2026-10-01T08:16:50.210297Z",
  "createdAt": "2026-09-30T16:36:36.326399Z"
}
```

**Error cases**

| Status | Code | Cause |
|---|---|---|
| 401 | — | No token, malformed token, forged token, or expired token |
| 401 | — | Account is locked or deactivated — status is re-checked on every request, so a pre-lock token stops working immediately |
| 403 | `ADMIN_DENIED` | Not reachable for this endpoint; no role is required |

**Ownership and security behaviour**

- The account returned is always the token holder's own. There is no `id`
  parameter, so this endpoint cannot be used to read another user's account.
- `passwordHash` is **never** serialised. The `UserSummary` type exposes only
  the fields above.
- Per Jackson `default-property-inclusion: non_null`, a null field such as
  `fullName` or `lastLoginAt` is **omitted** rather than sent as `null`. Clients
  must type these as optional.
- Login on success updates `lastLoginAt`, so the value reflects the most recent
  successful authentication.

**Relation to `GET /api/users/me`**

Both return the caller's own account and are equivalent in scope. The difference
is intent: `/api/auth/me` answers "who am I in this session" and is the natural
call for restoring client state after a page reload, while `/api/users/me`
belongs to the profile feature and is paired with `PATCH /api/users/me` and
`POST /api/users/me/password`. Both remain supported; the Android client uses
`/api/users/me`.

**Coverage note:** this endpoint has no dedicated assertion in the E2E or
contract suites. It is traced as `PARTIAL` in
[`project-management/REQUIREMENT_TRACEABILITY.md`](project-management/REQUIREMENT_TRACEABILITY.md).

## Profile

| Method | Path | Notes |
|---|---|---|
| GET | `/api/users/me` | Current account |
| PATCH | `/api/users/me` | Update full name and phone |
| POST | `/api/users/me/password` | 200 on success. Requires the current password |

## Categories

| Method | Path | Notes |
|---|---|---|
| GET | `/api/categories?type=` | System catalogue + own personal categories. Optional `type` filter |
| POST | `/api/categories` | Create a personal category |
| PUT | `/api/categories/{id}` | Update a personal category |
| DELETE | `/api/categories/{id}` | **Deactivates**, never hard-deletes |

Another user's personal category returns 403 without disclosing its name. An
inactive category returns `CATEGORY_INACTIVE` when used, while existing
transactions referencing it stay readable.

## Transactions

| Method | Path | Notes |
|---|---|---|
| GET | `/api/transactions` | Filters: `type`, `categoryId`, `from`, `to`, `page`, `size` |
| GET | `/api/transactions/recent` | Top 5 by date |
| GET | `/api/transactions/{id}` | 404 if not yours |
| POST | `/api/transactions` | 201 |
| PUT | `/api/transactions/{id}` | 404 if not yours |
| DELETE | `/api/transactions/{id}` | 204 |

```json
{
  "categoryId": 1,
  "type": "EXPENSE",
  "amount": 2400000.00,
  "note": "Groceries",
  "transactionDate": "2026-09-15"
}
```

Validation: amount required, greater than zero, at most `9999999999.99`, at most
2 decimal places (rejected rather than silently rounded); date required, not in
the future, not more than 10 years old; note at most 500 characters. The category
type must match the transaction type (`CATEGORY_TYPE_MISMATCH`).

## Budgets

| Method | Path | Notes |
|---|---|---|
| GET | `/api/budgets?includeInactive=` | Own budgets with live usage |
| GET | `/api/budgets/current` | Budgets covering today |
| GET | `/api/budgets/{id}` | 404 if not yours |
| POST | `/api/budgets` | 201. 409 `BUDGET_ALREADY_EXISTS` |
| PUT | `/api/budgets/{id}` | Update |
| DELETE | `/api/budgets/{id}` | 204 |

```json
{ "categoryId": 1, "amount": 3000000.00, "periodType": "MONTHLY", "periodStart": "2026-09-01" }
```

`periodType` is `WEEKLY`, `MONTHLY` or `YEARLY`. Each view reports `usedAmount`,
`remainingAmount` (negative when overspent), `usagePercentage` and `status`.

| Status | Condition |
|---|---|
| `SAFE` | below the warning threshold |
| `WARNING` | at or above `BUDGET_WARNING_THRESHOLD` (default 80) |
| `EXCEEDED` | at or above `BUDGET_EXCEEDED_THRESHOLD` (default 100) |

Thresholds live in configuration, not in code. Crossing one raises a notification
exactly once — a repeat transaction does not duplicate the alert.

## Statistics

| Method | Path | Notes |
|---|---|---|
| GET | `/api/dashboard` | Composed home payload |
| GET | `/api/statistics/overview?from=&to=` | Totals for a range |
| GET | `/api/statistics/by-category?type=&from=&to=` | Grouped totals with percentage |
| GET | `/api/statistics/monthly?year=` | 12 points |
| GET | `/api/statistics/daily?from=&to=` | Daily series |

## Notifications

| Method | Path | Notes |
|---|---|---|
| GET | `/api/notifications?page=&size=` | Own notifications |
| GET | `/api/notifications/unread` | Unread only |
| GET | `/api/notifications/unread-count` | `{ "count": 2 }` |
| PATCH | `/api/notifications/{id}/read` | Mark one read |
| PATCH | `/api/notifications/read-all` | Mark all read |

## AI assistant

| Method | Path | Notes |
|---|---|---|
| POST | `/api/ai/chat` | Ask a question |
| GET | `/api/ai/conversations?page=&size=` | Own conversations |
| GET | `/api/ai/conversations/{id}/messages` | Transcript. 404 if not yours |

```json
{ "conversationId": null, "message": "This month, where did I spend the most?" }
```

The response carries the answer split into `FACT` and `SUGGESTION`, plus the
machine-readable evidence:

```json
{
  "conversationId": 12,
  "answer": "FACT - You spend the most on \"Food & Drink\" in 2026-09. ...",
  "facts": ["period=2026-09", "topCategory=Food & Drink", "totalExpense=3600000.00",
            "averageDailyExpense=120000.00"],
  "grounded": true,
  "engine": "LOCAL",
  "providerAvailable": false,
  "intent": "SPENDING_ANALYSIS",
  "latencyMs": 12
}
```

`grounded: false` means the assistant declined to produce a figure — no
transactions, an empty comparison baseline, or an off-topic question. It states
the reason rather than estimating. `intent` is one of `OVERVIEW`,
`SPENDING_ANALYSIS`, `BUDGET_STATUS`, `PERIOD_COMPARISON`, `TREND`,
`INCOME_ANALYSIS`, `UNRECOGNISED`.

The `facts` array is persisted alongside the message, which is what makes an
answer checkable after the fact.

## Feedback

| Method | Path | Notes |
|---|---|---|
| POST | `/api/feedback` | Submit a ticket |
| GET | `/api/feedback?page=&size=` | Own tickets |
| GET | `/api/feedback/{id}` | Own ticket. 404 if not yours |

`category` is `BUG`, `FEATURE`, `UI`, `PERFORMANCE` or `OTHER`. Content must be
10–2000 characters. New tickets start `OPEN`.

## Administration — admin only

| Method | Path | Notes |
|---|---|---|
| GET | `/api/admin/dashboard` | System metrics |
| GET | `/api/admin/system` | Process runtime metrics |
| GET | `/api/admin/users?search=&status=&page=&size=` | Search and filter |
| GET | `/api/admin/users/{id}` | One account with its transaction count |
| PATCH | `/api/admin/users/{id}/status` | Lock, unlock or deactivate |
| GET | `/api/admin/categories` | System catalogue with usage counts |
| POST | `/api/admin/categories` | Add a category |
| PATCH | `/api/admin/categories/{id}` | Rename, re-type or deactivate |
| GET | `/api/admin/feedback?status=&page=&size=` | All tickets |
| PATCH | `/api/admin/feedback/{id}` | Triage and reply |
| GET | `/api/admin/audit-logs?page=&size=` | Audit trail, newest first |

```json
{ "status": "LOCKED", "reason": "Repeated failed logins" }
```

Changing a status records `ADMIN_LOCK_USER`, `ADMIN_UNLOCK_USER` or
`ADMIN_DEACTIVATE_USER`. Self-modification is refused with `CANNOT_MODIFY_SELF`,
and the refusal itself is audited as `ADMIN_DENIED`.

Deactivating a system category sets `active: false`. It disappears from the user
pickers while every existing transaction referencing it stays readable — this is
deliberate, and is covered by an end-to-end assertion.

## Operations

| Method | Path | Notes |
|---|---|---|
| GET | `/actuator/health` | `{ "status": "UP", ... }` |
| GET | `/v3/api-docs` | OpenAPI document |
| GET | `/swagger-ui.html` | Interactive docs |

Actuator exposes `health`, `info` and `metrics`, but only `health` and `info` are
public. `GET /actuator/metrics` returns 401 without a token.