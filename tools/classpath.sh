#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Build classpath from existing jar folders.
# By default, disabled libs are excluded. Set INCLUDE_DISABLED_LIBS=1 to include them.
LIB_DIRS=("$ROOT_DIR/lib" "$ROOT_DIR/libs")
if [[ "${INCLUDE_DISABLED_LIBS:-0}" == "1" ]]; then
  LIB_DIRS+=("$ROOT_DIR/lib_disabled")
fi

CP_ENTRIES=("$ROOT_DIR/target/classes")

for dir in "${LIB_DIRS[@]}"; do
  if [[ -d "$dir" ]]; then
    while IFS= read -r -d '' jar; do
      CP_ENTRIES+=("$jar")
    done < <(find "$dir" -maxdepth 1 -type f -name "*.jar" -print0 | sort -z)
  fi
done

# Optional: include extra system jars if explicitly requested.
if [[ "${INCLUDE_SYSTEM_JARS:-0}" == "1" ]]; then
  while IFS= read -r -d '' jar; do
    CP_ENTRIES+=("$jar")
  done < <(find /usr/share/java -maxdepth 1 -type f \( -name "log4j-api*.jar" -o -name "log4j-core*.jar" \) -print0 2>/dev/null | sort -z)
fi

# De-duplicate entries while preserving order.
declare -A seen=()
DEDUPED=()
for entry in "${CP_ENTRIES[@]}"; do
  if [[ -n "$entry" && -z "${seen[$entry]:-}" ]]; then
    DEDUPED+=("$entry")
    seen[$entry]=1
  fi
done

(IFS=:; printf '%s\n' "${DEDUPED[*]}")

