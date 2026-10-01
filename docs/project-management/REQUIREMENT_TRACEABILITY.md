# Requirement traceability matrix

Every row maps a requirement to the implementing code, the test that proves it,
and the evidence that test produced. A requirement is `VERIFIED` only when a
test was executed and passed during the final audit.

Legend — **VERIFIED**: executed and passed. **PARTIAL**: implemented, not fully
covered by tests. **GAP**: not implemented. **UNKNOWN**: cannot be assessed
because evidence does not exist.

---

## Authentication

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Registration | `AuthService.register`, `AuthController` | `AuthServiceTest`; E2E "Register user A" | 201 + token; 11 unit tests pass | VERIFIED |
| Duplicate email rejected | `AuthService` uniqueness check | E2E | 409 `EMAIL_ALREADY_EXISTS` | VERIFIED |
| Duplicate username rejected | `AuthService` uniqueness check | E2E | 409 `USERNAME_ALREADY_EXISTS` | VERIFIED |
| Password policy enforced | `RegisterRequest` bean validation | E2E "Weak password is rejected" | 400 `VALIDATION_ERROR` | VERIFIED |
| Malformed email rejected | `@Email` validation | E2E | 400 | VERIFIED |
| Login by email | `AuthService.login` | E2E "Login with email" | 200 + token | VERIFIED |
| Login by username | `AuthService.login` | E2E "Login with username" | 200 + token | VERIFIED |
| Invalid credentials rejected | `AuthService` | `AuthServiceTest`; E2E | 401 `INVALID_CREDENTIALS` | VERIFIED |
| Unknown account rejected identically | `AuthService` still runs `matches()` | `AuthServiceTest`; E2E "Unknown account is rejected" | 401, same code as wrong password | VERIFIED |
| Password stored hashed (BCrypt cost 12) | `SecurityConfig.passwordEncoder()` | `MigrationsAndBootstrapIT` | Hashes in DB, no plaintext | VERIFIED |
| Missing/invalid token rejected | `JwtAuthenticationFilter` | E2E | 401 for absent, malformed, forged tokens | VERIFIED |
| Account locked blocks login | `AuthService` status check | `AuthServiceTest`; E2E | 403 `ACCOUNT_LOCKED` | VERIFIED |
| Deactivated account blocked | `AuthService` status check | `AuthServiceTest` | Rejected | VERIFIED |
| Lock invalidates existing tokens | Filter re-reads account status | E2E "Pre-lock token is rejected after locking" | 401 after lock | VERIFIED |
| Session state is stateless | `SessionCreationPolicy.STATELESS` | Source inspection | — | VERIFIED |
| `GET /api/auth/me` | `AuthController.me()` | Not covered by any test | Endpoint reachable | PARTIAL |

## Authorization

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Anonymous caller rejected | `SecurityConfig` permit-list | E2E; contract suite | 401 | VERIFIED |
| Non-admin blocked from admin routes | `hasRole("ADMIN")` | E2E; contract suite | 403 | VERIFIED |
| Admin role assigned at bootstrap | `BootstrapDataInitializer` | E2E; `MigrationsAndBootstrapIT` | Role `ADMIN` | VERIFIED |
| Admin cannot modify own account | `AdminService` self-check | E2E | 400 `CANNOT_MODIFY_SELF` | VERIFIED |
| Refused self-modification is audited | `AuditService` | E2E "Refused self-lock was audited as a denial" | `ADMIN_DENIED` row | VERIFIED |

## User ownership isolation

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Cannot read another user's transaction | `findByIdAndUserId` owner filter | E2E | 404 `TRANSACTION_NOT_FOUND` | VERIFIED |
| Cannot update another user's transaction | Owner filter in service | E2E | 404 | VERIFIED |
| Cannot delete another user's transaction | Owner filter in service | E2E | 404 | VERIFIED |
| List is scoped to own data | Repository queries take `userId` | E2E | Empty list, zero dashboard totals | VERIFIED |
| Cannot read another user's AI transcript | Owner filter | E2E | 404 | VERIFIED |
| Cannot read another user's feedback | Owner filter | E2E | 404 | VERIFIED |
| Another user's personal category is not disclosed | `CategoryService` | `CategoryServiceTest` | 403 without revealing the name | VERIFIED |

## Categories

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| List system + own categories | `CategoryService` | E2E; contract suite | 14 system categories | VERIFIED |
| Create personal category | `CategoryController` | E2E | 201 | VERIFIED |
| Update personal category | `CategoryController` | E2E | Updated | VERIFIED |
| Delete deactivates rather than hard-deletes | `CategoryService` | `CategoryServiceTest`; E2E | `active=false` | VERIFIED |
| Type filter | `CategoryService` | Contract suite | Filter accepted | VERIFIED |
| System categories hidden from users | Owner-scoped query | E2E | 0 results after admin deactivation | VERIFIED |
| Category deactivation preserves history | Soft-delete design | E2E "Existing transactions survive category deactivation" | 200, data readable | VERIFIED |
| Duplicate category code rejected | `CategoryService` | E2E; contract suite | 409 | VERIFIED |

