# Security audit

Scope: source repository, configuration, Docker setup, both clients, and the
running deployment. Findings are classified **PASS**, **WARNING** or
**NOT IMPLEMENTED**. No secret value is reproduced in this document.

Audit date: 2026-10-01.

---

## 1. Credential storage — PASS

| Check | Result | Evidence |
|---|---|---|
| No password hash in any migration | **PASS** | Grep across V1–V9 found none |
| Passwords stored as BCrypt hashes | **PASS** | `SecurityConfig` uses `BCryptPasswordEncoder(12)`; `MigrationsAndBootstrapIT` asserts hashes |
| Bootstrap admin password from environment | **PASS** | `BootstrapDataInitializer` reads `APP_ADMIN_PASSWORD` |
| No default admin password | **PASS** | Application starts with no admin when unset, and logs it rather than inventing a default |
| Test credentials isolated | **PASS** | `application-test.yml` uses a fixed throwaway JWT secret, commented as test-only |
| `.env` excluded from source control | **PASS** | `.gitignore` lists `.env` in the secrets block, first entry |
| `.env.local`, `*.pem`, `*.key`, `secrets/` ignored | **PASS** | All present in `.gitignore` |
| Android local config ignored | **PASS** | `android/local.properties` ignored |
| APK and keystore ignored | **PASS** | `*.apk`, `*.aab`, `*.keystore` ignored |

## 2. Secret leakage sweep — PASS

The live `.env` secret values were extracted and searched for, by value, across
every tracked source file. **No live secret appears anywhere outside `.env`.**

| Secret | Length | Leaked into source? |
|---|---|---|
| `DB_PASSWORD` | 18 | **No** |
| `JWT_SECRET` | 64 | **No** |
| `APP_ADMIN_PASSWORD` | 11 | **No** |
| `AI_PROVIDER_API_KEY` | empty | n/a |

Additional sweep across all tracked files for credential patterns:

| Pattern | Result |
|---|---|
| Private key headers (`BEGIN RSA/PRIVATE KEY`) | None found |
| API key shapes (`sk-...`) | None found |
| Database URIs with embedded credentials | None found |
| Hardcoded bearer tokens | None found |
| Secrets in `docker-compose.yml` | None — all values use `${VAR}` indirection |
| Secrets in frontend source | None — no `VITE_*` secret, only a public API base URL |
| Secrets in Android source | None — no key material |

## 3. Authentication — PASS

| Control | Result | Evidence |
|---|---|---|
| Password hashing algorithm | BCrypt, **cost factor 12** | `SecurityConfig.passwordEncoder()` |
| Timing-attack resistance | **PASS** — a hash comparison runs even for a nonexistent account | `AuthService` comment and code; `AuthServiceTest` |
| Generic failure message | **PASS** — unknown account and wrong password both return `INVALID_CREDENTIALS` | E2E 2 assertions |
| Password policy | 8–72 chars, upper + lower + digit | E2E "Weak password is rejected" |
| Password change requires current password | **PASS** | E2E "Changing password with a wrong current one is rejected" |
| Token signing | HS256 HMAC via a ≥32-byte secret from the environment | `JwtService`, `JwtServiceTest` (7 tests) |
| Token required for protected routes | **PASS** | E2E: absent, malformed and forged tokens all → 401 |
| Tamper detection | **PASS** | `JwtServiceTest` |
| Expiry enforced | **PASS** | `JWT_EXPIRATION`, default 12h |
| JWT secret has no default | **PASS** | Startup fails without it; Compose enforces `${JWT_SECRET:?...}` |

## 4. Authorisation — PASS

| Control | Result | Evidence |
|---|---|---|
| Stateless sessions | **PASS** — `SessionCreationPolicy.STATELESS` | `SecurityConfig` |
| CSRF disabled | **PASS and appropriate** — bearer tokens are not sent ambiently, so there is no CSRF surface | `SecurityConfig` |
| Admin routes role-protected | **PASS** — `/api/admin/**` requires `ROLE_ADMIN` | E2E + contract (3 assertions) |
| Public endpoint allow-list | **PASS** — 8 explicit paths, default deny | `PUBLIC_ENDPOINTS` |
| Anonymous rejected | **PASS** — 401 | E2E, contract suite |
| Non-admin rejected | **PASS** — 403 | E2E, contract suite |
| Privilege self-restriction | **PASS** — admin cannot lock own account | E2E, `CANNOT_MODIFY_SELF` |
| Refusal is audited | **PASS** — `ADMIN_DENIED` written | E2E |

