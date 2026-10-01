# Configuration baseline

What is under version control, what is not, and how configuration is managed.
This audit found that the project is **not** under version control, so this
document records actual state rather than expected practice.

## 1. Source control — FAILING

| Check | Result | Evidence |
|---|---|---|
| `.git` directory exists | **No** | `Test-Path .git` → `False` |
| Repository initialised | **No** | `git rev-parse --is-inside-work-tree` → `fatal: not a git repository` |
| Git installed | Yes | 2.55.0 at `C:\Program Files\Git\cmd\git.exe` |
| Git on `PATH` | **No** | `where git` → not found; full path required |
| Branches | **None** | — |
| Tags | **None** | — |
| Remotes | **None** | — |
| Commit history | **None** | — |
| `.gitignore` prepared | Yes | 594 bytes, 30 patterns, correctly excludes `.env`, `*.key`, `*.pem`, `secrets/`, `target/`, `node_modules/`, `*.apk`, `android/local.properties`, `logs/` |
| Editor/IDE metadata ignored | Yes | `.idea/`, `*.iml`, `.vscode/`, `.settings/` excluded |

**Interpretation:** the ignore rules show version control was intended, but the
repository was never initialised. The result is that every artifact in this
project — 385 automated checks' worth of code, 13 documentation files and 9
migrations — exists with **no change history, no authorship and no provenance**.

This is recorded as risk R-15 and limitation L-07. It is deliberately **not**
remediated by running `git init`, because a single initial commit created after
the fact would misrepresent the project's history rather than record it.

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

| Artifact | Location | Produced by |
|---|---|---|
| Backend jar | Built in container, not distributed | `mvn clean package` |
| Console bundle | `admin-web/dist/` | `npm run build` |
| Debug APK | `android/app/build/outputs/apk/debug/app-debug.apk` | `gradlew assembleDebug` |
| Test evidence | `tests/results/*.csv` | PowerShell suites |
| Test reports | `backend/target/{surefire,failsafe}-reports/` | Maven |
| Docker images | Local Docker daemon | `docker compose build` |

There is **no release packaging**: no versioned jar, no signed APK, no release
image tag, no release notes. This is consistent with a project that has not been
released, only verified.

## 7. Reproducibility summary

| Element | Reproducible? | Notes |
|---|---|---|
| Database schema | **Yes** | Migrations + seed, verified on a fresh volume |
| Backend image | **Yes** | Verified by clean rebuild |
| Console image | **Yes** | Build arg is explicit |
| Runtime configuration | **Yes** | 17 documented variables |
| Android build | **Conditional** | Requires JDK 25 at a specific extracted path; see caveat below |
| Full stack startup | **Yes** | Single command, verified from clean |

### Android toolchain caveat

`android/gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`.
Auto-provisioning fails on this machine because an IDE language-server process
holds the download lock for the already-downloaded JDK archive. The workaround:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-25\jdk-25.0.3+9"
cd android; ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

This is a documented manual step, so Android reproducibility is marked
**conditional** rather than **yes**. Recorded in
[`REPRODUCIBILITY.md`](../REPRODUCIBILITY.md).

## 8. Configuration findings

| ID | Finding | Severity | Status |
|---|---|---|---|
| CF-01 | No version control repository | HIGH | Open — R-15, L-07 |
| CF-02 | `.env.example` and `.env` key sets differ | LOW | Open |
| CF-03 | Android version `1.0` diverges from `0.1.0` elsewhere | LOW | Open, cosmetic |
| CF-04 | No image scanning or signing | MEDIUM | Accepted for scope |
| CF-05 | Git installed but not on `PATH` | LOW | Environment, not project |
| CF-06 | Android toolchain needs a manual `JAVA_HOME` step | MEDIUM | Documented |