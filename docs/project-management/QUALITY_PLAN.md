# Quality plan

Maps each quality requirement to a quality criterion, the test that checks it,
the measured result, and the evidence file. Every "Result" column value was read
from a test report produced during the final audit on 2026-10-01.

## Quality objective

The system must be demonstrably correct on the behaviours the specification
names, must not leak one user's data to another, must never state an ungrounded
AI figure, and must be deployable by a second developer from documented commands.

## 1. Functional correctness

| Requirement | Quality criterion | Test | Result | Evidence |
|---|---|---|---|---|
| Authentication works | Register and log in by email and username return a usable token | `AuthServiceTest` (11) + E2E | PASS | `e2e-20261001-144857.csv` |
| Invalid credentials rejected | Wrong password and unknown account both return `INVALID_CREDENTIALS` | `AuthServiceTest` + E2E | PASS | Same |
| Account status enforced | Locked and deactivated accounts cannot log in | `AuthServiceTest` + E2E | PASS | Same |
| Categories managed | System catalogue and personal categories created, updated, deactivated | `CategoryServiceTest` (13) + E2E + contract | PASS | contract CSV |
| Transactions managed | Full CRUD with filters | `TransactionServiceTest` (19) + E2E | PASS | e2e CSV |
| Budgets computed | Usage, remaining, status derived from real transactions | `BudgetServiceTest` (15) + E2E | PASS | e2e CSV |
| Statistics correct | Totals reconcile with individual transactions | E2E assertions | PASS | e2e CSV |
| Feedback round trip | Submit, triage, reply, user sees the reply | E2E + contract | PASS | Both CSVs |
| Admin operations | All admin endpoints functional and audited | E2E + contract | PASS | Both CSVs |

## 2. Security

| Requirement | Quality criterion | Test | Result | Evidence |
|---|---|---|---|---|
| Password confidentiality | No plaintext password stored anywhere | `MigrationsAndBootstrapIT` | PASS | Test asserts hashes |
| Token integrity | Forged or malformed tokens rejected | E2E (3 assertions) | PASS | e2e CSV |
| Token revocation | A token issued before a lock stops working immediately | E2E | PASS | e2e CSV |
| Route authorisation | Non-admins blocked from every admin route | E2E + contract (3 assertions) | PASS | Both CSVs |
| Data isolation | No cross-user read, update, delete, transcript or feedback access | E2E (7 assertions) | PASS | e2e CSV |
| Category confidentiality | Another user's category returns 403 without disclosing its name | `CategoryServiceTest` | PASS | Test report |
| Privilege self-restriction | Admin cannot lock their own account; the refusal is audited | E2E (2 assertions) | PASS | e2e CSV |
| Error detail leakage | Stack traces never returned to clients | `include-stacktrace: never` | PASS | Configuration inspection |
| Secret hygiene | No secret in frontend, Android, migrations or `.env.example` | Grep sweep | PASS | No matches |
| Secret management | JWT secret required from environment, no default | Compose `:?` enforcement | PASS | Startup behaviour |
| **Log redaction** | Tokens and passwords absent from logs | **No test exists** | **NOT PERFORMED** | Gap L-05 |

## 3. Reliability and correctness of the AI

This is the highest-risk area, because a plausible but wrong number is worse
than an error message.

| Requirement | Quality criterion | Test | Result | Evidence |
|---|---|---|---|---|
| Grounded figures | Every reported number equals the stored aggregate | `AiGroundingIT` (8) | PASS | Failsafe report |
| Fact persistence | Numeric claims stored with the message | E2E | PASS | e2e CSV |
| Cross-user grounding | Snapshot never includes another user's rows | `AiGroundingIT` | PASS | Failsafe report |
| Honest refusal | No data → refuses rather than estimates | `AiGroundingIT` + E2E | PASS | e2e CSV |
| Honest refusal, empty baseline | Empty comparison period → `grounded=false` | E2E | PASS | e2e CSV |
| Off-topic handling | Unrecognised question → `UNRECOGNISED`, not a guess | `AiTextTest` (24) + E2E | PASS | e2e CSV |
| Fact/judgement separation | Every answer has distinct FACT and SUGGESTION sections | E2E (2 assertions) | PASS | e2e CSV |
| Regression protection | The analyst defect is caught if reintroduced | Deliberate defect injection | PASS | 4 failures observed, then fix restored |

## 4. Maintainability

| Requirement | Quality criterion | Method | Result | Evidence |
|---|---|---|---|---|
| Layer separation | Services own logic; controllers are thin; errors flow through one handler | Code review | PASS | `GlobalExceptionHandler`, service-only validation |
| Consistent error contract | One error envelope with a machine code | E2E asserts codes | PASS | e2e CSV |
| Consistent pagination | One `PageResponse<T>` shape for all list endpoints | Contract suite | PASS | contract CSV |
| Documentation currency | Docs match implementation | Documentation audit | **PARTIAL** | 3 discrepancies: D-04, D-05 open |
| Schema ownership | Migrations are the only schema authority | `ddl-auto: validate` | PASS | Startup fails on drift |
| Configuration externalised | No hardcoded secrets or URLs | Grep sweep | PASS | No matches |

