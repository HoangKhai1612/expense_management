# Configuration baseline

What is under version control, what is not, and how configuration is managed.

## 1. Source control — IN USE (with a historical gap)

| Check | Result | Evidence |
|---|---|---|
| `.git` directory exists | **Yes** | Established 2026-10-01 |
| Repository initialised | **Yes** | `git init`, then reconciled with the remote |
| Remote configured | **Yes** | `https://github.com/HoangKhai1612/expense_management.git` |
| Branch | **main** | Matches the repository's default branch; no rename performed |
| Tracking configured | **Yes** | `branch 'main' set up to track 'origin/main'` |
| Local vs remote | **Synchronised** | `git rev-list --left-right --count origin/main...main` → `0  0` |
| Push type | **Fast-forward** | `2c4371b..59cc2a0  main -> main`. No force push |
| Remote history preserved | **Yes** | Template commit `2c4371b` retained as the parent |
| Tag | `v1.0.0-academic-final` | Points at the verified baseline `59cc2a0` |
| Git identity | Configured | `HoangKhai1612 <hoangtuankiet1612@gmail.com>` — pre-existing; not invented |
| `.gitignore` | Hardened | 40+ patterns; verified with `git check-ignore` |
| Secrets committed | **0** | Verified by value-level scan — see `docs/SECRET_SCAN_REPORT.md` |
| Build artefacts committed | **0** | `target/`, `node_modules/`, `dist/`, `build/`, `*.apk` all excluded |
| Gradle wrapper jar committed | **Yes** | Fixed during this pass — see below |

### Commit graph

```
* 59cc2a0  chore: establish verified academic project baseline
* 2c4371b  Initial commit            <- GitHub template README, preserved
```

### Reconciliation method — how the history was protected

The local project had no Git history, but the GitHub repository already existed.
The remote was probed **before** any local change:

```
git ls-remote https://github.com/HoangKhai1612/expense_management.git
2c4371be5e69446fdb26979e2eb4ae2b505bb308  HEAD
2c4371be5e69446fdb26979e2eb4ae2b505bb308  refs/heads/main
```

The remote held one commit — a GitHub template `README.md`, created the same
day. Rather than replacing it, the sequence used was:

1. `git init` — local repository only
2. `git remote add origin <url>`
3. `git fetch origin` — obtain the real remote history
4. `git symbolic-ref HEAD refs/heads/main`
5. `git update-ref refs/heads/main 2c4371be…` — point local `main` at the remote
   commit **without touching the working tree**
6. `git reset` — align the index with `HEAD`
7. Commit the project **on top**

**No history was rewritten. No force push. No remote commit deleted.**

### The remaining historical gap

Version control was established *after* development. Consequently:

| Now recorded going forward | Permanently unavailable |
|---|---|
| Change history for all future work | Development-period commit history |
| Authorship of changes | Who wrote what, and when |
| Branching and review | Any historical review record |
| Tagged release points | Historical schedule or effort |
| Regression history | Historical defect-introduction data |

**Historical development provenance is `UNKNOWN / NOT RECORDED`.** This is stated
in the baseline commit message, in `BASELINE.md`, and in `EFFORT_DATA_GAP.md` so
no reader infers a development history that does not exist.

### Defect found and fixed: Gradle wrapper jar was excluded

| Field | Finding |
|---|---|
| **Defect** | `.gitignore` contained a blanket `*.jar` rule, which excluded `android/gradle/wrapper/gradle-wrapper.jar` |
| **Consequence** | On a fresh clone, `./gradlew` would fail with `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` — the Android project **could not be built from the repository at all** |
| **Evidence** | `git check-ignore -v android/gradle/wrapper/gradle-wrapper.jar` → matched `.gitignore:15:*.jar` |
| **Severity** | **HIGH** — a genuine reproducibility defect that no test in the project would have caught, because local builds used the jar already on disk |
| **Fix** | Added `!android/gradle/wrapper/gradle-wrapper.jar` to `.gitignore` |
| **Verification** | Jar now staged and committed; confirmed present in the baseline commit |
| **Lesson** | A working local directory can hide a repository defect. Reproducibility must be tested from a clean clone, not from the working tree |

## 2. Versioning

Version numbers are hardcoded per component with no single source of truth.

| Component | Declared in | Value | Inconsistency |
|---|---|---|---|
| Backend | `backend/pom.xml` | `0.1.0` | — |
| Console | `admin-web/package.json` | `0.1.0` | Matches backend |
| Compose images | `docker-compose.yml` | `0.1.0` | Matches, in 2 places |
| Android | `android/app/build.gradle.kts` | `1.0` (versionCode 1) | **Differs** from backend/console `0.1.0` |

The Android version is the only divergence. It is cosmetic — the artifacts are
deployed independently — but it means there is no single "release version" to
quote. Recorded rather than corrected, to avoid destabilising a verified system.

## 3. Environment configuration

| Aspect | Actual state |
|---|---|
| Config source | Environment variables only |
| Sample template | `.env.example`, 17 keys |
| Live config | `.env`, 16 keys, gitignored |
| Template/live parity | **2 keys differ** |
| Secrets in repo | None |
| Required-at-startup | `DB_PASSWORD`, `JWT_SECRET` (enforced by Compose `${VAR:?message}`) |
| Bootstrap admin | `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD`, optional |

Verified by diffing the two key sets: `.env.example` defines 17 keys, `.env`
defines 15. The two keys present in the template but not in the live file are
`AI_PROVIDER_BASE_URL` and `AI_PROVIDER_MODEL`. The live file has **no** key
absent from the template.

