#!/usr/bin/env bash
set -euo pipefail

# REQ-PUBLISHING-010, REQ-ADOPTION-007, REQ-ADOPTION-008, REQ-BUILD-007, MVP-01

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PLATFORM_REPO="${RBOT_PLATFORM_REPO:-https://github.com/apocarteres/platform.git}"
PLATFORM_DIR="${RBOT_PLATFORM_DIR:-$ROOT_DIR/.platform}"
VERSION_FILE="$ROOT_DIR/.platform-version"
ARCHIVE_CACHE_ROOT="${RBOT_PLATFORM_ARCHIVE_CACHE:-${HOME}/.cache/rbot-platform}"
FRONTEND_PACKAGES=(auth http app-update)
ENSURE=0
MAVEN_ONLY=0

while [ "$#" -gt 0 ]; do
  case "$1" in
    --ensure) ENSURE=1; shift ;;
    --maven-only) MAVEN_ONLY=1; shift ;;
    *) echo "Неизвестный ключ: $1" >&2; exit 2 ;;
  esac
done

log() {
  printf '[platform-install] %s\n' "$*"
}

maven_local_repository() {
  local from_opts
  from_opts="$(printf '%s\n' "${MAVEN_OPTS:-}" | sed -n 's/.*-Dmaven\.repo\.local=\([^[:space:]]*\).*/\1/p')"
  printf '%s' "${from_opts:-${HOME}/.m2/repository}"
}

[ -f "$VERSION_FILE" ] || { echo "Нет файла $VERSION_FILE с версией ядра" >&2; exit 2; }
REF="$(tr -d '[:space:]' < "$VERSION_FILE")"
[ -n "$REF" ] || { echo "Версия ядра в $VERSION_FILE пуста" >&2; exit 2; }
VERSION="${REF#v}"
MAVEN_REPO="$(maven_local_repository)/io/github/apocarteres/platform"
PACKAGES_DIR="$PLATFORM_DIR/target/local-packages"

POM_VERSION="$(awk '/<artifactId>platform-service-parent<\/artifactId>/{found=1} found && /<version>/{gsub(/.*<version>|<\/version>.*/, ""); print; exit}' "$ROOT_DIR/pom.xml")"
if [ "$POM_VERSION" != "$VERSION" ]; then
  echo "Версия ядра расходится: .platform-version=$REF, platform-service-parent в pom.xml=${POM_VERSION:-нет}" >&2
  exit 1
fi

check_node_engines() {
  node - "$ROOT_DIR/package.json" "$(npm -v)" <<'NODE'
const [file, npmVersion] = process.argv.slice(2);
const engines = require(file).engines ?? {};
const fits = (actual, range) => {
  const match = /^\^(\d+)\.(\d+)\.(\d+)$/.exec(range ?? '');
  if (!match) return true;
  const [major, minor, patch] = actual.replace(/^v/, '').split('.').map(Number);
  const [wantMajor, wantMinor, wantPatch] = match.slice(1).map(Number);
  return major === wantMajor && (minor > wantMinor || (minor === wantMinor && patch >= wantPatch));
};
const wrong = [['node', process.version, engines.node], ['npm', npmVersion, engines.npm]]
  .filter(([, actual, range]) => !fits(actual, range))
  .map(([name, actual, range]) => `${name} ${actual}, нужен ${range}`);
if (wrong.length > 0) {
  console.error(`Версии инструментов не те, что закреплены в engines: ${wrong.join('; ')}. Инструменты из mise.toml должны стоять в PATH первыми.`);
  process.exit(1);
}
NODE
}

archive() {
  printf '%s/apocarteres-%s-%s.tgz' "$PACKAGES_DIR" "$1" "$VERSION"
}

install_node_packages() {
  local archives=() name
  for name in "${FRONTEND_PACKAGES[@]}"; do
    [ -f "$(archive "$name")" ] || { echo "Архив пакета ядра не собран: $(archive "$name")" >&2; exit 1; }
    archives+=("$(archive "$name")")
  done
  log "ставлю пакет правил в корень"
  [ -f "$(archive project-conventions)" ] || { echo "Архив пакета правил не собран: $(archive project-conventions)" >&2; exit 1; }
  (cd "$ROOT_DIR" && npm install --save-dev --no-audit --no-fund --loglevel=error "$(archive project-conventions)")
  log "ставлю пакет правил в клиент"
  (cd "$ROOT_DIR/frontend" && npm install --save-dev --no-audit --no-fund --loglevel=error "$(archive project-conventions)")
  log "ставлю пакеты ядра в клиент: ${FRONTEND_PACKAGES[*]}"
  (cd "$ROOT_DIR/frontend" && npm install --no-audit --no-fund --loglevel=error "${archives[@]}")
}

maven_ready() {
  [ -f "$MAVEN_REPO/platform-persistence/$VERSION/platform-persistence-$VERSION.jar" ] \
    && [ -f "$MAVEN_REPO/platform-time/$VERSION/platform-time-$VERSION.jar" ]
}

if [ "$ENSURE" -eq 1 ] && maven_ready \
    && { [ "$MAVEN_ONLY" -eq 1 ] || { [ -x "$ROOT_DIR/node_modules/.bin/conventions" ] && [ -f "$(archive project-conventions)" ]; }; }; then
  log "ядро $REF уже установлено"
  exit 0
fi

if [ "$MAVEN_ONLY" -eq 0 ]; then
  check_node_engines
fi

if maven_ready && [ -f "$ARCHIVE_CACHE_ROOT/$VERSION/apocarteres-project-conventions-${VERSION}.tgz" ]; then
  log "ставлю пакеты ядра $REF из кэша архивов: сборка не нужна"
  if [ "$MAVEN_ONLY" -eq 0 ]; then
    mkdir -p "$PACKAGES_DIR"
    cp "$ARCHIVE_CACHE_ROOT/$VERSION/"*.tgz "$PACKAGES_DIR/"
    install_node_packages
  fi
  log "ядро $REF готово из кэша"
  exit 0
fi

if [ ! -d "$PLATFORM_DIR/.git" ]; then
  log "клонирую ядро в $PLATFORM_DIR"
  mkdir -p "$PLATFORM_DIR"
  git -C "$PLATFORM_DIR" init -q
  git -C "$PLATFORM_DIR" remote add origin "$PLATFORM_REPO"
fi

log "получаю $REF"
git -C "$PLATFORM_DIR" fetch --prune --tags origin
git -C "$PLATFORM_DIR" checkout --force --detach "$REF"
git -C "$PLATFORM_DIR" clean -fdx -e target/

log "публикую ядро локально"
(cd "$PLATFORM_DIR" && ./scripts/install-local.sh "$VERSION")

mkdir -p "$ARCHIVE_CACHE_ROOT/$VERSION"
cp "$PACKAGES_DIR/"apocarteres-*-"${VERSION}".tgz "$ARCHIVE_CACHE_ROOT/$VERSION/"
log "архивы ядра сохранены в кэш: $ARCHIVE_CACHE_ROOT/$VERSION"

if [ "$MAVEN_ONLY" -eq 0 ]; then
  install_node_packages
fi
log "ядро $REF готово"
