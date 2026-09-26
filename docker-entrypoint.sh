#!/bin/sh
# Runs on every container start, on every Render tier - not just paid tiers with the Pre-Deploy Command
# feature. Flyway migrations are idempotent (a no-op against an already-migrated database via the
# flyway_schema_history table), so re-running on every restart is safe, not just tolerated.
#
# set -e: a failed migration (non-zero exit from db-migration.jar) stops this script here, so a bad
# migration prevents the API from ever starting against a half-migrated schema - see DbMigrationApplication's
# own class comment ("a deployment can stop before it starts").
set -e

echo "Running database migrations..."
java -jar db-migration.jar

echo "Migrations complete - starting API..."
exec java -jar api.jar
