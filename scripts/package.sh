#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
rm -f ResolveIT_Submission.zip
zip -q -r ResolveIT_Submission.zip backend frontend docker-compose.yml .env.example .gitignore README.md start-resolveit.bat scripts docs \
  -x 'backend/target/*' 'frontend/node_modules/*' 'frontend/dist/*' 'frontend/.pnpm-store/*' \
     'backend/data/*' '*.db' '*.log' '*/.env'
printf '%s\n' 'Created ResolveIT_Submission.zip (source and documentation).'
