# RBOT-OPS-015
FROM debian:bookworm-slim

ARG MISE_VERSION=2026.6.14
ARG NODE_BOOTSTRAP_VERSION

ENV DEBIAN_FRONTEND=noninteractive \
    MISE_DATA_DIR=/work/home/.local/share/mise \
    MISE_CACHE_DIR=/work/home/.cache/mise

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        build-essential ca-certificates curl git jq procps python3 unzip xz-utils \
    && rm -rf /var/lib/apt/lists/*

RUN host_arch="$(dpkg --print-architecture)" \
    && case "$host_arch" in \
        amd64) mise_arch=x64; node_arch=x64 ;; \
        arm64) mise_arch=arm64; node_arch=arm64 ;; \
        *) echo "сборочный контейнер не знает архитектуру $host_arch" >&2; exit 2 ;; \
       esac \
    && curl -fsSL --retry 5 --retry-delay 5 --connect-timeout 20 --max-time 300 \
        "https://github.com/jdx/mise/releases/download/v${MISE_VERSION}/mise-v${MISE_VERSION}-linux-${mise_arch}.tar.gz" \
        -o /tmp/mise.tgz \
    && tar -C /usr/local/bin --strip-components=2 -xzf /tmp/mise.tgz mise/bin/mise \
    && rm -f /tmp/mise.tgz \
    && curl -fsSL --retry 5 --retry-delay 5 --connect-timeout 20 --max-time 600 \
        "https://nodejs.org/dist/v${NODE_BOOTSTRAP_VERSION}/node-v${NODE_BOOTSTRAP_VERSION}-linux-${node_arch}.tar.xz" \
        -o /tmp/node.tar.xz \
    && tar -C /usr/local --strip-components=1 -xJf /tmp/node.tar.xz \
    && rm -f /tmp/node.tar.xz

WORKDIR /work/src

CMD ["sleep", "infinity"]