**Note on the public allow-list.** `/swagger-ui`, `/swagger-ui/**`,
`/swagger-ui.html`, `/v3/api-docs` and `/v3/api-docs/**` are public. That is
deliberate for a demonstrable API, but it exposes the full endpoint catalogue in
production. Classified as a **WARNING** below.

## 5. Data isolation — PASS

| Control | Result | Evidence |
|---|---|---|
| Owner-scoped reads | **PASS** — `findByIdAndUserId`, not fetch-then-check | 7 E2E assertions |
| Owner-scoped writes | **PASS** | E2E update and delete → 404 |
| Owner-scoped deletes | **PASS** | E2E → 404 |
| Non-disclosure on cross-user access | **PASS** — 404 rather than 403, so existence is not confirmed | E2E |
| Another user's category not described | **PASS** — 403 without naming it | `CategoryServiceTest` |
| AI snapshot scoped to caller | **PASS** — user id is a service argument, never parsed from the question | `AiGroundingIT` |
| System catalogue mutations admin-only | **PASS** — 403 for normal users | E2E |
| Statistics scoped | **PASS** — zero totals for a user with no data | E2E |

This is the strongest security area in the system. Ownership is enforced in the
query rather than after the fetch, which is the correct pattern.

## 6. Error handling — PASS

| Control | Result | Evidence |
|---|---|---|
| Stack traces hidden | **PASS** — `include-stacktrace: never` | `application.yml` |
| Internal messages hidden | **PASS** — `include-message: never` | `application.yml` |
| Binding errors hidden | **PASS** — `include-binding-errors: never` | `application.yml` |
| Single error envelope | **PASS** — `GlobalExceptionHandler` | E2E asserts machine codes |
| Validation detail returned | **PASS** — field-level violations | `ApiError.violations` |
| No stack trace in startup log | **PASS** — 0 ERROR lines | `docker compose logs backend` |

## 7. Injection — PASS

| Control | Result | Evidence |
|---|---|---|
| SQL injection | **PASS** — Spring Data and named/parameterised `@Query` throughout; no string-concatenated SQL | Source inspection |
| AI prompt injection into data access | **PASS by construction** — no text-to-SQL exists; the analyst reasons over a typed snapshot, and the user id cannot be influenced by the question | Architecture; `AiGroundingIT` |
| Cross-tenant AI access | **PASS** — snapshot is built for the authenticated user only | E2E transcript isolation |
| Frontend XSS | **PASS** — React escapes by default; no `dangerouslySetInnerHTML` | Source inspection |
| No reflection of raw user input into HTML | **PASS** | Source inspection |

The absence of a text-to-SQL path is the strongest single security property in
the AI design: an attacker cannot talk the assistant into querying another
account, because the assistant cannot express a query at all.

## 8. Transport — NOT IMPLEMENTED

| Control | Status | Note |
|---|---|---|
| TLS | **NOT IMPLEMENTED** | The backend serves plain HTTP. Must be terminated by a reverse proxy or load balancer in front |
| HSTS | **NOT IMPLEMENTED** | Follows from TLS not being terminated here |
| Secure cookie flags | Not applicable | Stateless bearer tokens, no cookies |

The client stores the Android token outside `EncryptedSharedPreferences` (see
section 11). The web console keeps the token in browser storage, which is
exposed to XSS — acceptable given React's escaping and the absence of a
user-generated-HTML surface, but it is not equivalent to httpOnly cookies.

## 9. Deployment — WARNING

| Finding | Severity | Detail |
|---|---|---|
| **W-01** Swagger UI and OpenAPI publicly reachable | WARNING | `/swagger-ui.html` returns 200 unauthenticated (verified). Exposes the full endpoint catalogue. Acceptable for demonstration; restrict in production |
| **W-02** Actuator exposure | PASS | `/actuator/metrics` returns **401** unauthenticated — verified, not public. Only `health` and `info` are in the allow-list |
| **W-03** Development credentials documented | WARNING | `admin@finai.local` / `Admin#12345` appear in `.env.example`, `application-test.yml` and this project. Correct for a demo; must be changed per environment |
| **W-04** Containers run as non-root | PASS | Backend image creates a `finance` user; verified in the Dockerfile |
| **W-05** PostgreSQL exposed on a host port | WARNING | Bound to `0.0.0.0:5433`. On a shared host the database is reachable from the network. Bind to `127.0.0.1` for local-only use |
| **W-06** No rate limiting | NOT IMPLEMENTED | Login and AI chat are unlimited. Blocks public deployment |
| **W-07** No image scanning | NOT IMPLEMENTED | No Trivy/Grype pass. `npm audit` covers frontend dependencies only |