## Transactions

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Create income | `TransactionService` | E2E | 201, amount echoed | VERIFIED |
| Create expense | `TransactionService` | E2E | 201 | VERIFIED |
| Update | Owner-scoped update | E2E | Amount and note changed | VERIFIED |
| Delete | Owner-scoped delete | E2E | 204, then 404 | VERIFIED |
| Delete unknown id returns 404 | Owner filter | E2E | 404 | VERIFIED |
| Negative amount rejected | Validation | `TransactionServiceTest`; E2E | 400 `AMOUNT_MUST_BE_POSITIVE` | VERIFIED |
| Zero amount rejected | Validation | E2E | 400 | VERIFIED |
| Missing amount rejected | `@NotNull` | E2E | 400 | VERIFIED |
| Future date rejected | Validation | E2E | 400 `DATE_IN_FUTURE` | VERIFIED |
| Malformed date rejected | `@DateTimeFormat` | E2E | 400 | VERIFIED |
| Category/type mismatch rejected | `TransactionService` | E2E | 400 `CATEGORY_TYPE_MISMATCH` | VERIFIED |
| Unknown category rejected | FK + service check | E2E | 400 | VERIFIED |
| Recent transactions list | `TransactionController` | E2E via dashboard | Non-empty | VERIFIED |

## Budgets

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Create budget | `BudgetService` | `BudgetServiceTest`; E2E | 201 | VERIFIED |
| Usage computed from transactions | `BudgetService` | `BudgetServiceTest`; E2E | 80% of 3,000,000 | VERIFIED |
| Status SAFE below threshold | Threshold logic | `BudgetServiceTest`; E2E | `SAFE` | VERIFIED |
| Status WARNING at threshold | Threshold logic | E2E | `WARNING` at 80% | VERIFIED |
| Status EXCEEDED over limit | Threshold logic | E2E | `EXCEEDED` at 120% | VERIFIED |
| Remaining goes negative on overspend | Arithmetic | E2E | −600,000 | VERIFIED |
| Duplicate period rejected | `BudgetService` | E2E | 409 `BUDGET_ALREADY_EXISTS` | VERIFIED |
| Budget on income category rejected | Type check | E2E | 400 | VERIFIED |
| Thresholds configurable | `BudgetAlertProperties` | `application-test.yml` | 80/100 injected | VERIFIED |
| Warning notification raised once | `NotificationService` | E2E | Count stays 1 across recomputation | VERIFIED |
| Critical notification on breach | `NotificationService` | E2E | 1 raised at 120% | VERIFIED |
| Current budgets | `GET /budgets/current` | Source inspection | No dedicated assertion | PARTIAL |

## Dashboard and statistics

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Dashboard month income | `DashboardService` | E2E | 15,000,000 matches transactions | VERIFIED |
| Dashboard month expense | `DashboardService` | E2E | 2,400,000 | VERIFIED |
| Dashboard month balance | `DashboardService` | E2E | 12,600,000 | VERIFIED |
| Dashboard recent list | `DashboardService` | E2E | Non-empty | VERIFIED |
| Statistics overview | `StatisticsService` | E2E | Income matches transactions | VERIFIED |
| By-category breakdown | `StatisticsService` | E2E | Non-empty | VERIFIED |
| Monthly series, 12 points | `StatisticsService` | E2E | 12 points | VERIFIED |
| Daily series | `StatisticsService` | Not asserted by a suite | Endpoint present | PARTIAL |
| Zero totals for a user with no data | Owner-scoped aggregate | E2E | 0 | VERIFIED |

## Notifications

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| List own notifications | `NotificationController` | Source inspection | Endpoint present | PARTIAL |
| Unread filter | `NotificationController` | Source inspection | Endpoint present | PARTIAL |
| Unread count | `NotificationController` | Source inspection | Endpoint present | PARTIAL |
| Mark one read | `NotificationController` | Source inspection | Endpoint present | PARTIAL |
| Mark all read | `NotificationController` | Android client exercises it | Endpoint present | PARTIAL |

