# Effort and schedule data gap

**Purpose:** to state honestly what can and cannot be reconstructed about project
effort and schedule, so no figure in the report is invented.

## Headline finding

> **Effort and schedule history: `UNKNOWN / NOT RECORDED`.**
>
> There is no version control history, no issue tracker, no task board and no
> dated project artifact in this repository. Effort in person-hours or person-days
> **cannot** be derived from available evidence, and it is not estimated here.

## Why: the evidence that does not exist

| Expected source | Present? | Evidence |
|---|---|---|
| Git repository | **No** | `git rev-parse` → `fatal: not a git repository`. No `.git` in the project or any parent |
| Commit history | **No** | Nothing to enumerate. `.gitignore` exists, showing intent, but the repo was never initialised |
| Branches / tags | **No** | None exist |
| Issue tracker (Jira, GitHub Issues, etc.) | **No** | No configuration, no references in any file |
| Task board / backlog | **No** | No artefact of any kind |
| Sprint artefacts | **No** | No sprint notes, standups, retrospectives |
| Estimates in source | **No** | No `@ estimate` annotations, no task metadata |
| Dated design documents | **No** | Every design document was authored during the final phase (2026-10-01) and describes the finished state, not the path to it |
| File modification times | Partially useful | Reflect the last write only; a bulk copy or a single session rewrites them and they are not a reliable effort proxy |
| Test result CSV timestamps | Partially useful | Show when suites ran, not how much work preceded them |

## What CAN be reconstructed with evidence

These are **counts of artifacts**, not estimates of effort. They are verifiable
and safe to report.

### Measurable artifact counts

| Measure | Value | Source |
|---|---|---|
| Backend main source files | 78 `.java` files under `com/finai` | `Get-ChildItem` count |
| Backend main source lines | 5,500 | Sum of `Measure-Object -Line` |
| Backend test files | 11 (8 `*Test`/`*IT` + 3 support) | `src/test/java` |
| Database migrations | 9 | `db/migration/V1`–`V9` |
| Database tables | 11 | `flyway_schema_history` + `information_schema` |
| REST endpoints | 39 paths | Live `/v3/api-docs` |
| Backend packages | 16 | `com.finai/*` directories |
| Admin console source files | 14 (`.ts`/`.tsx`/`.css`) | `admin-web/src` |
| Admin console source lines | 1,635 | Sum of line counts |
| Android source files | 22 `.kt` | `android/app/src/main` |
| Android source lines | 2,668 | Sum of line counts |
| Android screens | 6 | `ui/screens` |
| Test assertions, E2E | 160 | `e2e-20261001-144857.csv` |
| Test assertions, contract | 102 | `admin-web-contract-20261001-144700.csv` |
| Backend tests | 113 | Surefire (98) + Failsafe (15) reports |
| Android tests | 10 | `TEST-com.finai.mobile.FormatTest.xml` |
| Total verified test cases | 375 assertions (113 + 160 + 102) + 10 Android | Sum of reports |
| Documentation files | 26 `.md` under `docs/` | `Get-ChildItem docs -Recurse -Filter *.md` |
| Defects found and fixed during this project | 3 | D-01, `AiAnalyst`, `Format.kt` |

### Verified schedule *boundaries* only

| Fact | Value | Evidence |
|---|---|---|
| Latest file modification in the project | 2026-10-01 | Filesystem |
| Test result CSVs span | 2026-09-30 23:37 → 2026-10-01 14:48 | `tests/results/` |
| Container `finai-postgres` created | 15 hours before the final audit | `docker compose ps` |
| Docker image ages at audit | backend 11 min (rebuilt), admin 21.3 MB, postgres image 13 days | `docker compose images` |

The CSV timestamps show that the *verification phase* spanned roughly
2026-09-30 23:37 to 2026-10-01 14:48 — about 15 hours of wall-clock time. That
is the only defensible time measurement in the repository, and it measures
testing and documentation, not the whole project.

## What CANNOT be stated

Do not report any of the following, because no evidence supports them:

- Total project duration in days or weeks
- Effort in person-hours or person-days
- Number of developers or team size
- Task or story counts beyond the 16 phases in `project-plan.md`
- Sprint or iteration count
- Percentage of work completed per week
- Any "velocity", burndown, or "on track / behind schedule" assessment

## Impact on the report

| Report claim | Can it be supported? |
|---|---|
| "The project comprises 4 deployable components" | **Yes** — filesystem evidence |
| "The schema has 11 tables defined by 9 migrations" | **Yes** — database evidence |
| "375 backend test assertions and 10 Android tests pass" | **Yes** — report evidence |
| "The API exposes 39 endpoints" | **Yes** — OpenAPI evidence |
| "The project took N weeks" | **No** |
| "The team spent N person-hours" | **No** |
| "Development followed 16 phases" | **Partially** — the phases are documented as a *plan*; their completion dates are not recorded |
| "The project was delivered ahead of schedule" | **No** — no schedule exists to compare against |

## Recommendation for future projects

1. `git init` and commit from the first day. Three defects here were found and
   fixed without any regression history to prove they were the only ones.
2. Record estimates and actuals per task in the issue tracker.
3. Keep a dated decision log for design changes.
4. Baseline the deployment with tags (`v0.1.0`) so a release is identifiable.

Initialising Git now would produce one commit containing the finished system.
That would be a *false* history rather than an absent one, so it is deliberately
not done. It is recorded as limitation **L-07** and as gap **CF-01** in
[`BASELINE.md`](BASELINE.md).