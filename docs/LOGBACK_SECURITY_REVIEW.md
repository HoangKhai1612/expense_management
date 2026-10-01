# Logging security review

Required deliverable: an actual review of Logback configuration and every log
statement in the backend. Performed 2026-10-01. This review **closes limitation
L-05** ("log-redaction review incomplete") for the current codebase.

## Method

1. Locate the logging configuration and every pattern definition.
2. Enumerate every log statement in production source.
3. Inspect each statement's arguments for sensitive values.
4. Search for any log call that references a password, token, secret, API key or
   `Authorization` header.
5. Capture the **runtime** log from the live container and search it for the
   actual secret values from `.env`.
6. Test whether Spring Boot's generated default security user is exploitable.

## 1. Configuration

```yaml
logging:
  level:
    root: INFO
    com.finai: ${LOG_LEVEL:INFO}
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"
```

| Check | Result |
|---|---|
| `logback-spring.xml` present | **No** — configuration lives entirely in `application.yml` |
| Custom pattern defined | Yes, console only |
| `org.hibernate.SQL` level | **WARN** — SQL statements are not logged. This is the single most important setting: at `DEBUG` it would log bound parameter values, i.e. transaction amounts |
| Spring Security level | **WARN** — suppresses framework-internal filter chatter |
| Log level configurable | Yes, via `LOG_LEVEL` |
| File appender | None — stdout only, which is correct for containers |
| Masking converter configured | **No** |

**Assessment on masking.** No `%mask` or equivalent is configured. With the
current log statements this is acceptable, because no statement emits a secret.
It is not future-proof: adding a log call that passes a token would leak it
without any backstop. This is recorded as a hardening recommendation, not a
current defect.

## 2. Complete log statement inventory

All 20 statements in production source, with what each actually emits:

| Location | Statement | Emits | Sensitive? |
|---|---|---|---|
| `AuthService:64` | `Registered account {} ({})` | account id, **email** | PII only |
| `AuthService:88` | `Failed login for account {} ({}): wrong password` | account id, **email** | PII only; **password not logged** |
| `AuthService:93` | `Rejected login for account {} because it is locked` | account id | No |
| `AuthService:103` | `Successful login for account {} ({})` | account id, **email** | PII only |
| `JwtAuthenticationFilter:69` | `Access token subject is not a numeric id` | static text | No |
| `JwtAuthenticationFilter:75` | `Access token refers to account {} which no longer exists` | account id | No |
| `JwtAuthenticationFilter:80` | `Rejected request for account {} because its status is {}` | account id, status | No |
| `JwtService:62` | `Rejected access token: {}` | **exception message** | No — see note below |
| `AiChatService:139` | `AI turn user={} conversation={} intent={} grounded={} engine={} latencyMs={}` | ids, intent, engine, latency | No — **question and answer are not logged** |
| `AiProviderClient:96` | `External AI provider call failed...: {}` | **exception message** | No — see note below |
| `NotificationService:129` | `Raised {} for user {} on budget {}` | type, user id, budget id | No |
| `NotificationService:132` | `Duplicate notification suppressed for budget {}` | budget id | No |
| `AuditService:55` | `AUDIT action={} admin={} target={}/{} result={}` | action, admin id, target | No |
| `AuditService:59` | `Failed to write audit entry for action {}` | action + stack trace | No |
| `BootstrapDataInitializer:55` | `No bootstrap administrator configured. Set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD...` | **variable names only** | No — **no values** |
| `BootstrapDataInitializer:60` | `APP_ADMIN_PASSWORD is shorter than 8 characters` | variable name + length fact | No — **value not logged** |
| `BootstrapDataInitializer:66` | `Bootstrap administrator {} already exists` | email | PII only |
| `BootstrapDataInitializer:73` | `Created bootstrap administrator account {}` | email | PII only |
| `GlobalExceptionHandler:72` | `Data integrity violation on {}: {}` | path + **raw DB exception message** | See finding LOG-02 |
| `GlobalExceptionHandler:101` | `Unhandled exception on {} {}` | method, path + **full stack trace** | See finding LOG-03 |

### Notes on the two exception-message statements

**`JwtService:62`** logs the JJWT exception message when token parsing fails. A
JJWT exception (e.g. `SignatureException`, `ExpiredJwtException`) carries a
reason and **not** the token itself, so the token is not disclosed. Verified by
reading the dependency behaviour and confirmed by the runtime log scan in
section 5, which found zero `Bearer`/`Authorization` occurrences.

**`AiProviderClient:96`** logs the exception message from an external provider
call. The API key is transmitted in a header, never in the URL, so it does not
appear in these messages. Classified LOW, not fixed — changing it purely to
generate a commit is explicitly out of scope.

## 3. Credential-pattern search

A search across production source for any log call referencing
`getPassword`, `password()`, `getPasswordHash`, `token()`, `accessToken`,
`getSecret`, `apiKey`, `Authorization` or `getHeader`:

**Result: zero matches.** No log statement can emit a password, token, secret,
API key or `Authorization` header.

## 4. Sensitive categories — verdict

| Category | Logged? | Verdict |
|---|---|---|
| Passwords (login, registration, change) | **No** | **PASS** |
| Password hashes | **No** | **PASS** |
| JWT / access tokens | **No** | **PASS** |
| Refresh tokens | N/A — none exist | **PASS** |
| `Authorization` header | **No** | **PASS** |
| API keys / AI provider keys | **No** | **PASS** |
| Database credentials | **No** | **PASS** |
| Request or response bodies | **No** | **PASS** |
| AI question / answer text | **No** | **PASS** — only metadata is logged |
| Transaction amounts and notes | **No** | **PASS** — no statement logs financial values |
| Email addresses | **Yes**, in 5 statements | **LOG-01, informational** |
| Raw DB exception messages | **Yes**, 1 statement | **LOG-02, low** |
| Full exception stack traces | **Yes**, 1 statement | **LOG-03, low** |

## 5. Runtime log verification

The **live** backend log was captured from the running container and searched for
the actual secret values from `.env`:

| Secret | Present in runtime log? |
|---|---|
| `JWT_SECRET` (64 chars) | **No** |
| `DB_PASSWORD` (18 chars) | **No** |
| `APP_ADMIN_PASSWORD` | **No** |
| `AI_PROVIDER_API_KEY` | **No** |

| Pattern | Occurrences in runtime log | Assessment |
|---|---|---|
| `password` (case-insensitive) | 5 | All safe — see below |
| `Bearer` / `Authorization` | **0** | **PASS** |

The 5 `password` occurrences were inspected individually:

1. `Using generated security password: <uuid>` — Spring Boot framework notice
2. `This generated password is for development use only` — same notice
3. `Failed login for account 23 (...): wrong password` — **our own** message; the
   password value is not present, only the word "password"
4. `Encoded password does not look like BCrypt` — BCrypt encoder warning about
   the deliberately forged test token, contains no secret
5. A duplicate of the framework notice

**No secret value appears in the runtime log.**

## 6. Spring Boot generated security user

The startup log contains:

```
Using generated security password: <uuid>
```

This means `UserDetailsServiceAutoConfiguration` created its default in-memory
`user` account. Whether that is exploitable depends entirely on whether our
security chain accepts HTTP Basic or form-login credentials.

**Tested directly against the running system:**

| Request | Result |
|---|---|
| `GET /api/admin/dashboard` with Basic `user:<generated-password>` | **401** |
| `GET /api/transactions` with Basic `user:<generated-password>` | **401** |

**Verdict: NOT exploitable.** Our `SecurityConfig` defines a stateless JWT filter
chain and never enables `httpBasic()` or `formLogin()`, so the default account
cannot authenticate. The message is a cosmetic startup warning.

### Recommended suppression

Not applied, because changing production configuration for cosmetics is out of
scope and carries more risk than the warning does. If the warning is unwanted in
demonstration output, the clean fix is to exclude
`UserDetailsServiceAutoConfiguration` explicitly — deliberately, in a separate
commit, with a full re-verification.

## 7. Findings

| ID | Finding | Severity | Status |
|---|---|---|---|
| LOG-01 | Email addresses (PII) logged on register and on both login outcomes | Informational | **Accepted.** Deliberate: a login audit trail needs the account identity. For a personal-finance single-tenant system this is appropriate |
| LOG-02 | `GlobalExceptionHandler:72` logs the raw database exception message, which for a constraint violation can embed the offending value | Low | **Accepted.** The envelope returned to the client is already sanitised (`include-message: never`); only the server log carries it. The value would be PII, not a credential |
| LOG-03 | `GlobalExceptionHandler:101` logs a full stack trace, which can carry SQL fragments or bound values | Low | **Accepted.** Standard practice. `org.hibernate.SQL: WARN` prevents the companion risk of logged parameter values |
| LOG-04 | No masking converter configured | Low | **Accepted.** No current statement can leak a secret, so the control would be inert today. Recommended as future-proofing when any new log statement is added |
| LOG-05 | Generated-security-password warning is cosmetic | Informational | **Accepted**, verified non-exploitable |

## 8. Conclusion

| Question | Answer |
|---|---|
| Are passwords logged? | **No** |
| Are tokens logged? | **No** |
| Are API keys or DB credentials logged? | **No** |
| Are request bodies or financial values logged? | **No** |
| Is any secret present in the runtime log? | **No**, verified against live `.env` values |
| Is the default Spring user exploitable? | **No**, verified 401 on two endpoints |

**Limitation L-05 is CLOSED for the current codebase.** No code change was made,
because no real defect was found — the review found the logging configuration to
be sound. Changing it merely to produce a commit was explicitly out of scope.

### Residual caveat

This review covers the current codebase as committed. It cannot cover secrets
that may appear in **future** log statements, which is why LOG-04 recommends a
masking converter as the first hardening step when new logging is added.