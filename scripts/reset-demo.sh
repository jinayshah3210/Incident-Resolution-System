#!/usr/bin/env sh
# Recreates the ResolveIT database (DESTROYS its data), then the app re-runs Flyway and the demo seeder on start.
set -eu
: "${DB_NAME:=resolveit}" "${DB_USER:=resolveit}"
printf 'This drops and recreates database %s. Continue? [y/N] ' "$DB_NAME"; read -r answer
[ "$answer" = "y" ] || exit 1
psql -h "${DB_HOST:-localhost}" -U postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" -c "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
echo "Database recreated. Start the backend to apply migrations and demo accounts."
