# Deployment

Local Docker Compose is the supported deployment. A production deployment is
described at the end, with the parts that need more work called out explicitly.

## Local with Docker Compose

```bash
cp .env.example .env
# edit .env: JWT_SECRET (>= 32 bytes) and DB_PASSWORD are mandatory
docker compose up -d --build
docker compose ps
```

`docker compose up` fails fast with a clear message if `DB_PASSWORD` or
`JWT_SECRET` is missing, rather than starting with a default password.

| Service | Container | Host port |
|---|---|---|
| PostgreSQL 16 | `finai-postgres` | 5433 |
| Backend | `finai-backend` | 8081 |
| Admin console | `finai-admin-web` | 5173 |

Startup order is enforced: the backend waits on the PostgreSQL health check
(`pg_isready`), so it never races the database.

Host ports are offset from the defaults (5433 / 8081 / 5173) so the stack runs
beside an existing local PostgreSQL. Override them in `.env` if needed.

Useful commands:

```bash
docker compose logs -f backend
docker compose restart backend
docker compose down          # keeps data
docker compose down -v       # deletes the database volume
```

## Local backend on the host

Faster than a container rebuild when iterating on Java:

```bash
docker compose up -d postgres
./scripts/run-backend-local.ps1
```

Output is teed to `logs/backend.log` rather than discarded, so a startup failure
is readable after the fact.

## Configuration

All configuration comes from environment variables; see `.env.example`. Secrets
are never committed — `.env` is gitignored.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `DB_PASSWORD` | yes | – | Compose refuses to start without it |
| `JWT_SECRET` | yes | – | Must be >= 32 bytes; startup fails otherwise |
| `DB_NAME` / `DB_USER` | no | `finai` | |
| `DB_HOST` / `DB_PORT` | no | `localhost` / `5433` | Host-run only; Compose overrides |
| `JWT_EXPIRATION` | no | `12h` | |
| `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD` | no | – | No admin is created when unset |
| `AI_PROVIDER` | no | `LOCAL` | Non-`LOCAL` requires an API key |
| `AI_PROVIDER_API_KEY` / `_BASE_URL` / `_MODEL` | no | – | |
| `BUDGET_WARNING_THRESHOLD` | no | `80` | |
| `BUDGET_EXCEEDED_THRESHOLD` | no | `100` | |
| `CORS_ALLOWED_ORIGINS` | no | localhost dev ports | Must include the console origin |
| `LOG_LEVEL` | no | `INFO` | |

### Bootstrap administrator

The admin account is created from `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD` on
first start. No password hash is stored in any migration file.

If the variables are unset the application still starts — it simply has no
administrator, and this is logged rather than hidden behind a default password.
An existing admin is never overwritten on restart.

### Health checks

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP","groups":["liveness","readiness"]}
```

Only `health`, `info` and `metrics` are exposed. There is no public endpoint that
discloses configuration or environment.

## Database

Migrations run automatically on startup via Flyway. Validate before deploying:

```bash
cd backend && mvn verify   # runs MigrationsAndBootstrapIT
```

All 9 migrations apply cleanly to an empty PostgreSQL 16 database, producing 11
tables. `ddl-auto` is `validate`, not `update` — the schema only ever changes
through a migration file, so the model and the database cannot silently drift
apart.

Back up with `pg_dump`. Restore with `psql`; Flyway will pick up from the
recorded version in `flyway_schema_history`.

## Building the images individually

```bash
docker compose build backend
docker compose build admin-web

# Android
cd android && ./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

The console image is a multi-stage build: Node compiles the bundle, then only the
static output is copied into an Nginx image.

## Production checklist

What would need to change for a real deployment, and what is honestly missing:

1. **Secrets.** Move `DB_PASSWORD` and `JWT_SECRET` into a secret manager. `.env`
   is fine for local work and inadequate for production.
2. **TLS.** Terminate TLS in front of the backend. The service speaks plain HTTP
   and has no certificate handling of its own.
3. **CORS.** Replace the localhost defaults with the real console origin.
4. **Database.** Managed PostgreSQL with automated backups and point-in-time
   recovery. The named volume in Compose has no backup story at all.
5. **Container platform.** A Compose file is not an orchestrator. There is no
   Kubernetes manifest, no replica count, no rolling update, and no resource
   limits.
6. **Observability.** Logs go to stdout with no aggregation, alerting or trace
   collection. Actuator `metrics` is available but nothing scrapes it.
7. **Logging redaction.** Logback has not been reviewed for token and password
   leakage in this configuration. `application.yml` sets the log level; the appender
   pattern comes from the Spring Boot default.

Items 5–7 are gaps, not design decisions. They are listed again in
[`known-limitations.md`](known-limitations.md).

## Maintenance commands

```bash
# Full local verification
mvn -f backend/pom.xml verify
powershell -ExecutionPolicy Bypass -File tests/e2e-api-tests.ps1
powershell -ExecutionPolicy Bypass -File tests/admin-web-contract.ps1
npm --prefix admin-web run build
cd android; ./gradlew testDebugUnitTest assembleDebug
```

Results land in `tests/results/*.csv`. See [`testing.md`](testing.md).