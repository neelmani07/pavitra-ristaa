#!/usr/bin/env bash
# Broad smoke test against a running deployment: hits every module's core GET endpoints (public and
# authenticated) and confirms none of them 5xx or 404 on a route that should exist. This isn't a
# substitute for the real test suite (./mvnw test) - it's the fast "did the last deploy actually work"
# check you run against a live URL after a deploy.
#
# Usage:
#   ./scripts/smoke-test.sh                                    # BASE_URL defaults to local :8080
#   BASE_URL=https://pavitra-ristaa.onrender.com ./scripts/smoke-test.sh
#
# For the authenticated half of the run, also set DB_URL/DB_USERNAME/DB_PASSWORD (same names used
# everywhere else in this repo) pointing at the SAME database that BASE_URL's server uses - the script
# registers a throwaway test user over HTTP, flips its account_status to ACTIVE directly in Postgres
# (bypassing OTP, which isn't wired to a real provider yet - see
# docs/decisions/OTP_DELIVERY_PROVIDER.md) so it can log in, then PUTs a minimal profile so the
# profile/preview/completion endpoints have something to return instead of a legitimate 404 for a
# never-set-up user. The test user and every row it created (profile, login history, refresh token,
# OTP, role) are deleted again at the end, success or failure (trap below).
#
# NEVER point DB_URL at a production database - this creates a real row and runs raw DELETEs. Staging
# or local only.
#
# Note for anyone editing this script: don't use `path` or `status` as a bash variable name. Both are
# special parameters in zsh (`path` is tied to `$PATH` itself) - assigning to them silently breaks
# command lookup for the rest of the scope, which looks exactly like "command not found: curl" with no
# obvious cause. Use `route` / `http_status` instead, as below.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASS=0
FAIL=0
FAILED_ROUTES=()

check() {
  local method=$1 route=$2 expected=$3 auth_header=${4:-}
  local http_status
  if [ -n "$auth_header" ]; then
    http_status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" -H "$auth_header" "$BASE_URL$route")
  else
    http_status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$BASE_URL$route")
  fi
  if [[ ",$expected," == *",$http_status,"* ]]; then
    PASS=$((PASS + 1))
    printf "  \033[32mOK\033[0m   %-6s %-55s -> %s\n" "$method" "$route" "$http_status"
  else
    FAIL=$((FAIL + 1))
    FAILED_ROUTES+=("$method $route -> $http_status (expected $expected)")
    printf "  \033[31mFAIL\033[0m %-6s %-55s -> %s (expected %s)\n" "$method" "$route" "$http_status" "$expected"
  fi
}

echo "== Smoke testing $BASE_URL =="
echo
echo "-- Infra --"
check GET /actuator/health 200
check GET /v3/api-docs 200

echo
echo "-- Public endpoints (no auth) --"
check GET /api/v1/relationship-modes 200
check GET /api/v1/master-data/categories 200
check GET /api/v1/reports/reasons 200
check GET /api/v1/safety-center 200
check GET /api/v1/help 200
check GET /api/v1/plans 200

echo
echo "-- Auth boundary (should reject, not error) --"
check GET /api/v1/me/profile 401
check GET / 401

TEST_EMAIL="smoketest-$(date +%s)@example.com"
TEST_PASSWORD="SmokeTest123!"

if [ -z "${DB_URL:-}" ]; then
  echo
  echo "-- Skipping authenticated endpoints: set DB_URL/DB_USERNAME/DB_PASSWORD to also cover these --"
