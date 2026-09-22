#!/usr/bin/env bash
# Creates .env for local development with randomly generated secrets.
# Safe to re-run: values that are already set are never changed, only blanks are filled.
set -euo pipefail
cd "$(dirname "$0")/.."

[ -f .env ] || cp .env.example .env
chmod 600 .env

# Sets KEY=value only if KEY is missing or blank in .env.
fill() {
  local key=$1 value=$2
  if grep -Eq "^${key}=.+$" .env; then
    return
  fi
  if grep -Eq "^${key}=" .env; then
    awk -v k="$key" -v v="$value" 'BEGIN { FS = OFS = "=" } $1 == k && $2 == "" { print k "=" v; next } { print }' \
      .env > .env.tmp && chmod 600 .env.tmp && mv .env.tmp .env
  else
    printf '%s=%s\n' "$key" "$value" >> .env
  fi
}

fill DB_PASSWORD "$(openssl rand -hex 16)"
fill MEDIA_ACCESS_KEY "$(openssl rand -hex 8)"
fill MEDIA_SECRET_KEY "$(openssl rand -hex 16)"
fill JWT_SECRET "$(openssl rand -hex 32)"

echo "Local settings are in .env (git-ignored). Existing values were kept."
if ! docker info >/dev/null 2>&1; then
  echo "Note: Docker is not reachable. On macOS start it with: colima start --cpu 4 --memory 4 --disk 40"
fi
echo "Next: docker compose up -d --wait"