## 10. Log hygiene — NOT IMPLEMENTED

**This is the one explicitly named production gap, and it remains open.**

| Check | Status |
|---|---|
| Logback pattern reviewed for token/password leakage | **NOT PERFORMED** |
| Request/response bodies logged | Not observed at INFO |
| JWT tokens in log output | Not observed, but no systematic check was run |
| Passwords in log output | **PASS** — `AuthService` logs outcome and account, never the password |

The backend log was scanned during the audit and contained 0 ERROR lines. That
is **not** a redaction review: it says nothing about what INFO-level output would
contain under a different code path, such as an exception with a request
attached.

## 11. Client-side security — PARTIAL

| Area | Backend | Android |
|---|---|---|
| Token storage | Browser storage (localStorage) | **Plain storage, not `EncryptedSharedPreferences` or Keystore** |
| TLS | Not implemented | Not applicable locally |
| Certificate pinning | No | No |
| Local data at rest | n/a | **Not encrypted** |
| Obfuscation | n/a | `isMinifyEnabled = false`, no ProGuard rules |

The Android token storage is a **WARNING** for a debug build and would be a
defect in a release build. It is recorded as accepted-for-scope in
[`known-limitations.md`](known-limitations.md).

## 12. Dependencies

| Control | Result | Evidence |
|---|---|---|
| Frontend vulnerability scan | **PASS** — 0 vulnerabilities | `npm audit` |
| Backend dependency scan | **NOT IMPLEMENTED** | No OWASP dependency-check, Snyk or Dependabot |
| Android dependency scan | **NOT IMPLEMENTED** | No Gradle dependency verification plugin |
| Lock file committed | Yes for the console | `package-lock.json` present |
| Spring Boot parent pins managed versions | **PASS** — reduces dependency risk | `spring-boot-starter-parent` 3.5.16 |

Only the frontend dependency set is scanned. The backend and Android dependency
sets are not, and no claim is made about them.

## 13. Findings summary

| # | Finding | Class | Severity |
|---|---|---|---|
| S-01 | Project is not under version control, so secrets cannot be proven absent from history | WARNING | HIGH |
| S-02 | No `git` history means no audit trail of who changed security code | WARNING | HIGH |
| S-03 | Log redaction review not performed | NOT IMPLEMENTED | MEDIUM |
| S-04 | No rate limiting on login or AI | NOT IMPLEMENTED | MEDIUM |
| S-05 | No TLS termination | NOT IMPLEMENTED | MEDIUM |
| S-06 | Swagger UI and OpenAPI public | WARNING | MEDIUM |
| S-07 | ~~Actuator metrics public~~ — **retracted** | PASS | Verified: `/actuator/metrics` returns 401 unauthenticated |
| S-08 | PostgreSQL bound to all interfaces | WARNING | MEDIUM |
| S-09 | Development credentials documented | WARNING | MEDIUM |
| S-10 | Android token stored unencrypted | WARNING | MEDIUM |
| S-11 | No backend or Android dependency scanning | NOT IMPLEMENTED | LOW |
| S-12 | No image scanning | NOT IMPLEMENTED | LOW |
| S-13 | No SAST/DAST or penetration testing | NOT IMPLEMENTED | LOW |
| S-14 | No production backup, so a breach cannot be recovered from | NOT IMPLEMENTED | MEDIUM |

### The honest caveat about S-01

Because there is no repository, this audit can prove that no secret exists in the
**current working tree**. It **cannot** prove that no secret ever existed in a
deleted or overwritten file, because there is no history to inspect. That
limitation is inherent, not a finding that can be closed by re-running a scan.

---

## Controls that passed with no action needed

Password hashing at cost 12; timing-safe login; generic credential errors;
stateless sessions; owner-scoped queries with non-disclosure; admin route
protection; privilege self-restriction; parameterised SQL; absence of a
text-to-SQL AI path; stack-trace suppression; non-root container; no secret in
any tracked file; no secret in either client; no hash in any migration.