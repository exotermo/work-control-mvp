#!/usr/bin/env bash

set -euo pipefail

work_control_script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
work_control_root="$(cd "$work_control_script_dir/../.." && pwd)"
work_control_sdkman_jdk="${HOME}/.sdkman/candidates/java/21.0.2-tem"
work_control_java_home="${JAVA_HOME:-}"
work_control_go_cache="${TMPDIR:-/tmp}/work-control-go-cache"

if [[ ! -x "$work_control_java_home/bin/jlink" ]]; then
    if [[ -x "$work_control_sdkman_jdk/bin/jlink" ]]; then
        work_control_java_home="$work_control_sdkman_jdk"
    else
        echo "Erro: selecione um JDK completo com jlink antes de continuar." >&2
        exit 1
    fi
fi

echo "[1/3] Android: testes unitários, APK e APK de instrumentação"
(
    cd "$work_control_root/apps/android"
    JAVA_HOME="$work_control_java_home" ./gradlew \
        testDebugUnitTest assembleDebug :app:assembleDebugAndroidTest
)

echo "[2/3] API Go: testes automatizados"
(
    cd "$work_control_root/services/api-go"
    GOCACHE="$work_control_go_cache" go test ./...
)

echo "[3/3] Infra: validação sintática do Docker Compose"
docker compose \
    -f "$work_control_root/infra/docker/docker-compose.yml" \
    config --quiet
docker compose \
    -f "$work_control_root/infra/docker/docker-compose.yml" \
    --profile oidc \
    config --quiet

echo "Baseline local validada com sucesso."
