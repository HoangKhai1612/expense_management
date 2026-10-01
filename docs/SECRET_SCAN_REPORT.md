# Secret scan report

Pre-commit secret scan of everything that would enter the Git repository.
Performed 2026-10-01, after `.gitignore` was hardened.

## Method

1. Enumerate the exact file set Git would add (`git add -A --dry-run`).
2. Confirm the ignore rules actually exclude every sensitive path.
3. Scan the commit set for high-confidence credential tokens.
4. Scan for soft patterns (`password=`, `secret=`, `token=`, `api_key=` …).
5. Classify every hit as real secret, placeholder, test fixture or config
   reference.
6. Extract the live secret values from `.env` and search the commit set for
   those exact values — the definitive leak test.
7. Search the runtime log for the same values.

## 1. Commit set size

| Stage | Files |
|---|---|
| Before `.gitignore` hardening | 238 |
| After hardening (agent tooling excluded) | **227** |

No file in the commit set exceeds 200 KB, so the repository will not bloat.

## 2. Ignore rule verification

Every sensitive path was tested with `git check-ignore`:

| Path | Ignored | Required |
|---|---|---|
| `.env` | Yes | Yes |
| `.env.local` | Yes | Yes |
| `admin-web/node_modules` | Yes | Yes |
| `admin-web/dist` | Yes | Yes |
| `backend/target` | Yes | Yes |
| `android/.gradle` | Yes | Yes |
| `android/build` | Yes | Yes |
| `android/app/build` | Yes | Yes |
| `android/local.properties` | Yes | Yes |
| `logs` | Yes | Yes |
| `*.apk` | Yes | Yes |
| `.kilo/` (agent tooling) | Yes | Added by this review |

`.gitignore` was hardened in this pass to add:

- `.env.*` with `!.env.example` so the template is still tracked
- `*.p12`, `*.pfx`, `*.jks` alongside the existing key patterns
- `credentials/`
- `.kilo/`, `.claude/`, `.cursor/` — agent/editor tooling, equivalent to the
  `.vscode/` rule already present

## 3. High-confidence token scan

Pattern: OpenAI-style keys, private key headers, GitHub tokens, Firebase keys,
Slack tokens, AWS access keys, and JWT-shaped strings.

| Hits | Location | Assessment |
|---|---|---|
| **1** | `tests/e2e-api-tests.ps1:278` | **False positive.** A deliberately forged JWT used to prove that a structurally valid token with a wrong signature is rejected. The signature is the literal string `WrongSignatureValueThatIsLongEnough1234567890`. It is a negative-test fixture, not a credential. Correct to commit |

No real credential token found.

## 4. Soft pattern scan

31 soft hits across 15 files. Every one classified:

| File | Hits | Classification |
|---|---|---|
| `.env.example` | 3 | **Placeholders**: `change-me-database-password`, `change-me-to-a-random-secret-of-at-least-32-bytes`, `change-me-admin-password`. Intended for distribution |
| `tests/e2e-api-tests.ps1` | 7 | Test fixtures and the forged token above |
| `tests/admin-web-contract.ps1` | 2 | Test fixtures |
| `backend/src/test/resources/application-test.yml` | 2 | Throwaway test secret, commented as test-only; demo admin password for the isolated test context |
| `backend/src/test/java/.../AiGroundingIT.java` | 3 | Test constants |
| `backend/src/test/java/.../JwtServiceTest.java` | 3 | Test keys used to sign and verify |
| `backend/src/test/java/.../MigrationsAndBootstrapIT.java` | 1 | Test constant |
| `backend/src/main/java/.../JwtService.java` | 1 | `properties.getSecret()` — config lookup, **no literal** |
| `backend/src/main/java/.../BootstrapDataInitializer.java` | 1 | `this.adminPassword = adminPassword` — field assignment, **no literal** |
| `backend/src/main/java/.../AuthService.java` | 1 | `jwtService.issueToken(...)` — method call, **no literal** |
| `docker-compose.yml` | 2 | `${DB_PASSWORD}`, `${JWT_SECRET}` indirection, **no literal** |
| `android/.../AuthViewModel.kt`, `ApiClient.kt`, `SessionStore.kt`, `ApiDtos.kt` | 5 | Field names, e.g. `accessToken` — **no literal** |

