#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
# shellcheck source=cncf-common.sh
source "$SCRIPT_DIR/cncf-common.sh"

cd "$PROJECT_ROOT"
exec /Users/asami/.codex/skills/cncf-sbt-serial-execution/scripts/run-sbt-serial.sh \
  --batch cozyPrepareRuntime