## 5. Build reproducibility

| Requirement | Quality criterion | Method | Result | Evidence |
|---|---|---|---|---|
| Backend builds from clean | `mvn verify` succeeds | Fresh run | PASS | BUILD SUCCESS |
| Console builds from clean | `tsc` + Vite succeed | Fresh run | PASS | 33 modules, 203.39 kB |
| Images build from clean | `docker compose build` | After `down` | PASS | 3 images built |
| Stack starts from clean | `docker compose up -d` | After `down` | PASS | 3 healthy |
| Schema initialises automatically | Migrations run without manual SQL | Fresh volume | PASS | 9 applied, 11 tables |
| Dependency resolution pinned | Versions locked | `package-lock.json` present | PASS | File exists |
| Android builds | APK produced | Gradle | PASS | 20,318,592 bytes |

## 6. Integration

| Requirement | Quality criterion | Test | Result | Evidence |
|---|---|---|---|---|
| Database integration | Migrations and seed work on real PostgreSQL | `MigrationsAndBootstrapIT` (7) | PASS | Failsafe report |
| Container integration | Backend connects to PostgreSQL over the compose network | Live container check | PASS | `psql` via `exec` |
| Frontend–backend contract | Console types match live responses | Contract suite (102) | PASS | contract CSV |
| Backend health | `/actuator/health` reports UP | E2E + manual | PASS | e2e CSV |
| Android–backend contract | Retrofit paths match real endpoints | Source comparison | PASS | `FinanceApi` vs OpenAPI |
| **External AI provider** | Non-`LOCAL` provider path works | **No test exists** | **UNVERIFIED** | Gap |

## 7. Regression testing

| Requirement | Quality criterion | Method | Result | Evidence |
|---|---|---|---|---|
| Automated regression | Every change re-runnable without manual steps | Suites are scriptable | PASS | 5 suites, all scripted |
| Idempotent E2E | Suite can run repeatedly without drift | Unique identifiers, baseline-relative counts | PASS | Passed 5+ times consecutively |
| Unit/integration separation | Machine without Docker can still run tests | Surefire/Failsafe split | PASS | `mvn test` needs no Docker |
| Multiple environments | Suite passes against host and container | Run against both | PASS | 160/160 and 102/102 in both |
| Defect regression tests | Every fixed defect has a test | 3 defects, 3 tests | PASS | See Chapter 2 evidence |
| **Continuous regression** | Tests run on every change | **No CI exists** | **ABSENT** | Gap L-01 |

## 8. Deployment quality

| Requirement | Quality criterion | Method | Result | Evidence |
|---|---|---|---|---|
| Single-command startup | `docker compose up -d` | Clean run | PASS | 3 healthy |
| Ordered startup | Backend waits for database health | `depends_on: service_healthy` | PASS | Startup order observed |
| Health signalling | Every container reports healthy | `docker compose ps` | PASS | **3/3 healthy after D-01 fix** |
| Clean error logs | No exceptions on startup | Log scan | PASS | 0 ERROR lines |
| Documentation of deployment | Commands and variables documented | `deployment.md` | PASS | Documented |
| **TLS** | Encrypted transport | Not implemented | **ABSENT** | Gap L-02 |
| **Backups** | Recoverable data | Not implemented | **ABSENT** | Gap L-03 |

## Quality targets: proposed vs measured

Targets are stated as the criterion actually used, not as a percentage.

| Criterion | Target | Measured | Met |
|---|---|---|---|
| Backend test failures | 0 | 0 of 113 | Yes |
| E2E failures | 0 | 0 of 160 | Yes |
| Contract failures | 0 | 0 of 102 | Yes |
| Android test failures | 0 | 0 of 10 | Yes |
| Known npm vulnerabilities | 0 | 0 | Yes |
| Containers healthy | 3 of 3 | 3 of 3 | Yes |
| Errors in startup log | 0 | 0 | Yes |
| Migrations applied | 9 of 9 | 9 of 9 | Yes |
| Cross-user leakage | 0 cases | 0 of 7 probed | Yes (not exhaustive) |
| Ungrounded AI figures | 0 cases | 0 of 5 probed | Yes (not exhaustive) |

## Quality gaps summary

Eight quality activities are absent: CI, UI automation, load testing, security
scanning, penetration testing, log-redaction review, TLS, and backup
verification. Each is recorded in [`known-limitations.md`](../known-limitations.md)
and none is claimed as satisfied anywhere in this documentation set.