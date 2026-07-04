#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLASSPATH_SCRIPT="$ROOT_DIR/tools/classpath.sh"
JAVA_FX_MODULE_PATH="${JAVA_FX_MODULE_PATH:-/usr/share/openjfx/lib}"
JAVA_FX_MODULES="${JAVA_FX_MODULES:-javafx.controls,javafx.web,javafx.swing}"
JAVA_NATIVE_ACCESS_FLAGS="${JAVA_NATIVE_ACCESS_FLAGS:---enable-native-access=javafx.graphics --enable-native-access=javafx.web --enable-native-access=javafx.media --enable-native-access=ALL-UNNAMED}"

if [[ ! -x "$CLASSPATH_SCRIPT" ]]; then
  chmod +x "$CLASSPATH_SCRIPT"
fi

usage() {
  cat <<'EOF'
Usage:
  tools/alg.sh classpath
  tools/alg.sh compile
  tools/alg.sh run [MainClass]

Env flags:
  INCLUDE_DISABLED_LIBS=1   Include jars from lib_disabled/
  INCLUDE_SYSTEM_JARS=1     Include /usr/share/java/log4j-{api,core}*.jar when present
EOF
}

copy_resources() {
  mkdir -p "$ROOT_DIR/target/classes"
  if [[ -d "$ROOT_DIR/src/main/resources" ]]; then
    cp -a "$ROOT_DIR/src/main/resources/." "$ROOT_DIR/target/classes/"
  fi
}

compile_all() {
  copy_resources
  local cp
  cp="$($CLASSPATH_SCRIPT)"

  # Exclude optional utility classes requiring dependencies not bundled in this project.
  local files=()
  while IFS= read -r -d '' f; do
    files+=("$f")
  done < <(find "$ROOT_DIR/src/main/java" -maxdepth 1 -name "*.java" \
    ! -name "HtmlSupport.java" \
    ! -name "TestStatusLogger.java" \
    -print0)

  javac \
    --module-path "$JAVA_FX_MODULE_PATH" \
    --add-modules "$JAVA_FX_MODULES" \
    -cp "$cp" \
    -d "$ROOT_DIR/target/classes" \
    "${files[@]}"
}

run_main() {
  local main_class="${1:-AlgDatabankGui}"
  local cp
  cp="$($CLASSPATH_SCRIPT)"

  java \
    $JAVA_NATIVE_ACCESS_FLAGS \
    --module-path "$JAVA_FX_MODULE_PATH" \
    --add-modules "$JAVA_FX_MODULES" \
    -cp "$cp" \
    "$main_class"
}

cmd="${1:-}"
case "$cmd" in
  classpath)
    "$CLASSPATH_SCRIPT"
    ;;
  compile)
    compile_all
    ;;
  run)
    shift || true
    run_main "${1:-AlgDatabankGui}"
    ;;
  *)
    usage
    exit 1
    ;;
esac

