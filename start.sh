#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALG_SCRIPT="$ROOT_DIR/tools/alg.sh"

if [[ ! -x "$ALG_SCRIPT" ]]; then
  chmod +x "$ALG_SCRIPT"
fi

usage() {
  cat <<'EOF'
Usage:
  ./start.sh [--skip-compile] [MainClass]

Examples:
  ./start.sh
  ./start.sh AlgDatabankGui
  ./start.sh --skip-compile KonkreterProbedruck
EOF
}

skip_compile=0
main_class="AlgDatabankGui"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-compile)
      skip_compile=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      main_class="$1"
      shift
      ;;
  esac
done

cd "$ROOT_DIR"

if [[ "$skip_compile" -eq 0 ]]; then
  ./tools/alg.sh compile
fi

exec ./tools/alg.sh run "$main_class"

