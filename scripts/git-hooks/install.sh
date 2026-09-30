#!/usr/bin/env bash
set -euo pipefail

# REQ-QUALITY-004, RBOT-QUAL-001

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HOOKS_DIR="$(cd "$ROOT_DIR" && git rev-parse --absolute-git-dir)/hooks"

mkdir -p "$HOOKS_DIR"
ln -sf "$ROOT_DIR/scripts/git-hooks/pre-push" "$HOOKS_DIR/pre-push"
printf '[git-hooks] pre-push установлен: %s\n' "$HOOKS_DIR/pre-push"
ln -sf "$ROOT_DIR/scripts/git-hooks/commit-msg" "$HOOKS_DIR/commit-msg"
printf '[git-hooks] commit-msg установлен: %s\n' "$HOOKS_DIR/commit-msg"
