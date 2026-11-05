#!/bin/bash
# Build the LoR Android app on the boat VM lor-android-build (set up by boat-setup.sh).
# Usage: android/boat-build.sh <checkout-root> <out-dir> [gradle tasks...]
# Default tasks: assembleDebug testDebugUnitTest (task arguments must not contain quotes).
# Syncs the checkout's android/ to the VM, builds there under a lock, and on success copies
# outputs/apk/debug/app-debug.apk and the unit-test reports into <out-dir>. Fails, without
# resuming it, if the VM is stopped: `boat resume bx_fxc6p2bw`, then `boat stop` when done.
set -euo pipefail
[ $# -ge 2 ] || { sed -n 3,7p "$0"; exit 2; }
B=${LOR_BOAT_VM:-bx_fxc6p2bw}
ROOT=$(cd "$1" && pwd); OUT=$2; shift 2
TASKS=${*:-assembleDebug testDebugUnitTest}
# One directory per checkout path on the VM.
NAME=$(basename "$ROOT")-$(echo "$ROOT" | sha1sum | cut -c1-8)
mkdir -p "$OUT"
rm -rf "$OUT/outputs" "$OUT/reports" "$OUT/test-results" "$OUT/build.log"
TGZ=$(mktemp --suffix=.tgz)
trap 'rm -f "$TGZ"' EXIT
(cd "$ROOT" && git ls-files -co --exclude-standard android | grep -v '^android/local.properties$' \
  | while IFS= read -r f; do [ -e "$f" ] && echo "$f"; done | tar czf "$TGZ" -T -)
boat extend "$B" --ttl 1800 >/dev/null
boat scp "$TGZ" "$B:/home/user/src-$NAME.tgz" >/dev/null
# Everything that touches this checkout's files on the VM runs under the lock. Old sources are
# replaced (Gradle's caches and build dirs kept), and old artifacts are deleted before the build.
INNER="source ~/lor-env.sh; set -o pipefail; D=~/w/$NAME/android; mkdir -p \$D;
  find \$D -mindepth 1 -maxdepth 1 ! -name .gradle ! -name app -exec rm -rf {} +;
  [ -d \$D/app ] && find \$D/app -mindepth 1 -maxdepth 1 ! -name build -exec rm -rf {} +;
  rm -rf \$D/app/build/outputs/apk ~/out-$NAME.tgz;
  tar xzf ~/src-$NAME.tgz -C ~/w/$NAME && cd \$D && echo sdk.dir=\$ANDROID_HOME > local.properties &&
  ./gradlew --console=plain $TASKS > ~/build-$NAME.log 2>&1;
  rc=\$?; cd \$D/app/build 2>/dev/null && tar czf ~/out-$NAME.tgz \$(ls -d outputs/apk/debug/app-debug.apk reports/tests test-results 2>/dev/null) 2>/dev/null; exit \$rc"
PID=$(boat exec "$B" --detach --json -- "flock /tmp/lor-build.lock bash -c '$INNER'" | python3 -c 'import json,sys; print(json.load(sys.stdin)["processId"])')
fails=0
for _ in $(seq 1 240); do
  sleep 15
  if ! ST=$(boat exec "$B" --status "$PID" --json 2>/dev/null); then
    fails=$((fails + 1)); [ $fails -ge 8 ] && { echo "boat-build: lost the VM (is $B stopped?)" >&2; exit 1; }; continue
  fi
  fails=0
  RUNNING=$(echo "$ST" | python3 -c 'import json,sys; d=json.load(sys.stdin); print(d.get("running"))')
  [ "$RUNNING" = "False" ] && break
done
[ "${RUNNING:-}" = "False" ] || { echo "boat-build: still running after an hour" >&2; exit 1; }
boat scp "$B:/home/user/build-$NAME.log" "$OUT/build.log" >/dev/null
grep -E 'BUILD (SUCCESSFUL|FAILED)' "$OUT/build.log" || { echo "boat-build: no result in the log" >&2; tail -20 "$OUT/build.log"; exit 1; }
grep -q 'BUILD SUCCESSFUL' "$OUT/build.log" || { grep -E '^e: |error:|FAILED' "$OUT/build.log" | head -20; exit 1; }
boat scp "$B:/home/user/out-$NAME.tgz" "$OUT/out.tgz" >/dev/null
tar xzf "$OUT/out.tgz" -C "$OUT" && rm "$OUT/out.tgz"
