# Architecture

## Backend module layout

The backend is a single Spring Boot application organised by business capability
rather than by layer. Each package owns its entity, repository, service and
controller, so a change to a rule stays inside one package.

```
com.finai
├── auth          registration, login, token issuance
├── security      JWT issue/verify, current-user resolution
├── user          account entity, roles, profile
├── category      system catalogue + personal categories
├── transaction   income/expense entries
├── budget        limits, usage, threshold alerts
├── statistics    overview, by-category, monthly/daily series
├── dashboard     the composed home screen payload
├── notification  in-app alerts
├── feedback      user tickets and admin triage
├── ai            intent detection, analyst, snapshot, provider client
├── admin         privileged operations and metrics
├── audit         administrative audit trail
├── bootstrap     first-run seeding
├── config        @ConfigurationProperties and security wiring
└── common        error envelope, pagination envelope
```

### Layering rules

- Controllers do no business logic and never build an error response directly.
- Services throw `ApiException` with a machine code; `GlobalExceptionHandler`
  turns that into the single error envelope.
- Repositories are Spring Data interfaces; aggregate queries that cannot be
  derived are declared as `@Query` and are always parameterised.

## Error envelope

Every failure uses one shape, so clients branch on a code rather than parsing
prose:

```json
{
  "timestamp": "2026-09-30T16:39:59.734292Z",
  "status": 400,
  "code": "CATEGORY_TYPE_MISMATCH",
  "message": "Category 1 is an EXPENSE category and cannot be used for an INCOME entry.",
  "path": "/api/transactions",
  "violations": [{ "field": "amount", "message": "Amount is required." }]
}
```

Codes in use include `EMAIL_ALREADY_EXISTS`, `USERNAME_ALREADY_EXISTS`,
`INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `CANNOT_MODIFY_SELF`,
`AMOUNT_MUST_BE_POSITIVE`, `AMOUNT_TOO_PRECISE`, `DATE_IN_FUTURE`,
`CATEGORY_TYPE_MISMATCH`, `BUDGET_ALREADY_EXISTS`, `TRANSACTION_NOT_FOUND`.

## Pagination envelope

Shared by the Android client and Admin Web so neither has to special-case
Spring's `Page`:

```json
{
  "content": [],
  "page": 0, "size": 20,
  "totalElements": 0, "totalPages": 0,
  "first": true, "last": true, "empty": true
}
```

## Data model

Eleven tables, created by nine Flyway migrations. Flyway owns the schema;
Hibernate runs with `ddl-auto: validate`, so a drifting migration fails at
start-up rather than silently at runtime.

| Table | Purpose |
|---|---|
| `users` | accounts, BCrypt password hash, role FK, status |
| `roles` | `USER` / `ADMIN` as data, not an enum |
| `categories` | system catalogue plus per-user personal categories |
| `transactions` | income/expense entries, always scoped by `user_id` |
| `budgets` | per-category limits for a date range |
| `notifications` | budget alerts and other in-app messages |
| `feedback` | user tickets with admin status and reply |
| `ai_conversations` | one conversation per chat thread |
| `ai_messages` | question, answer, and the verified facts behind it |
| `audit_logs` | every privileged action, including refusals |
| `flyway_schema_history` | migration bookkeeping |

`row_version` on `users` gives optimistic locking, so two concurrent admin edits
cannot silently overwrite each other.

## Authorisation model

Three rules carry most of the security weight:

**Ownership is enforced in the query, not after the fetch.** Every user-scoped
read is `findByIdAndUserId(id, userId)`. A transaction belonging to someone else
returns 404, not 403: answering 403 would confirm that the id exists.

**System categories are reserved for admins.** A normal user sees the catalogue
and their own categories, and can only mutate the latter. This is what stops an
administrative edit from silently rewriting every user's history.

**The admin check happens in the filter chain.** `SecurityConfig` requires
`ROLE_ADMIN` for `/api/admin/**` before the controller is reached, and
`GlobalExceptionHandler` records the refusal in the audit trail.

### Locking

`ADMIN_LOCK_USER` changes the account status. `JwtAuthenticationFilter` re-reads
the account on every request, so a token issued before the lock stops working
immediately rather than at expiry. Locking records the reason in the audit trail
and refuses self-lock with `CANNOT_MODIFY_SELF`.

## The AI assistant

Three cooperating pieces:

**`AiText`** classifies a question into one of seven intents by deterministic
keyword matching, with or without Vietnamese diacritics. There is no learned
classifier on purpose: the mapping is part of the product contract, it has to be
testable, and an unrecognised question must degrade to a refusal rather than to a
wrong number. Precedence is ordered so a specific shape wins over the general
one — "this month, where did I spend the most?" is a spending question, not an
overview.

**`AiDataSnapshotService`** builds the verified fact base. The user id is a
required argument and never parsed from the question. Every value is a
parameterised aggregate over that user's rows.

**`AiAnalyst`** assembles the answer from the snapshot and emits each numeric
claim into a `facts` list that is persisted with the message.

Answers are always two sections:

```
FACT - verified statements, each traceable to a stored figure
SUGGESTION - judgement, explicitly labelled as not a verified fact
```

`grounded` is false whenever the assistant declines to produce a figure — no
data, an empty comparison baseline, or an off-topic question. In that case it says
so rather than estimating.

`AI_PROVIDER=LOCAL` (the default) uses this deterministic analyst and contacts no
external service. Setting a different provider enables `AiProviderClient`, which
calls the configured endpoint but still receives the computed snapshot as its
context.

## Clients

**Admin Web** is a React single-page app. The API base URL is baked in at build
time because a static bundle cannot resolve it at runtime; in development Vite
proxies `/api` to the backend so the browser sees one origin and there are no CORS
preflights. Nginx serves the bundle with SPA fallback and `no-store` on
`index.html`, so a deploy cannot leave a client on a stale bundle.

**Android** is a Jetpack Compose app using Retrofit and OkHttp. The token lives in
DataStore, read by an interceptor on the network thread. `Result` distinguishes
`Loaded`, `Failed` (with the backend's machine code) and `Offline`, so the UI can
say "you appear to be offline" rather than showing a generic failure. The base URL
defaults to `10.0.2.2` (the emulator's view of the host) and is overridable with
`-PapiBaseUrl=`.

## Notable error-envelope detail

Jackson runs with `default-property-inclusion: non_null`, so a null field is
**omitted** from the payload rather than serialised as `null`. Both clients were
written against that reality: nullable fields are typed optional, and the
contract test asserts a field is never an explicit JSON `null`. This surfaced as a
real contract bug while building Admin Web, where nullable fields had initially
been typed as required-but-nullable.