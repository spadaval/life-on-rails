#!/bin/bash
# Build the LoR Android app on the boat VM lor-android-build (set up by boat-setup.sh).
# Usage: android/boat-build.sh <checkout-root> <out-dir> [gradle tasks...]
# Default tasks: assembleDebug testDebugUnitTest (task arguments and the checkout's name must not
# contain quotes or spaces; tasks must produce a debug APK or unit-test results, not a release).
# Syncs the checkout's android/ to the VM, builds there under a lock, and on success copies
# outputs/apk/debug/app-debug.apk and the unit-test reports into <out-dir>. Fails, without
# resuming it, if the VM is stopped (`boat resume bx_fxc6p2bw`; it stops itself 30 min after use).
set -euo pipefail
[ $# -ge 2 ] || { sed -n 3,8p "$0"; exit 2; }
B=${LOR_BOAT_VM:-bx_fxc6p2bw}
ROOT=$(cd "$1" && pwd); OUT=$2; shift 2
TASKS=${*:-assembleDebug testDebugUnitTest}
# A build directory per checkout path on the VM; transfer files and logs per run.
NAME=$(basename "$ROOT")-$(echo "$ROOT" | sha1sum | cut -c1-8)
RUN=$NAME-$$-$RANDOM
mkdir -p "$OUT"
rm -rf "$OUT/outputs" "$OUT/reports" "$OUT/test-results" "$OUT/build.log"
TGZ=$(mktemp --suffix=.tgz)
trap 'rm -f "$TGZ"' EXIT
(cd "$ROOT" && git ls-files -co --exclude-standard android | grep -v '^android/local.properties$' \
  | while IFS= read -r f; do if [ -e "$f" ]; then echo "$f"; fi; done | tar czf "$TGZ" -T -)
boat extend "$B" --ttl 1800 >/dev/null
boat scp "$TGZ" "$B:/home/user/src-$RUN.tgz" >/dev/null
# Everything that touches this checkout's files on the VM runs under the lock. Old sources are
# replaced (Gradle's caches and build dirs kept), and old outputs are deleted before the build.
INNER="source ~/lor-env.sh; D=~/w/$NAME/android; mkdir -p \$D;
  find \$D -mindepth 1 -maxdepth 1 ! -name .gradle ! -name app -exec rm -rf {} +;
  [ -d \$D/app ] && find \$D/app -mindepth 1 -maxdepth 1 ! -name build -exec rm -rf {} +;
  rm -rf \$D/app/build/outputs/apk \$D/app/build/reports/tests \$D/app/build/test-results;
  tar xzf ~/src-$RUN.tgz -C ~/w/$NAME && rm ~/src-$RUN.tgz && cd \$D && echo sdk.dir=\$ANDROID_HOME > local.properties &&
  ./gradlew --console=plain $TASKS > ~/build-$RUN.log 2>&1;
  rc=\$?; cd \$D/app/build 2>/dev/null && tar czf ~/out-$RUN.tgz \$(ls -d outputs/apk/debug/app-debug.apk reports/tests test-results 2>/dev/null) 2>/dev/null; exit \$rc"
PID=$(boat exec "$B" --detach --json -- "flock /tmp/lor-build.lock bash -c '$INNER'" | python3 -c 'import json,sys; print(json.load(sys.stdin)["processId"])')
fails=0; EXIT=
for i in $(seq 1 240); do
  sleep 15
  # Keep the VM up while this build waits or runs.
  [ $((i % 40)) -eq 0 ] && boat extend "$B" --ttl 1800 >/dev/null 2>&1 || true
  ST=$(boat exec "$B" --status "$PID" --json 2>/dev/null) || ST=
  STATE=$(echo "$ST" | python3 -c 'import json,sys; d=json.load(sys.stdin); print("running" if d.get("running") else "exit:%s" % d.get("exitCode"))' 2>/dev/null) || STATE=
  if [ -z "$STATE" ]; then
    fails=$((fails + 1)); [ $fails -ge 8 ] && { echo "boat-build: lost the VM (is $B stopped?)" >&2; exit 1; }; continue
  fi
  fails=0
  [ "$STATE" = running ] && continue
  EXIT=${STATE#exit:}; break
done
[ -n "$EXIT" ] || { echo "boat-build: still running after an hour" >&2; exit 1; }
boat scp "$B:/home/user/build-$RUN.log" "$OUT/build.log" >/dev/null 2>&1 || true
boat exec "$B" -- "rm -f ~/build-$RUN.log ~/src-$RUN.tgz" >/dev/null 2>&1 || true
if [ "$EXIT" != 0 ]; then
  echo "boat-build: build exited $EXIT" >&2
  [ -f "$OUT/build.log" ] && { grep -E '^e: |error:|FAILED|BUILD' "$OUT/build.log" | head -20 || tail -20 "$OUT/build.log"; }
  boat exec "$B" -- "rm -f ~/out-$RUN.tgz" >/dev/null 2>&1 || true
  exit 1
fi
grep -E 'BUILD SUCCESSFUL' "$OUT/build.log"
boat scp "$B:/home/user/out-$RUN.tgz" "$OUT/out.tgz" >/dev/null
boat exec "$B" -- "rm -f ~/out-$RUN.tgz" >/dev/null 2>&1 || true
tar xzf "$OUT/out.tgz" -C "$OUT" && rm "$OUT/out.tgz"