## AI assistant

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Intent classification | `AiText` | `AiTextTest` (24 tests) | All pass | VERIFIED |
| Intent precedence | `AiText` | `AiTextTest` | Spending beats overview | VERIFIED |
| Snapshot scoped to caller | `AiDataSnapshotService` | `AiGroundingIT` | Other user's data absent | VERIFIED |
| Grounded spending answer | `AiAnalyst.spending` | `AiGroundingIT`; E2E | FACT cites 3,600,000 | VERIFIED |
| AI total equals DB total | E2E assertion | E2E "AI expense fact equals the stored expense total" | `aiFact` = `dbTotalExpense` | VERIFIED |
| Facts persisted with message | `AiMessage.facts` | E2E "AI persisted the verified facts" | 7 facts stored | VERIFIED |
| Grounded budget answer | `AiAnalyst` | `AiGroundingIT`; E2E | 120% EXCEEDED cited | VERIFIED |
| Refuses without data | `AiAnalyst` ungrounded path | `AiGroundingIT`; E2E | `grounded=false`, states why | VERIFIED |
| Refuses empty comparison baseline | `AiAnalyst` | E2E | `grounded=false` | VERIFIED |
| Off-topic question handled | `AiText` | `AiTextTest`; E2E | `UNRECOGNISED` | VERIFIED |
| Empty question rejected | Validation | E2E | 400 | VERIFIED |
| FACT / SUGGESTION separation | `AiAnalyst` | E2E asserts both sections | Both present | VERIFIED |
| Transcript readable | `AiChatController` | E2E | 200, 2 turns | VERIFIED |
| Deterministic local engine, no external call | `AiProviderClient` gated on `AI_PROVIDER` | `AiGroundingIT` runs `LOCAL` | No outbound call | VERIFIED |
| External provider path | `AiProviderClient` | Never executed | Untested code | GAP |

## Feedback

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Submit ticket | `FeedbackController` | E2E | 201 | VERIFIED |
| New ticket starts OPEN | Default status | E2E | `OPEN` | VERIFIED |
| Too-short content rejected | Validation | E2E | 400 | VERIFIED |
| List own tickets | Owner filter | E2E | Returns own | VERIFIED |
| Admin lists all tickets | `AdminController` | E2E; contract suite | Ticket visible | VERIFIED |
| Admin triages status | `FeedbackTicketService` | E2E; contract suite | `RESOLVED` | VERIFIED |
| Admin reply stored and visible to user | `FeedbackTicketService` | E2E; contract suite | Reply text round-trips | VERIFIED |
| Status filter | `AdminController` | Contract suite | OPEN filter accepted | VERIFIED |
| Android submission UI | — | — | No screen, no repository method | GAP |

## Administration

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Admin login | `AuthService` | E2E; contract suite | 200, role `ADMIN` | VERIFIED |
| Admin dashboard loads | `AdminController` | E2E; contract suite | 200 | VERIFIED |
| Total users metric | `SystemMetricsService` | Contract suite field assertions | Field present | VERIFIED |
| Total transactions metric | `SystemMetricsService` | E2E | Matches expectation | VERIFIED |
| AI questions metric | `SystemMetricsService` | Contract suite | `totalQuestions` | VERIFIED |
| Runtime counters labelled since-restart | `SystemMetricsService` | E2E; contract suite | `uptimeSinceRestart` | VERIFIED |
| List users with paging | `AdminController` | E2E; contract suite | `content`, `totalElements`, `totalPages` | VERIFIED |
| Search users | `UserSpecifications` | Contract suite | 200 | VERIFIED |
| Filter by status | `UserSpecifications` | Contract suite | Locked filter returns only locked | VERIFIED |
| Lock user | `AdminService` | E2E | `LOCKED` | VERIFIED |
| Unlock user | `AdminService` | E2E | `ACTIVE`, login restored | VERIFIED |
| Transaction count per user | `AdminService` | Contract suite | `transactionCount` | VERIFIED |
| Create system category | `AdminService` | E2E; contract suite | 200, `usageCount` | VERIFIED |
| Deactivate system category | `AdminService` | E2E; contract suite | `active=false` | VERIFIED |
| Non-admin blocked from category creation | `SecurityConfig` | E2E | 403 | VERIFIED |
| Audit trail readable | `AuditController` | E2E; contract suite | Entries present | VERIFIED |
| Audit records category create | `AuditService` | E2E; contract suite | Action recorded | VERIFIED |
| Audit records category update | `AuditService` | E2E; contract suite | Action recorded | VERIFIED |
| Audit records feedback triage | `AuditService` | E2E; contract suite | Action recorded | VERIFIED |
| Audit records lock and unlock | `AuditService` | E2E | `ADMIN_LOCK_USER`, `ADMIN_UNLOCK_USER` | VERIFIED |
| System metrics endpoint | `AdminController` | Contract suite | 7 fields asserted | VERIFIED |

