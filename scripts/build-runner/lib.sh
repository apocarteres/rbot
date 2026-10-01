#!/usr/bin/env bash

# RBOT-OPS-015

rbot_runner_container() {
  printf '%s\n' "${RBOT_BUILD_RUNNER_CONTAINER:-rbot-build-runner}"
}

rbot_runner_require() {
  local container
  container="$(rbot_runner_container)"
  if ! docker container inspect "$container" > /dev/null 2>&1; then
    printf 'сборочного контейнера нет: %s. Подготовьте его: mise run build-runner-prepare\n' "$container" >&2
    return 1
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$container")" != "true" ]; then
    docker start "$container" > /dev/null
  fi
}

rbot_runner_snapshot() {
  local repo="${1:?нужен путь репозитория}"
  local index tree commit
  index="$(mktemp)"
  rm -f "$index"
  GIT_INDEX_FILE="$index" git -C "$repo" add -A
  tree="$(GIT_INDEX_FILE="$index" git -C "$repo" write-tree)"
  rm -f "$index"
  commit="$(git -C "$repo" commit-tree "$tree" -p HEAD -m "build-runner: снимок рабочего дерева")"
  git -C "$repo" update-ref refs/build-runner/worktree "$commit"
  printf '%s\n' "$commit"
}

rbot_runner_load_source() {
  local repo="${1:?нужен путь репозитория}"
  local commit="${2:?нужен коммит}"
  local container
  container="$(rbot_runner_container)"
  docker restart "$container" > /dev/null
  docker exec "$container" bash -lc 'find /work/src -mindepth 1 -maxdepth 1 -exec rm -rf {} +; rm -f /tmp/rbot-source.bundle'
  git -C "$repo" bundle create - --all refs/build-runner/worktree 2> /dev/null \
    | docker exec -i "$container" bash -lc 'cat > /tmp/rbot-source.bundle'
  docker exec "$container" bash -lc "
    set -eu
    git clone --quiet --no-checkout /tmp/rbot-source.bundle /work/src
    git -C /work/src checkout --quiet --force --detach '$commit'
    rm -f /tmp/rbot-source.bundle
  "
}

rbot_runner_exec() {
  docker exec \
    -e HOME=/work/home \
    -e LC_ALL=C.UTF-8 \
    -e LANG=C.UTF-8 \
    -e npm_config_cache=/cache/npm \
    -e MAVEN_OPTS="-Dmaven.repo.local=/cache/m2" \
    -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
    -w /work/src \
    "$(rbot_runner_container)" \
    bash -lc "$1"
}
