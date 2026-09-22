#!/usr/bin/env bash
# Starts everything needed for local development and runs the API in the foreground (Ctrl+C stops the API):
#   Docker (Colima) -> .env -> PostgreSQL + MinIO -> build -> database migrations -> API
# Stop the infrastructure afterwards with:  docker compose down
set -euo pipefail
cd "$(dirname "$0")/.."

# 1. Docker: start Colima if Docker is not reachable
if ! docker info >/dev/null 2>&1; then
  if command -v colima >/dev/null 2>&1; then
    echo "==> Starting Colima"
    colima start --cpu 4 --memory 4 --disk 40
  else
    echo "Docker is not running and Colima is not installed. See the README." >&2
    exit 1
  fi
fi

# 2. Local settings (creates .env with random secrets the first time; never changes existing values)
./scripts/setup-local.sh >/dev/null

# 3. PostgreSQL + MinIO
echo "==> Starting PostgreSQL and MinIO"
docker compose up -d --wait

# 4. Build the jars
echo "==> Building"
./mvnw -q -pl api,db-migration -am package -DskipTests

# 5. Load .env into this shell, apply migrations, run the API
set -a
# shellcheck disable=SC1091
source .env
set +a
echo "==> Applying database migrations"
java -jar db-migration/target/pavitra-db-migration-*-exec.jar
echo "==> Starting the API on http://localhost:8080  (Swagger UI: /swagger-ui.html)"
exec java -jar api/target/pavitra-api-*.jar
