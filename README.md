# Pavitra Ristaa backend

Java 21, Spring Boot 4, PostgreSQL. A modular monolith built as a Maven multi-module project.

| Module | Kind | What it is |
|---|---|---|
| `shared` | library | API envelope, error types, security principal, utilities. No dependency on features. |
| `db-migration` | deployable | Flyway SQL and the job that applies it. The only thing that changes the schema. |
| `api` | deployable | The REST API. Features are packages under `com.pavitraristaa`. |

Source-of-truth docs are in [docs/](docs/); start with `docs/ai/PAVITRA_RISTAA_BACKEND_AI_CONTEXT_V1.0.md`.

## Build and test

```bash
./mvnw test
```

The persistence tests use Testcontainers and are skipped when Docker is not running (see the Colima setup below).

## Local infrastructure (PostgreSQL + MinIO)

`docker-compose.yml` starts PostgreSQL 18 and MinIO (S3-compatible storage) and creates the private `pavitra-media`
bucket. It is for local development only.

One-time Docker setup on macOS with [Colima](https://github.com/abiosoft/colima):

```bash
brew install colima docker-compose
colima start --cpu 4 --memory 4 --disk 40
```

Compose is a Docker plugin: add `"cliPluginsExtraDirs": ["/opt/homebrew/lib/docker/cli-plugins"]` to
`~/.docker/config.json`. Colima keeps its socket outside `/var/run`, so Testcontainers needs two variables
(put them in your shell profile):

```bash
export DOCKER_HOST="unix://$HOME/.colima/default/docker.sock"
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```

## Daily use: one command

```bash
./scripts/dev.sh
```

This starts Colima if it is stopped, creates `.env` on the first run, starts PostgreSQL and MinIO, builds, applies the
database migrations and runs the API in the foreground (Ctrl+C stops the API). When you are done:

```bash
docker compose down     # stop PostgreSQL and MinIO (data is kept)
colima stop             # optional: stop the VM to free its 4 GB of RAM
```

## Step by step (what `dev.sh` does)

```bash
./scripts/setup-local.sh      # creates .env with random secrets (safe to re-run; keeps existing values)
docker compose up -d --wait   # postgres on localhost:5433, MinIO on localhost:9000 (console :9001)
```

The `pavitra-media` bucket is created by MinIO's healthcheck, so `--wait` returns only once it exists.
PostgreSQL is published on **5433** so it does not clash with a PostgreSQL already running on 5432.
`docker compose down` stops everything and keeps the data; `docker compose down -v` deletes it.
Stop the VM when you are done with `colima stop`.

## Run locally

1. Build the jars:

   ```bash
   ./mvnw package -DskipTests
   ```

2. Load the settings from `.env` into your shell, then apply the migrations (safe to repeat):

   ```bash
   set -a; source .env; set +a
   java -jar db-migration/target/pavitra-db-migration-0.0.1-SNAPSHOT-exec.jar
   ```

3. Start the API:

   ```bash
   java -jar api/target/pavitra-api-0.0.1-SNAPSHOT.jar
   ```

Swagger UI is at `http://localhost:8080/swagger-ui.html`.

## Configuration (environment variables)

| Variable | Used by | Notes |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | both | Required when `SPRING_PROFILES_ACTIVE` is not `local` |
| `SPRING_PROFILES_ACTIVE` | both | `local` (default), `dev`, `prod` |
| `JWT_SECRET` | api | Required in `dev` and `prod`; `prod` refuses to start unless it is at least 32 characters |
| `MEDIA_BUCKET`, `MEDIA_ENDPOINT`, `MEDIA_ACCESS_KEY`, `MEDIA_SECRET_KEY`, `MEDIA_REGION`, `MEDIA_PATH_STYLE`, `MEDIA_PROVIDER` | api | S3 / MinIO. `local` defaults to MinIO at `localhost:9000`, bucket `pavitra-media` |
| `GOOGLE_CLIENT_ID`, `APPLE_CLIENT_ID` | api | Social login |

## Deployment order

1. Run the `db-migration` job. A non-zero exit code means it failed; stop there.
2. Start the new `api` version.
