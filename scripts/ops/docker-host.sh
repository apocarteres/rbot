#!/usr/bin/env bash

# MVP-01

rbot_docker_host() {
  if [ -n "${DOCKER_HOST:-}" ]; then
    printf '%s\n' "$DOCKER_HOST"
    return 0
  fi
  local endpoint socket
  endpoint="$(docker context inspect --format '{{.Endpoints.docker.Host}}' 2> /dev/null || true)"
  if [[ "$endpoint" == unix://* || "$endpoint" == tcp://* ]] && docker -H "$endpoint" version > /dev/null 2>&1; then
    printf '%s\n' "$endpoint"
    return 0
  fi
  for socket in "$HOME"/.lima/*/sock/docker.sock; do
    [ -S "$socket" ] || continue
    if docker -H "unix://$socket" version > /dev/null 2>&1; then
      printf 'unix://%s\n' "$socket"
      return 0
    fi
  done
  echo "Демон Docker не найден: задайте DOCKER_HOST или поднимите Lima-машину с Docker" >&2
  return 1
}

rbot_testcontainers_env() {
  local host
  host="$(rbot_docker_host)" || return 1
  export DOCKER_HOST="$host"
  if docker -H "$host" info --format '{{json .SecurityOptions}}' 2> /dev/null | grep -q rootless; then
    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-/run/user/$(id -u)/docker.sock}"
    export TESTCONTAINERS_HOST_OVERRIDE="${TESTCONTAINERS_HOST_OVERRIDE:-127.0.0.1}"
  fi
  printf '[docker] демон: %s\n' "$DOCKER_HOST" >&2
}
