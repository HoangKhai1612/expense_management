# Known limitations

Everything here is a real gap, not a design choice. Cross-referenced with
[`acceptance-audit.md`](acceptance-audit.md).

## Testing

**No UI test automation.** The admin console and the Android app are verified by
TypeScript typecheck, Gradle build and manual walkthrough. There is no Selenium
or Espresso suite, so a rendering regression would not be caught by any command
in the repository.

**No load or performance testing.** There is no evidence about behaviour under
concurrent users: no p95 latency figures, no throughput numbers, no evidence
that the AI endpoint degrades gracefully under load.

**No security scanning or penetration test.** `npm audit` reports 0 known
vulnerabilities in the console's dependencies and that is the entire extent of
the security verification. No SAST, no DAST, no dependency review for the
backend, no penetration test.

**No CI.** Every command must be run by hand. Nothing prevents a change from
landing with a failing suite.

**Single database engine.** Only PostgreSQL 16 is exercised. The schema avoids
vendor-specific types, but that portability is unproven.

**No failure-injection tests.** Nothing verifies behaviour when the database
becomes unavailable mid-transaction.

## Deployment

**Compose is not an orchestrator.** No Kubernetes manifests, no replica counts,
no rolling updates, no resource limits, no readiness-based traffic management.

**No TLS.** The backend speaks plain HTTP and handles no certificates. TLS must
be terminated in front of it, which is fine, but it means the service cannot be
deployed without that layer.

**Secrets live in `.env` for local work.** Adequate for development, inadequate
for production. There is no secret manager integration.

**No backups.** The Compose named volume has no backup or restore automation.
Data loss is unrecoverable.

**No log aggregation or alerting.** Logs go to stdout. Actuator exposes metrics
but nothing scrapes them. No traces.

**Log redaction unverified.** The appender pattern is the Spring Boot default. A
deliberate review for token and password leakage in log output has not been
done.

## Backend

**Single currency.** Amounts are stored as `NUMERIC(19,2)` with no currency
code. Mixed-currency accounts cannot be represented, and no conversion exists.

**AI answers are English only.** `AiText` matches English and Vietnamese keywords
only. Other languages fall through to `UNRECOGNISED` and the assistant declines
rather than guessing, which is safe but unhelpful.

**Time zone handling is thin.** Dates are stored as `DATE` and periods are
computed in UTC. A user whose local date differs from UTC across midnight can
see a boundary day land in the wrong period.

**No refresh tokens.** Access tokens last `JWT_EXPIRATION` (default 12h) with no
refresh mechanism. The client re-authenticates when a token expires.

**No rate limiting.** Login and AI chat are unlimited. For a local or
single-tenant deployment this is acceptable; it is not acceptable on the public
internet.

**No pagination cap enforced at the database level.** Page size is bounded in
validation, but large pages still translate into broad queries.

**Soft deletes are never purged.** Deactivated categories, deactivated users and
soft-deleted rows accumulate indefinitely. There is no retention job.

**No email verification or password reset.** Both are absent because no mail
transport is configured. Adding them requires an external mail service.

## Admin console

**No role granularity.** Every administrator can lock users, edit the category
catalogue and change system settings. There is no read-only or support role.

**Client-side route guards only.** Route protection is enforced in React Router.
The real enforcement is the `ROLE_ADMIN` check on the backend; the UI guard is
convenience, not security.

**No optimistic concurrency.** A category rename has no version check, so two
administrators editing the same category produce a last-write-wins result.

**No dark mode, localisation, or responsive refinement** beyond basic styling.

## Android

**Debug build only.** No release signing config, no ProGuard/R8 rules beyond the
defaults, no Play Store packaging.

**No offline mode.** The app requires a live backend. There is no local cache, so
it shows an error state with no data when the network is unavailable.

**Plain token storage.** The session token is held in memory/`SharedPreferences`
rather than `EncryptedSharedPreferences` or the Keystore. On a rooted device this
is readable. Acceptable for a development build, not for release.

**No instrumented tests.** Only `FormatTest` exists, covering currency and date
formatting. No UI, navigation or repository test.

**No accessibility audit.** Content descriptions are present on interactive
elements but have not been verified with TalkBack.

## AI assistant

**No streaming.** Responses arrive complete. Long answers feel slow on mobile.

**The local analyst is not a language model.** It is deterministic intent
classification plus templated aggregation. It is accurate and grounded, but it
cannot answer anything outside the six implemented intents — and for an
unexpected question it correctly declines rather than improvising.

**No external provider has been exercised.** `AI_PROVIDER` supports an external
API, but only `LOCAL` has been run. The provider path is unverified code.

**No conversation memory across devices.** Conversations are per-user and stored
in the database, but there is no synchronisation of local UI state beyond
reloading from the server.

**No prompt-injection surface, by construction.** There is no text-to-SQL path
and the user id is a required service argument rather than something parsed from
the question, so the assistant cannot be steered toward another account. This is
a structural property worth keeping if the design ever changes.