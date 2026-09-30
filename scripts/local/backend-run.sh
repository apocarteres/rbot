#!/usr/bin/env bash
set -euo pipefail

# MVP-01

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/ops/docker-host.sh"
cd "$ROOT_DIR"
DOCKER_HOST="$(rbot_docker_host)"
export DOCKER_HOST
docker compose -f docker-compose.dev.yml up -d --wait
env_file="deploy/local.env"
[ -f "$env_file" ] || cp deploy/local.env.example "$env_file"
set -a
. "$env_file"
set +a
./mvnw -B -ntp -q -DskipTests package
java -jar target/rbot-1.0.0.jar migrate
SPRING_PROFILES_ACTIVE=local exec java -jar target/rbot-1.0.0.jar