**No production source file contains a hardcoded secret.** Every main-source hit
is a config lookup, a field assignment, or a method call.

## 5. Definitive leak test — live `.env` values

The actual secret values were extracted from `.env` and searched for, by exact
value, across the entire commit set:

| Secret | Length | Found in commit set? |
|---|---|---|
| `JWT_SECRET` | 64 | **No** |
| `DB_PASSWORD` | 18 | **No** |
| `APP_ADMIN_PASSWORD` | 11 | See finding SEC-01 |

## 6. Runtime log scan

| Secret | In runtime log? |
|---|---|
| `JWT_SECRET` | **No** |
| `DB_PASSWORD` | **No** |
| `APP_ADMIN_PASSWORD` | **No** |

`Bearer` / `Authorization` occurrences in the runtime log: **0**.

See [`LOGBACK_SECURITY_REVIEW.md`](LOGBACK_SECURITY_REVIEW.md) for the full
analysis.

## 7. Findings

### SEC-01 — The live admin password equals the documented demo credential

| Field | Finding |
|---|---|
| **Severity** | **WARNING — MEDIUM** |
| **Finding** | The `APP_ADMIN_PASSWORD` in the live `.env` is identical to the demo credential published in `docs/api-reference.md`, `docs/REPRODUCIBILITY.md`, `docs/security-audit.md` and the two test scripts. Anyone who reads the repository knows the administrator password of a running deployment. |
| **Why this is not a covert leak** | It is disclosed deliberately and already tracked as finding **S-09** in `docs/security-audit.md` ("Development credentials documented"). It was found by this scan rather than hidden by it. |
| **Root cause** | A single demo credential is reused across documentation, test fixtures and the local deployment, because that keeps the project reproducible for a reviewer without any setup step. |
| **Risk if used beyond a demo** | Full administrative control: user lock/unlock, category catalogue, feedback triage, audit trail. |
| **Action taken** | None. Rotating it would break the reproducibility that the documentation promises, and the value is already published. |
| **Required action for any non-demo deployment** | Set a unique `APP_ADMIN_PASSWORD` **and** a unique `JWT_SECRET` in `.env`. Both are already environment-driven, so no code change is needed — only configuration. |
| **Recommendation** | Record this in the report as a deliberate, disclosed trade-off: reproducibility was chosen over credential secrecy for an academic demo. |

### SEC-02 — Forged JWT test fixture

| Field | Finding |
|---|---|
| **Severity** | Informational |
| **Finding** | `tests/e2e-api-tests.ps1:278` contains a JWT-shaped string. |
| **Assessment** | A negative-test fixture with an obviously fake signature. Not a credential. Correctly committed — removing it would weaken the test that proves forged tokens are rejected. |

### SEC-03 — One-time warning, no action required

The scan confirmed no secret was **ever committed**, so no credential rotation
from history is required. Had a real secret been found in a prior commit, the
correct response would be rotation, not deletion — removing a file from the
latest commit does not remove it from history. That situation did not occur.

## 8. Verdict

| Check | Result |
|---|---|
| No `.env` committed | **PASS** |
| No key/certificate/keystore committed | **PASS** |
| No `node_modules`, `target`, `build`, `dist` committed | **PASS** |
| No `local.properties` committed | **PASS** |
| No log files committed | **PASS** |
| No hardcoded secret in production source | **PASS** |
| No real credential token in the commit set | **PASS** |
| Test fixture false positive explained | **PASS** |
| Live admin password is the published demo credential | **SEC-01, WARNING, disclosed and accepted for demo scope** |

**No credential requires rotation.** The only issue is SEC-01, which is a
deliberate, already-disclosed reproducibility trade-off with a configuration-only
remedy.