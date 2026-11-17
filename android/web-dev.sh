#!/bin/bash
# The launcher lab, rebuilt on every save: homecore's wasm development build in Gradle's continuous
# mode, served with live reload at http://127.0.0.1:${LOR_LAB_PORT:-3860}/ (loopback only).
# Usage: android/web-dev.sh [--dry-run]
#
# Decision lor-0v68 (spike lor-8a7n's numbers) is why it looks like this: the only Gradle build
# allowed on this host, in a scope capped at 4 GB of memory and 512 MB of swap, Gradle at -Xmx1g,
# its own Kotlin daemon at -Xmx1536m (-Dlor.web=1 keeps any other build from sharing that daemon,
# and it from outliving the scope), two workers, incremental wasm, one at a time, and never beside
# an emulator. ~/.gradle/init.d/no-local-builds.gradle refuses the build outside such a scope.
# On exit (Ctrl-C) the scope is stopped, Kotlin daemon included, and its peak memory printed.
# --dry-run runs the refusals and prints the command without starting it.
set -euo pipefail
cd "$(dirname "$0")"
UNIT=lor-web-dev
PORT=${LOR_LAB_PORT:-3860}

refuse() { echo "web-dev: $*" >&2; exit 3; }
if pgrep '^qemu-system' >/dev/null; then
  refuse "an Android emulator runs on this host; decision lor-0v68 allows this build only without one (emulators belong on the Mac)"
fi
case "$PORT" in
  3847|3848|3849|3857|3858|3859) refuse "port $PORT belongs to lor-web, a test server or the dashboard prototype; pick another with LOR_LAB_PORT" ;;
esac
if systemctl --user is-active --quiet "$UNIT.scope"; then
  refuse "another web-dev.sh is running ($UNIT.scope); stop it first: systemctl --user stop $UNIT.scope"
fi
if ss -Htln "sport = :$PORT" | grep -q .; then refuse "port $PORT is taken"; fi

GRADLE=(./gradlew --no-daemon --console=plain --max-workers=2
  -Dorg.gradle.jvmargs="-Xmx1g -Dfile.encoding=UTF-8"
  "-Pkotlin.daemon.jvmargs=-Xmx1536m -Dlor.web=1"
  -Pkotlin.incremental.wasm=true)
CMD=(systemd-run --user --scope --quiet --slice=agents.slice --unit="$UNIT"
  -p MemoryMax=4G -p MemorySwapMax=512M
  env LOR_LAB_PORT="$PORT"
  "${GRADLE[@]}" --continuous :homecore:wasmJsBrowserDevelopmentRun)

if [ "${1:-}" = --dry-run ]; then printf '%q ' "${CMD[@]}"; echo; exit 0; fi

stop() {
  local cg peak
  cg=$(systemctl --user show -P ControlGroup "$UNIT.scope" 2>/dev/null || true)
  peak=$( [ -n "$cg" ] && cat "/sys/fs/cgroup$cg/memory.peak" 2>/dev/null || true)
  systemctl --user stop "$UNIT.scope" 2>/dev/null || true
  [ -n "$peak" ] && echo "web-dev: the scope's peak memory was $((peak / 1048576)) MB" >&2 || true
}
trap stop EXIT
echo "web-dev: the lab at http://127.0.0.1:$PORT/ once the first build is done" >&2
"${CMD[@]}"
