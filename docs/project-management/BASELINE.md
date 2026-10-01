# Baseline

Frozen reference point for the final audit. Every value below was read from the
running system or the repository on the date shown, not copied from earlier
documentation.

## Identification

| Field | Value |
|---|---|
| Baseline ID | `BASELINE-2026-10-01-A` |
| Date | 2026-10-01 (final audit run 14:46–14:50 local) |
| Project root | `C:\Users\Administrator\AndroidStudioProjects\expense_management` |
| Git commit | **NOT RECORDED — project is not a Git repository** |
| Branch | **NOT RECORDED** — no `.git` directory exists |
| Deployment mode | Docker Compose (containerised) |

> **Finding CF-01.** There is no `.git` directory anywhere in the project or its
> parents, so there is no commit history, no branch, no tag and no authorship
> record. Git 2.55.0 is installed at `C:\Program Files\Git\cmd\git.exe` but is not
> on `PATH`, and the repository was never initialised. This is a genuine gap
> against the source-control requirement and is not recoverable retrospectively.

## Component versions

| Component | Version | Evidence |
|---|---|---|
| Backend artifact | `finance-ai-backend` **0.1.0** | `backend/pom.xml` |
| Backend framework | Spring Boot **3.5.16** | `spring-boot-starter-parent` |
| Java | **21** | `<java.version>21</java.version>` |
| Admin console | `personal-finance-ai-admin-web` **0.1.0** | `admin-web/package.json` |
| Android application | versionName **1.0**, versionCode **1**, applicationId `com.finai.mobile` | `android/app/build.gradle.kts` |
| Android SDK | minSdk 24, targetSdk 37, compileSdk 37 | `android/app/build.gradle.kts` |
| Database | PostgreSQL **16-alpine** | `docker-compose.yml` |
| Database schema | Flyway **V9** (9 successful migrations) | `flyway_schema_history` |
| Docker Engine | **29.6.2** | `docker version` |
| Node.js | 24.11.0 | earlier run |
| Gradle | 9.6.0 | build output |
| Gradle daemon JVM | pinned to **25** by `android/gradle/gradle-daemon-jvm.properties` | file contents |

## Docker images

| Image | Tag | Image ID | Size |
|---|---|---|---|
| `personal-finance-ai-backend` | 0.1.0 | `fcb6566a0a6c` | 191 MB |
| `personal-finance-ai-admin` | 0.1.0 | `deb90825248e` | 21.3 MB |
| `postgres` | 16-alpine | `721873c34ceb` | 116 MB |

## Container status (clean rebuild, 2026-10-01 14:47)

| Container | Status | Ports |
|---|---|---|
| `finai-postgres` | healthy | 5433→5432 |
| `finai-backend` | healthy | 8081→8081 |
| `finai-admin-web` | healthy | 5173→80 |

## Schema state

- Flyway migrations applied: **9**, all `success = t`
- Tables in `public` schema: **11** (10 domain tables + `flyway_schema_history`)
- Bootstrap data verified by `MigrationsAndBootstrapIT`: 2 roles, 14 categories,
  1 administrator, passwords stored hashed

## Test baseline

Every row was executed during the final audit, not inherited.

| Suite | Command | Result |
|---|---|---|
| Backend unit + integration | `mvn verify` | 98 + 15 = **113 tests, 0 failures, 0 errors** |
| Admin console build | `npm run build` | Success (`tsc --noEmit` + Vite, 33 modules, 203.39 kB JS) |
| Dependency scan | `npm audit` | **0 vulnerabilities** |
| Android unit | `./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug` | **10 tests, 0 failures** |
| Android debug APK | same | `app-debug.apk`, 20,318,592 bytes |
| E2E API | `tests/e2e-api-tests.ps1` | **160/160 passed** |
| Admin contract | `tests/admin-web-contract.ps1` | **102/102 passed** |
| Docker clean rebuild | `docker compose down && build && up -d` | 3 images built, 3 containers healthy |
| Backend log scan | `docker compose logs backend` | 0 ERROR / Exception lines |

Result CSVs: `tests/results/e2e-20261001-144857.csv`,
`tests/results/admin-web-contract-20261001-144700.csv`.

## Known limitations at baseline

Carried forward and re-verified; see
[`known-limitations.md`](../known-limitations.md) for detail.

| # | Limitation | Status |
|---|---|---|
| L-01 | No CI pipeline | Open |
| L-02 | No TLS termination | Open |
| L-03 | No production backup strategy | Open |
| L-04 | No rate limiting on login or AI | Open |
| L-05 | Log-redaction review not completed | Open |
| L-06 | No UI test automation | Open |
| L-07 | No source control (no Git repository) | Open — found by this audit |
| L-08 | No Android feedback UI | Open — found by this audit |
| L-09 | `/api/auth/me` undocumented | Open — found by this audit |

## Environment requirements

| Requirement | Value | Verified |
|---|---|---|
| JDK for backend | 21 | `mvn verify` succeeded |
| JDK for Android build | **25** (daemon-JVM pinned) | Gradle build succeeded |
| Maven | 3.9.9 | `mvn verify` succeeded |
| Node.js | 24.11.0 | `npm run build` succeeded |
| Docker Engine | 29.6.2 | Compose stack ran |
| Android SDK | API 37 | APK produced |

### Android toolchain caveat

Auto-provisioning of JDK 25 is blocked on this machine: the foojay-resolved
archive is fully downloaded but its lock is held by a long-running IDE
language-server process, so Gradle reports "Timeout waiting to lock". The
workaround is to set `JAVA_HOME` to an extracted copy:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-25\jdk-25.0.3+9"
```

Passing `org.gradle.java.installations.paths` does **not** work — daemon-JVM
criteria are resolved before project properties are applied. Documented in
[`../testing.md`](../testing.md).

## Baseline integrity statement

This baseline was captured after the two source defects were fixed and after the
admin-web healthcheck fix. It is the reference point for
[`FINAL_TEST_EVIDENCE.md`](FINAL_TEST_EVIDENCE.md) and
[`FINAL_PROJECT_STATUS.md`](FINAL_PROJECT_STATUS.md). Any later change that
breaks a number in this table invalidates the baseline.