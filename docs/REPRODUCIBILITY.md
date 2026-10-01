# Reproducibility

Can another developer reproduce this project from documented instructions?
This document states the honest answer, including the steps that are still
manual.

**Verdict: reproducible for the Docker stack; conditional for Android.**
The one manual step is documented below rather than hidden.

---

## Prerequisites

| Requirement | Version | How to check |
|---|---|---|
| Docker Engine | 29.6.2+ with Compose v2 | `docker version && docker compose version` |
| Node.js | 24.11.0 (20+ likely fine) | `node --version` |
| JDK | 21 for the backend | `java -version` |
| JDK | **25 for the Android build** | `java -version` — see caveat |
| Maven | 3.9.9 (or use `scripts/mvnw.ps1`) | `mvn --version` |
| Android SDK | API 37 | Set in `android/local.properties` |
| Git | Optional — see the warning below | `git --version` |

> **Warning.** This project is **not** a Git repository, so there is nothing to
> clone. A new developer must be given the source tree by other means. See
> [`project-management/CONFIGURATION_BASELINE.md`](project-management/CONFIGURATION_BASELINE.md).

## Environment variables

```bash
cp .env.example .env
```

Then edit `.env`. **Two values are mandatory** — Compose refuses to start
without them:

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | HMAC signing key, minimum 32 bytes |
| `DB_PASSWORD` | Database password |

Optional but recommended for the demo:

| Variable | Purpose |
|---|---|
| `APP_ADMIN_EMAIL` | Bootstrap administrator login |
| `APP_ADMIN_PASSWORD` | Bootstrap administrator password |
| `AI_PROVIDER` | Leave as `LOCAL` to avoid external calls |
| `CORS_ALLOWED_ORIGINS` | Add the console origin if not on port 5173 |

`AI_PROVIDER_BASE_URL` and `AI_PROVIDER_MODEL` exist only in `.env.example`;
they are unused while `AI_PROVIDER=LOCAL`.

## Startup commands

```bash
# 1. Start everything
docker compose up -d --build

# 2. Confirm health (expect 3 healthy)
docker compose ps

# 3. Confirm the API
curl http://localhost:8081/actuator/health
```

Expected results: `finai-postgres`, `finai-backend` and `finai-admin-web` all
report **healthy**. The API is on 8081, the console on 5173.

Startup order is automatic: the backend waits for the PostgreSQL health check
before starting, so it never races the database.

## Database initialisation

**No manual step.** On first start the backend runs all 9 Flyway migrations
(`V1__create_users` … `V9__seed_reference_data`) and creates the bootstrap
administrator. Verify:

```bash
docker compose exec -T postgres psql -U finai -d finai \
  -c "select count(*) from flyway_schema_history where success;"   # expect 9
docker compose exec -T postgres psql -U finai -d finai \
  -c "select count(*) from information_schema.tables where table_schema='public';"  # expect 11
```

To start from a genuinely empty database:

```bash
docker compose down -v      # -v also deletes the data volume
docker compose up -d
```

## Admin configuration

The administrator is created from `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` on
first start. If unset, the app still runs with no administrator and logs that
fact. No password hash is stored in any migration.

Console access: <http://localhost:5173> → sign in with those credentials.

## Android configuration

```bash
# The emulator's view of the host machine
./gradlew assembleDebug
```

The API base URL defaults to `http://10.0.2.2:8081`, which is how the Android
emulator reaches the host. Override it for a physical device on the same LAN:

```bash
./gradlew assembleDebug -PapiBaseUrl=http://192.168.1.50:8081
```

Install with:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### The one manual step

`android/gradle/gradle-daemon-jvm.properties` pins `toolchainVersion=25`, so
Gradle requires a JDK 25 daemon regardless of `JAVA_HOME`. Automatic
provisioning **fails on the machine used for this project**, because an IDE
language-server process holds the download lock for the already-fetched JDK
archive:

```
Timeout waiting to lock OpenJDK25U-jdk_x64_windows_hotspot_25-any-vendor-25.0.3_9.zip
```

Workaround, verified working:

```powershell
# Extract once if not already present
Expand-Archive "$env:USERPROFILE\.gradle\jdks\OpenJDK25U-jdk_x64_windows_hotspot_25-any-vendor-25.0.3_9.zip" `
              -DestinationPath "$env:USERPROFILE\tools\jdk-25"

# Then build with that JDK
$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-25\jdk-25.0.3+9"
cd android
.\gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

Two notes that cost time when this was first attempted:

- Passing `-Porg.gradle.java.installations.paths=...` does **not** work.
  Daemon-JVM criteria are resolved before project properties are applied.
- Use `cleanTestDebugUnitTest` when you need the tests to actually re-execute.
  A bare `testDebugUnitTest` may report UP-TO-DATE.
- Do not run two Gradle invocations concurrently in the same project. They
  contend for the daemon and produce a misleading `BUILD FAILED`.

## Test commands

Run in this order. Each is independent; 1 and 2 do not need the stack up.

```bash
# 1. Backend unit tests (98) - needs only a JDK
cd backend && mvn test

# 2. Unit + integration (113) - needs Docker for Testcontainers
mvn verify

# 3. End-to-end API (160 assertions) - needs the stack running
powershell -ExecutionPolicy Bypass -File tests/e2e-api-tests.ps1

# 4. Admin console contract (102 assertions) - needs the stack running
powershell -ExecutionPolicy Bypass -File tests/admin-web-contract.ps1

# 5. Admin console build + typecheck
npm --prefix admin-web run build
npm --prefix admin-web run audit

# 6. Android (10 tests) - needs the JDK 25 step above
cd android && ./gradlew cleanTestDebugUnitTest testDebugUnitTest assembleDebug
```

Both PowerShell suites are **idempotent** — they create uniquely identified test
data per run and write a timestamped CSV to `tests/results/`. They can be re-run
freely, and both have been run repeatedly against both host and containerised
backends.

## Environment caveats

| Caveat | Impact | Workaround |
|---|---|---|
| Gradle daemon JVM pinned to 25 | Android build fails without JDK 25 | Set `JAVA_HOME` as shown above |
| JDK 25 auto-provisioning lock | Provisioning times out | Use the extracted archive |
| Git not on `PATH` | `git` command not found | Use `C:\Program Files\Git\cmd\git.exe` |
| No Git repository | Cannot clone the project | Source must be transferred directly |
| Java 25 required for Android only | Backend must still use Java 21 | Two separate JDKs |
| Android emulator needed for UI testing | No instrumented tests can run | Emulator or physical device |
| Test YAML password must be quoted | An unquoted `Admin#12345` truncates at `#` | Already quoted in `application-test.yml` |

## Shutdown commands

```bash
docker compose down          # stop containers, keep data
docker compose down -v       # stop and delete the database volume
docker compose stop backend  # stop one service
docker compose restart backend
```

## Reproducibility verdict

| Target | Reproducible? | Notes |
|---|---|---|
| Database schema | **Yes** | Verified on a fresh volume: 9 migrations, 11 tables |
| Backend image | **Yes** | Verified by clean rebuild |
| Console image | **Yes** | Build arg is explicit and documented |
| Full stack startup | **Yes** | Single command, verified from clean state |
| Test suites | **Yes** | All scripted and idempotent |
| Android build | **Conditional** | Requires the manual JDK 25 step |
| Source acquisition | **No** | No repository exists to clone from |
| Production deployment | **Not documented** | No production topology is defined |

The two honest weaknesses are the missing repository (a new developer cannot
obtain the source by cloning) and the Android JDK step. Both are recorded as
findings rather than papered over.