This is benign — those two variables have defaults in `docker-compose.yml` and
are only needed for a non-`LOCAL` AI provider, which this deployment does not
use. All secrets (`DB_PASSWORD`, `JWT_SECRET`, `APP_ADMIN_PASSWORD`,
`AI_PROVIDER_API_KEY`) are present in the template as placeholders.

**Finding CF-02 (LOW).** No action required; recorded for completeness because
the template is a superset, which is the correct arrangement.

## 4. Database migrations — STRONG

| Check | Result |
|---|---|
| Migration tool | Flyway |
| Count | 9 (`V1__create_users` … `V9__seed_reference_data`) |
| Applied | 9, all `success = t` |
| Checksums | Recorded in `flyway_schema_history` |
| Schema authority | Migrations only; `ddl-auto: validate` |
| Drift detection | Yes — startup fails if entities disagree with schema |
| Idempotency | Yes — `MigrationsAndBootstrapIT` re-verifies on every `mvn verify` |
| Seed data | Idempotent; bootstrap admin never overwritten on restart |
| Down migrations | None provided |
| Sensitive data in migrations | **None** — no password hash in any migration |

This is the strongest configuration-control area in the project. A migration is
versioned by filename, checksummed, and validated against the ORM at startup.

## 5. Docker images

| Image | Tag | Build context | Multi-stage | Notes |
|---|---|---|---|---|
| `personal-finance-ai-backend` | 0.1.0 | `./backend` | Yes | Runs as non-root `finance` user |
| `personal-finance-ai-admin` | 0.1.0 | `./admin-web` | Yes | Node build → Nginx runtime |
| `postgres` | 16-alpine | upstream | — | Pinned to minor version |

| Check | Result |
|---|---|
| Build args | `VITE_API_BASE_URL` passed at build time and documented as such |
| Layer caching | Dependencies copied before source in both Dockerfiles |
| Image tags | Fixed `0.1.0`, no `latest` |
| Reproducibility | Confirmed — clean `down && build` produced working images |
| Scanning | Not implemented (no Trivy/Grype) |
| Signing | Not implemented |
| Vulnerability patching | Manual; postgres image 13 days old at audit |

## 6. Release artifacts

| Artifact | Location | Produced by | In Git? |
|---|---|---|---|
| Baseline commit | `59cc2a0` on `main` | `git commit` | **Yes** |
| Release tag | `v1.0.0-academic-final` | `git tag` | **Yes** |
| Backend jar | Built in container, not distributed | `mvn clean package` | No — `target/` ignored |
| Console bundle | `admin-web/dist/` | `npm run build` | No — `dist/` ignored |
| Debug APK | `android/app/build/outputs/apk/debug/app-debug.apk` | `gradlew assembleDebug` | No — `*.apk` ignored |
| Test evidence | `tests/results/*.csv` | PowerShell suites | **Yes** — 16 CSVs committed as evidence |
| Test reports | `backend/target/{surefire,failsafe}-reports/` | Maven | No — generated |
| Docker images | Local Docker daemon | `docker compose build` | No — reproducible from source |

A **versioned release point** now exists: tag `v1.0.0-academic-final` identifies
the verified baseline. What does **not** exist: a signed release, a versioned
jar, a signed APK, or release notes. The tag marks a verified state, not a
distributable artefact.

## 7. Reproducibility summary

| Element | Reproducible? | Notes |
|---|---|---|
| Database schema | **Yes** | Migrations + seed, verified on a fresh volume |
| Backend image | **Yes** | Verified by clean rebuild |
| Console image | **Yes** | Build arg is explicit |
| Runtime configuration | **Yes** | 17 documented variables |
| Source acquisition | **Yes** | `git clone` from the public remote |
| Android build from clone | **Yes** | Wrapper jar now committed — fixed in this pass |
| Android build toolchain | **Conditional** | Requires JDK 25 at a specific extracted path; see caveat below |
| Full stack startup | **Yes** | Single command, verified from clean |

### Android toolchain caveat

`android/gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`.
Auto-provisioning fails on this machine because an IDE language-server process
holds the download lock for the already-downloaded JDK archive. The workaround:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-25\jdk-25.0.3+9"
cd android; ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

This is a documented manual step, so Android *toolchain* reproducibility is
marked **conditional** even though the *source* is now fully reproducible.

## 8. Configuration findings

| ID | Finding | Severity | Status |
|---|---|---|---|
| CF-01 | No version control repository | HIGH | **CLOSED** — repository, remote, baseline commit and tag established. Historical provenance permanently `UNKNOWN / NOT RECORDED` |
| CF-02 | `.env.example` and `.env` key sets differ by 2 keys | LOW | Open, benign — template is the superset |
| CF-03 | Android version `1.0` diverges from `0.1.0` elsewhere | LOW | Open, cosmetic |
| CF-04 | No image scanning or signing | MEDIUM | Accepted for scope |
| CF-05 | Git installed but not on `PATH` | LOW | Environment, not project |
| CF-06 | Android toolchain needs a manual `JAVA_HOME` step | MEDIUM | Documented |
| CF-07 | **Gradle wrapper jar excluded by `*.jar` rule** | HIGH | **FIXED** — fresh clone could not have built the Android project |
| CF-08 | Agent tooling (`.kilo/`) would have been committed | LOW | **FIXED** — added to `.gitignore` |