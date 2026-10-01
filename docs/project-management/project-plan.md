# Project plan

Scope: a personal finance tracker with a grounded AI assistant, an Android
client, and an administrator console.

## Product goal

Users record income and expenses, set budgets, and ask questions about their own
data. Administrators get a separate console for accounts, the category
catalogue, feedback and the audit trail.

The constraint that shaped the design: **the assistant never invents a number.**
Every figure it reports is a parameterised aggregate over the authenticated
user's own rows, and answers are split into verified facts and judgement.

## Phases

| # | Phase | Deliverable | Verified by |
|---|---|---|---|
| 0 | Foundations | Project skeleton, toolchain, Compose | `docker compose ps` |
| 1 | Database | 9 Flyway migrations, 11 tables, seed data | `MigrationsAndBootstrapIT` |
| 2 | Security | JWT, register/login, role guards, lock/deactivate | `AuthServiceTest`, `JwtServiceTest`, E2E |
| 3 | Users and profiles | Profile read/update, password change | E2E |
| 4 | Categories | System catalogue + personal, deactivate-not-delete | `CategoryServiceTest`, E2E |
| 5 | Transactions | CRUD, validation, filters, recent | `TransactionServiceTest`, E2E |
| 6 | Budgets | Periods, usage, thresholds, one-shot alerts | `BudgetServiceTest`, E2E |
| 7 | Notifications | List, unread count, read/mark-all | E2E |
| 8 | Statistics | Overview, by-category, monthly, daily | E2E |
| 9 | AI assistant | Snapshot service, analyst, intents, chat persistence | `AiTextTest`, `AiAnalystTest`, `AiGroundingIT`, E2E |
| 10 | Feedback | Submit, list, detail | E2E |
| 11 | Administration | Users, categories, feedback, dashboard, system | E2E |
| 12 | Audit | Audit trail on privileged actions | E2E |
| 13 | Admin console | React app, auth, 6 pages, contract tests | `npm run build`, 102/102 contract |
| 14 | Android | Compose app, 7 screens, navigation | `testDebugUnitTest assembleDebug` |
| 15 | Verification and docs | E2E suite, contract suite, docs, audit | See `../acceptance-audit.md` |

## Milestones

| Milestone | Contents | State |
|---|---|---|
| M1 — Data foundation | Phases 0–1 | Complete |
| M2 — Core finance | Phases 2–8 | Complete |
| M3 — AI assistant | Phases 9–10 | Complete |
| M4 — Administration | Phases 11–12 | Complete |
| M5 — Clients | Phases 13–14 | Complete |
| M6 — Verification | Phase 15 | Complete |

## Design decisions

**No text-to-SQL.** `AiDataSnapshotService` builds a typed snapshot of the
caller's rows; the analyst reasons only over that. This removes injection and
accidental-write risk, and makes the user id a required argument rather than
something parsed from the question — so the assistant cannot be steered toward
another account.

**Facts are persisted with the message.** Every figure the assistant emits is
stored next to the answer, giving an audit trail linking what the user was told
to what the database actually held.

**Categories are deactivated, never deleted.** A hard delete would break the
foreign key from `transactions` or force a decision about other users' history.

**Locking a user invalidates their tokens immediately.** The filter re-checks
account status on every request, so a pre-lock token stops working without
waiting for expiry.

**Runtime metrics are labelled process-scoped.** The admin dashboard exposes
`uptimeSinceRestart` so counters that reset on restart are not read as
all-time figures.

**Surefire and Failsafe are separated.** Integration tests need Docker; keeping
them out of the default lifecycle means `mvn test` works on a machine without it.

## Scope not delivered

UI test automation, CI, performance testing, security scanning, Kubernetes
manifests, TLS, backup automation, log aggregation. Each is listed with a reason
in `../known-limitations.md`.

## Risks

See [`risk-register.md`](risk-register.md).