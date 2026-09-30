#!/usr/bin/env bash
set -euo pipefail

# RBOT-QUAL-001, REQ-QUALITY-001, REQ-DEPLOYMENT-019

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"
failed=0

fail() {
  printf '[scripts-test] %s\n' "$*" >&2
  failed=1
}

while IFS= read -r script; do
  bash -n "$script" || fail "синтаксис: $script"
done < <(find scripts -type f -name '*.sh' | sort; printf '%s\n' scripts/git-hooks/pre-push scripts/git-hooks/commit-msg)

expect_refusal() {
  local name="$1"; shift
  if "$@" > /dev/null 2>&1; then
    fail "$name: ожидался отказ"
  fi
}

expect_refusal "deploy.sh без --env" scripts/deploy.sh
expect_refusal "deploy.sh с позиционной средой" scripts/deploy.sh production
expect_refusal "deploy.sh с неизвестной средой" scripts/deploy.sh --env prod
[ "$(scripts/platform/conventions.sh components --environments | paste -sd, -)" = "qa,production" ] || fail "среды: ожидались qa,production"

message="$(mktemp)"
trap 'rm -f "$message"' EXIT
printf 'RBOT-OPS-001 Проверка\n' > "$message"; scripts/git-hooks/commit-msg "$message" || fail "commit-msg отверг задачу проекта"
printf 'MVP-01 Проверка\n' > "$message"; scripts/git-hooks/commit-msg "$message" || fail "commit-msg отверг этап плана"
printf 'Проверка без задачи\n' > "$message"; expect_refusal "commit-msg без задачи" scripts/git-hooks/commit-msg "$message"

[ "$failed" -eq 0 ] && printf '[scripts-test] скрипты: синтаксис, вход развёртывания и commit-msg — без отказов\n'
exit "$failed"