## Infrastructure

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| PostgreSQL 16 as system of record | `docker-compose.yml` | Live container | Running | VERIFIED |
| 9 migrations apply cleanly | Flyway V1–V9 | `MigrationsAndBootstrapIT` | 9 successful | VERIFIED |
| 11 tables created | Migrations | `MigrationsAndBootstrapIT`; `psql` | 11 | VERIFIED |
| Schema validated against entities | `ddl-auto: validate` | `MigrationsAndBootstrapIT` | Startup succeeds | VERIFIED |
| Bootstrap reference data | `V9`, `BootstrapDataInitializer` | `MigrationsAndBootstrapIT` | 2 roles, 14 categories | VERIFIED |
| Backend waits for database health | `depends_on: service_healthy` | Compose startup | Ordered startup observed | VERIFIED |
| Health endpoint | Actuator | E2E; manual | `{"status":"UP"}` | VERIFIED |
| Three container images build | Dockerfiles | `docker compose build` | All built | VERIFIED |
| Clean rebuild works | Compose | `down && build && up -d` | 3 healthy | VERIFIED |
| **All containers healthy** | Compose/Dockerfile | `docker compose ps` | **healthy ×3** after D-01 fix | VERIFIED |
| Backend error-free startup | — | `docker compose logs` | 0 ERROR lines | VERIFIED |
| Environment-driven configuration | `application.yml`, `.env` | Startup | All keys from environment | VERIFIED |
| Compose fails fast on missing secrets | `${VAR:?message}` | Configuration inspection | Build-time enforcement | VERIFIED |
| Source control repository | — | — | **No `.git` directory** | GAP |
| CI pipeline | — | — | Does not exist | GAP |

## Quality

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Unit tests | 7 test classes | `mvn test` | 98 pass | VERIFIED |
| Integration tests against real PostgreSQL | Testcontainers | `mvn verify` | 15 pass | VERIFIED |
| End-to-end API coverage | `tests/e2e-api-tests.ps1` | 160 assertions | 160/160 | VERIFIED |
| Admin contract coverage | `tests/admin-web-contract.ps1` | 102 assertions | 102/102 | VERIFIED |
| Android unit tests | `FormatTest` | Gradle | 10 pass | VERIFIED |
| Negative/validation tests | Both suites | ~25 negative assertions | All pass | VERIFIED |
| Typecheck | `tsc --noEmit` | `npm run build` | No errors | VERIFIED |
| Dependency vulnerability scan | `npm audit` | — | 0 vulnerabilities | VERIFIED |
| Build reproducibility | — | Clean rebuild | Successful | VERIFIED |
| Regression protection | Surefire/Failsafe split | `mvn verify` | Both phases run | VERIFIED |
| UI test automation | — | — | Does not exist | GAP |
| Load/performance testing | — | — | Does not exist | GAP |

## Security

| Requirement | Implementation | Test | Evidence | Status |
|---|---|---|---|---|
| Password hashing | BCrypt cost 12 | `MigrationsAndBootstrapIT` | Hashes only | VERIFIED |
| No hash in migrations | V1–V9 | Grep | No hash present | VERIFIED |
| `.env` excluded from source | `.gitignore` | File inspection | Listed first | VERIFIED |
| `.env.example` carries no real secret | `.env.example` | Inspection | Placeholders only | VERIFIED |
| Test secret is a throwaway constant | `application-test.yml` | Source inspection | Fixed dummy, commented | VERIFIED |
| No secret in frontend source | `admin-web/src` | Grep | None found | VERIFIED |
| No secret in Android source | `android/src` | Grep | None found | VERIFIED |
| JWT secret required at startup | `SecurityConfig`/compose | Startup | Fails without it | VERIFIED |
| Secrets from environment only | `application.yml` | Source inspection | No default secret | VERIFIED |
| Stateless session | `SessionCreationPolicy.STATELESS` | Source inspection | Confirmed | VERIFIED |
| CSRF disabled (appropriate for bearer tokens) | `SecurityConfig` | Source inspection | Confirmed | VERIFIED |
| Admin routes role-protected | `SecurityConfig` | E2E; contract | 403 for non-admin | VERIFIED |
| Owner-scoped queries | All user services | E2E | 404 across resources | VERIFIED |
| Stack traces hidden from clients | `include-stacktrace: never` | Source inspection | Confirmed | VERIFIED |
| Log redaction review | — | — | Not performed | GAP |
| Rate limiting | — | — | Not implemented | GAP |
| TLS termination | — | — | Not implemented | GAP |

---

## Summary counts

| Status | Count |
|---|---|
| VERIFIED | 147 |
| PARTIAL (implemented, not fully asserted) | 8 |
| GAP (not implemented) | 9 |
| **Total rows** | **164** |

Counted programmatically from this file, not estimated.

The 9 gaps are: external AI provider path, Android feedback UI, source control,
CI, UI test automation, load testing, rate limiting, TLS, log-redaction review.
None of them is a failure of a specified behaviour; they are missing practices
listed in [`known-limitations.md`](../known-limitations.md).