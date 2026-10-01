# Risk register

Scored likelihood x impact, 1–5. Anything scoring 15 or above needs a plan.

| # | Risk | L | I | Score | Mitigation / status |
|---|---|---|---|---|---|
| 1 | AI produces an ungrounded figure | 3 | 5 | 15 | **Mitigated.** Snapshot service, no text-to-SQL, per-intent fact tests, `AiGroundingIT` asserts values not wording |
| 2 | Cross-user data leakage | 2 | 5 | 10 | **Mitigated.** Owner filters in every query; user id is a service argument, not a request parameter; E2E 404/403 cases |
| 3 | Category deletion breaks transaction history | 4 | 4 | 16 | **Avoided by design.** Deactivate instead of delete; E2E asserts history survives |
| 4 | Locked user keeps access via a live token | 3 | 4 | 12 | **Mitigated.** Status re-checked per request. E2E asserts a pre-lock token is rejected with 401 after locking |
| 5 | Migration fails on a partially migrated database | 2 | 5 | 10 | **Mitigated.** Flyway checksum validation, `ddl-auto: validate`, `MigrationsAndBootstrapIT` |
| 6 | Secret leakage via logs | 3 | 4 | 12 | **Open.** Appender pattern is the Spring Boot default; no redaction review done |
| 7 | Secrets committed to version control | 1 | 5 | 5 | **Mitigated.** `.env` gitignored; no hash in migrations; bootstrap from environment only |
| 8 | Budget alert spam | 3 | 3 | 9 | **Mitigated.** E2E asserts the warning alert is raised exactly once across a recomputation |
| 9 | Unbounded page size degrades performance | 2 | 3 | 6 | **Partly mitigated.** Page size validated; no database-level cap |
| 10 | No CI, so a failing change lands | 3 | 4 | 12 | **Open.** All verification is manual |
| 11 | External AI provider path is unverified | 2 | 3 | 6 | **Accepted.** Only `LOCAL` is supported in practice; documented |
| 12 | No backups — data loss unrecoverable | 2 | 5 | 10 | **Open.** Compose volume only; no dump or restore automation |
| 13 | No rate limiting on login or AI chat | 3 | 4 | 12 | **Open.** Acceptable locally, not on the public internet |
| 14 | Time zone boundary puts a day in the wrong period | 2 | 3 | 6 | **Accepted.** Periods computed in UTC; documented |
| 15 | No automated UI regression testing | 3 | 3 | 9 | **Partly mitigated.** Contract suite covers the data layer the UI consumes |

## Highest-ranked open items

1. **R10 — no CI.** Everything depends on someone remembering to run the suites.
   A GitHub Actions workflow running `mvn verify`, `npm run build` and the two
   PowerShell suites would close this.
2. **R6 — log redaction.** Cheap to review, and the consequence of getting it
   wrong is credential exposure.
3. **R12 — no backups.** Unrecoverable data loss is the worst outcome available
   to a system holding financial records.
4. **R13 — no rate limiting.** Blocks any public deployment.
5. **R8 / R4 — missing test cases.** Both have working implementations; the gap
   is proof, not behaviour.

## Retired risks

| Risk | Resolution |
|---|---|
| Immutable-list mutation broke the spending analyst | Found in review, fixed with `new ArrayList<>(...)`, regression test confirmed to fail against the original defect |
| E2E suite not re-runnable | Made idempotent: unique accounts and category codes, baseline-relative audit counts. Passed twice consecutively |
| Admin console types could drift from the API | Contract suite added; 102/102 passing |
| Admin console did not exist | Built from scratch; typechecks and bundles |