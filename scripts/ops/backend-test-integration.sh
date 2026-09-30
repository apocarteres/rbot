#!/usr/bin/env bash
set -euo pipefail

# MVP-01

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/ops/docker-host.sh"
cd "$ROOT_DIR"
rbot_testcontainers_env
exec ./mvnw -B -ntp -Pintegration-tests test
