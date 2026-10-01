#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-015

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/build-runner/lib.sh"

TASK=""
COMMIT=""
WORKTREE=0
while [ "$#" -gt 0 ]; do
  case "$1" in
    --commit) COMMIT="${2:?ключу --commit нужно значение}"; shift 2 ;;
    --worktree) WORKTREE=1; shift ;;
    -*) printf 'Неизвестный ключ: %s\n' "$1" >&2; exit 2 ;;
    *)
      [ -z "$TASK" ] || { printf 'Задача уже названа: %s\n' "$TASK" >&2; exit 2; }
      TASK="$1"
      shift
      ;;
  esac
done
[ -n "$TASK" ] || { printf 'Назовите задачу: mise run build-runner-run -- check [--worktree | --commit REF]\n' >&2; exit 2; }

log() {
  printf '[build-runner] %s\n' "$*"
}

rbot_runner_require
if [ "$WORKTREE" -eq 1 ]; then
  [ "$TASK" != "verify" ] || { printf 'verify пишет расписку на коммит: снимок рабочего дерева для него не годится\n' >&2; exit 2; }
  COMMIT="$(rbot_runner_snapshot "$ROOT_DIR")"
  log "снимок рабочего дерева: $COMMIT"
else
  COMMIT="$(git -C "$ROOT_DIR" rev-parse "${COMMIT:-HEAD}^{commit}")"
  git -C "$ROOT_DIR" update-ref refs/build-runner/worktree "$COMMIT"
  if [ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]; then
    log "внимание: незакоммиченные изменения в прогон не вошли"
  fi
fi
log "демон: $(docker context show), коммит $COMMIT, задача $TASK"

rbot_runner_load_source "$ROOT_DIR" "$COMMIT"
rbot_runner_exec "
  set -e
  mise trust --yes > /dev/null && mise install -y
  mise run platform-ensure
  mise run frontend-install
  mise run $TASK
"

if [ "$TASK" = "verify" ]; then
  mkdir -p "$ROOT_DIR/target/verify"
  docker cp "$(rbot_runner_container):/work/src/target/verify/$COMMIT.json" "$ROOT_DIR/target/verify/$COMMIT.json"
  log "расписка перенесена: target/verify/$COMMIT.json"
fi
log "задача $TASK пройдена на коммите $COMMIT"