else
  DB_HOST=$(echo "$DB_URL" | sed -E 's#jdbc:postgresql://([^/]+)/.*#\1#')
  DB_NAME=$(echo "$DB_URL" | sed -E 's#.*/([^?]+)\?.*#\1#')
  DB_CONN="host=$DB_HOST dbname=$DB_NAME user=$DB_USERNAME sslmode=prefer"

  cleanup() {
    PGPASSWORD="$DB_PASSWORD" psql "$DB_CONN" -v ON_ERROR_STOP=0 -q <<SQL >/dev/null 2>&1
BEGIN;
DELETE FROM login_history WHERE user_id = (SELECT id FROM "user" WHERE email = '$TEST_EMAIL');
DELETE FROM refresh_token WHERE user_id = (SELECT id FROM "user" WHERE email = '$TEST_EMAIL');
DELETE FROM otp WHERE user_id = (SELECT id FROM "user" WHERE email = '$TEST_EMAIL');
DELETE FROM user_role WHERE user_id = (SELECT id FROM "user" WHERE email = '$TEST_EMAIL');
DELETE FROM user_profile WHERE user_id = (SELECT id FROM "user" WHERE email = '$TEST_EMAIL');
DELETE FROM "user" WHERE email = '$TEST_EMAIL';
COMMIT;
SQL
  }
  trap cleanup EXIT

  echo
  echo "-- Registering throwaway test user ($TEST_EMAIL) --"
  register_status=$(curl -s -o /tmp/smoketest_register.json -w "%{http_code}" -X POST "$BASE_URL/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\",\"fullName\":\"Smoke Test\",\"phoneNumber\":\"+91$(date +%s | tail -c 10)\",\"gender\":\"MALE\",\"dateOfBirth\":\"1995-01-01\",\"acceptedTerms\":true,\"acceptedPrivacyPolicy\":true}")
  if [ "$register_status" != "201" ]; then
    echo "  Registration failed (status $register_status) - aborting authenticated checks."
    cat /tmp/smoketest_register.json
    FAIL=$((FAIL + 1))
  else
    PGPASSWORD="$DB_PASSWORD" psql "$DB_CONN" -q -c \
      "UPDATE \"user\" SET account_status = 'ACTIVE' WHERE email = '$TEST_EMAIL';" >/dev/null

    login_response=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
      -H "Content-Type: application/json" \
      -d "{\"identifier\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")
    JWT=$(echo "$login_response" | grep -oE '"accessToken"\s*:\s*"[^"]+"' | sed -E 's/.*"([^"]+)"$/\1/')

    if [ -z "$JWT" ]; then
      echo "  Login failed - aborting authenticated checks. Response: $login_response"
      FAIL=$((FAIL + 1))
    else
      AUTH="Authorization: Bearer $JWT"

      echo
      echo "-- Creating a minimal profile (several endpoints legitimately 404 without one) --"
      profile_put_status=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE_URL/api/v1/me/profile" \
        -H "$AUTH" -H "Content-Type: application/json" \
        -d '{"firstName":"Smoke","lastName":"Test","displayName":"SmokeTest","dateOfBirth":"1995-01-01","gender":"MALE","headline":"smoke test","aboutMe":"smoke test"}')
      if [ "$profile_put_status" = "200" ]; then
        PASS=$((PASS + 1))
        printf "  \033[32mOK\033[0m   %-6s %-55s -> %s\n" PUT /api/v1/me/profile "$profile_put_status"
      else
        FAIL=$((FAIL + 1))
        FAILED_ROUTES+=("PUT /api/v1/me/profile -> $profile_put_status (expected 200)")
        printf "  \033[31mFAIL\033[0m %-6s %-55s -> %s (expected 200)\n" PUT /api/v1/me/profile "$profile_put_status"
      fi

      echo
      echo "-- Authenticated endpoints (auth, profile, connections, messaging, subscriptions, etc.) --"
      check GET /api/v1/auth/me 200 "$AUTH"
      check GET /api/v1/auth/sessions 200 "$AUTH"
      check GET /api/v1/auth/linked-accounts 200 "$AUTH"
      check GET /api/v1/me/profile 200 "$AUTH"
      check GET /api/v1/me/profile/education 200 "$AUTH"
      check GET /api/v1/me/profile/career 200 "$AUTH"
      check GET /api/v1/me/profile/family 200 "$AUTH"
      check GET /api/v1/me/profile/lifestyle 200 "$AUTH"
      check GET /api/v1/me/profile/spiritual 200 "$AUTH"
      check GET /api/v1/me/profile/languages 200 "$AUTH"
      check GET /api/v1/me/profile/interests 200 "$AUTH"
      check GET /api/v1/me/profile/hobbies 200 "$AUTH"
      check GET /api/v1/me/profile/photos 200 "$AUTH"
      check GET /api/v1/me/profile/preview 200 "$AUTH"
      check GET /api/v1/me/profile/completion 200 "$AUTH"
      check GET /api/v1/me/relationship-modes 200 "$AUTH"
      check GET /api/v1/me/preferences 200 "$AUTH"
      check GET /api/v1/shortlist 200 "$AUTH"
      check GET /api/v1/favorites 200 "$AUTH"
      check GET /api/v1/home 200 "$AUTH"
      check GET /api/v1/discovery/profiles 200 "$AUTH"
      check GET /api/v1/discovery/recently-viewed 200 "$AUTH"
      check GET /api/v1/discovery/recommendations 200 "$AUTH"
      check GET /api/v1/discovery/saved-searches 200 "$AUTH"
      check GET /api/v1/discovery/search-history 200 "$AUTH"
      check GET /api/v1/discovery/collections 200 "$AUTH"
      check GET /api/v1/verification/status 200 "$AUTH"
      check GET /api/v1/blocks 200 "$AUTH"
      check GET /api/v1/conversations 200 "$AUTH"
      check GET /api/v1/notifications 200 "$AUTH"
      check GET /api/v1/notification-settings 200 "$AUTH"
      check GET /api/v1/interests/sent 200 "$AUTH"
      check GET /api/v1/interests/received 200 "$AUTH"
      check GET /api/v1/matches 200 "$AUTH"
      check GET /api/v1/matches/history 200 "$AUTH"
      check GET /api/v1/support/tickets 200 "$AUTH"
      check GET /api/v1/subscriptions/me 200,404 "$AUTH"
      check GET /api/v1/invoices 200 "$AUTH"
      check GET /api/v1/payments 200 "$AUTH"

      echo
      echo "-- Admin endpoints (non-admin user, should reject with 403, not error) --"
      check GET /api/v1/admin/users 403 "$AUTH"
      check GET /api/v1/admin/audit-logs 403 "$AUTH"
      check GET /api/v1/admin/moderation 403 "$AUTH"
      check GET /api/v1/admin/reports 403 "$AUTH"
      check GET /api/v1/admin/support/tickets 403 "$AUTH"
      check GET /api/v1/admin/verifications 403 "$AUTH"
      check GET /api/v1/admin/appeals 403 "$AUTH"
      check GET /api/v1/admin/settings 403 "$AUTH"
    fi
  fi
fi

echo
echo "== Results: $PASS passed, $FAIL failed =="
if [ "$FAIL" -gt 0 ]; then
  echo
  echo "Failed routes:"
  for r in "${FAILED_ROUTES[@]}"; do echo "  - $r"; done
  exit 1
fi
exit 0
