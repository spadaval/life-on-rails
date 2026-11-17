#!/bin/bash
# The launcher lab, rebuilt on every save: homecore's wasm development build in Gradle's continuous
# mode, served with live reload at http://127.0.0.1:${LOR_LAB_PORT:-3860}/ (loopback only).
# Usage: android/web-dev.sh [--only <concept>[,<concept>]] [--dry-run]
#
# --only compiles just the named design concepts (homecore/build.gradle.kts's `concepts` list,
# passed as -Plor.concepts): the lab then holds current, the concepts' shared kit and those
# concepts, and the others drop out of the Version dropdown. Work on one concept with it set to
# that concept: with every concept in, the build no longer fits the scope (lor-j1fe below). Without
# it every concept is compiled, as boat's full bundle is. The bundle a build with --only leaves in
# homecore/build/dist is not the lab to publish: publish one built with every concept.
#
# Decision lor-0v68 (spike lor-8a7n's numbers) is why it looks like this: the only Gradle build
# allowed on this host, in a scope capped at 4 GB of memory and 512 MB of swap, Gradle at -Xmx1g,
# its own Kotlin daemon at -Xmx1536m (-Dlor.web=1 keeps any other build from sharing that daemon,
# and it from outliving the scope), two workers, incremental wasm, one at a time, and never beside
# an emulator. ~/.gradle/init.d/no-local-builds.gradle refuses the build outside such a scope.
# It removes homecore's own compiled code and development link before starting (not the link's
# cache of its libraries), so the continuous build compiles and links once itself. With home, the
# panels, the app screens and the first concept in the module (lor-5fk6, 2026-11-17): a first build
# of 32 to 68 s, then an edit takes about 21 s cold and 13 to 16 s warm, and a long session peaks at
# about 4,080 MB of the scope's 4 GB (3,730 MB of it anon, never killed). A compile in another
# process does not warm it.
# Known crash (Kotlin 2.3.20): an incremental compile whose changed files are all in wasmJsMain
# fails with ArrayIndexOutOfBoundsException in WasmIrFileMetadata.fromByteArray, and every build
# after it fails the same way until this script restarts. Changes that include a file from webMain
# or commonMain build. So the design concepts live in webMain; after editing lab/Main.kt or another
# wasmJsMain file, restart this script.
# On exit (Ctrl-C) the scope is stopped, Kotlin daemon included, and its peak memory printed.
# --dry-run runs the refusals and prints the command without starting it.
set -euo pipefail
cd "$(dirname "$0")"
ONLY=() DRY=
while [ $# -gt 0 ]; do
  case "$1" in
    --only) [ $# -ge 2 ] || { echo "web-dev: --only needs a concept name" >&2; exit 2; }; ONLY=("-Plor.concepts=$2"); shift 2 ;;
    --only=*) ONLY=("-Plor.concepts=${1#--only=}"); shift ;;
    --dry-run) DRY=1; shift ;;
    *) echo "web-dev: unknown argument $1 (usage: web-dev.sh [--only <concept>] [--dry-run])" >&2; exit 2 ;;
  esac
done
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
  -Pkotlin.incremental.wasm=true "${ONLY[@]}")
CMD=(systemd-run --user --scope --quiet --slice=agents.slice --unit="$UNIT"
  -p MemoryMax=4G -p MemorySwapMax=512M
  env LOR_LAB_PORT="$PORT"
  "${GRADLE[@]}" --continuous :homecore:wasmJsBrowserDevelopmentRun)

if [ -n "$DRY" ]; then printf '%q ' "${CMD[@]}"; echo; exit 0; fi

rm -rf homecore/build/classes/kotlin/wasmJs/main homecore/build/compileSync/wasmJs/main/developmentExecutable

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
