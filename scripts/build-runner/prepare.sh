#!/usr/bin/env bash
set -euo pipefail

# RBOT-OPS-015

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$ROOT_DIR/scripts/build-runner/lib.sh"

log() {
  printf '[build-runner] %s\n' "$*"
}

CONTAINER="$(rbot_runner_container)"
IMAGE="${RBOT_BUILD_RUNNER_IMAGE:-rbot-build-runner:1}"
RUNNER_UID="${RBOT_BUILD_RUNNER_UID:-1000}"
RUNNER_GID="${RBOT_BUILD_RUNNER_GID:-1000}"
VOLUMES=("${CONTAINER}-src" "${CONTAINER}-home" "${CONTAINER}-m2" "${CONTAINER}-npm")

docker version > /dev/null
log "демон: $(docker context show) ($(docker info --format '{{.Name}} {{.Architecture}}'))"

NODE_VERSION="$(sed -n 's/^node[[:space:]]*=[[:space:]]*"\(.*\)"/\1/p' "$ROOT_DIR/mise.toml" | head -1)"
[ -n "$NODE_VERSION" ] || { echo "не нашёл версию node в mise.toml" >&2; exit 1; }

log "образ $IMAGE"
docker build --quiet --build-arg "NODE_BOOTSTRAP_VERSION=$NODE_VERSION" -t "$IMAGE" "$ROOT_DIR/scripts/build-runner" > /dev/null

for volume in "${VOLUMES[@]}"; do
  docker volume inspect "$volume" > /dev/null 2>&1 || docker volume create "$volume" > /dev/null
  docker run --rm -v "$volume:/volume" alpine:3 chown -R "$RUNNER_UID:$RUNNER_GID" /volume > /dev/null
done

DOCKER_SOCKET_GID="$(docker run --rm -v /var/run/docker.sock:/var/run/docker.sock alpine:3 stat -c %g /var/run/docker.sock)"

if docker container inspect "$CONTAINER" > /dev/null 2>&1; then
  log "контейнер пересоздаётся"
  docker rm -f "$CONTAINER" > /dev/null
fi

docker create \
  --name "$CONTAINER" \
  --user "$RUNNER_UID:$RUNNER_GID" \
  --group-add "$DOCKER_SOCKET_GID" \
  --add-host host.docker.internal:host-gateway \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v "${VOLUMES[0]}:/work/src" \
  -v "${VOLUMES[1]}:/work/home" \
  -v "${VOLUMES[2]}:/cache/m2" \
  -v "${VOLUMES[3]}:/cache/npm" \
  "$IMAGE" > /dev/null
docker start "$CONTAINER" > /dev/null
docker exec -u 0 "$CONTAINER" chown -R "$RUNNER_UID:$RUNNER_GID" /work/src /work/home /cache/m2 /cache/npm
log "контейнер готов: $CONTAINER"
