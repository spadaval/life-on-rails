#!/bin/bash
# Build the LoR Android app on the boat VM lor-android-build.
# Usage: android/boat-build.sh <checkout-root> <out-dir> [gradle tasks...]
# Default tasks: assembleDebug testDebugUnitTest. Copies app-debug.apk and the unit-test
# reports to <out-dir>. Each checkout gets its own directory on the VM; builds queue on a lock.
set -euo pipefail
B=${LOR_BOAT_VM:-bx_fxc6p2bw}
ROOT=$(cd "$1" && pwd); OUT=$2; shift 2
TASKS=${*:-assembleDebug testDebugUnitTest}
NAME=$(basename "$ROOT")
mkdir -p "$OUT"
TGZ=$(mktemp --suffix=.tgz)
(cd "$ROOT" && git ls-files -co --exclude-standard android | grep -v '^android/local.properties$' | tar czf "$TGZ" -T -)
boat extend "$B" --ttl 1800 >/dev/null 2>&1 || true
boat scp "$TGZ" "$B:/home/user/src-$NAME.tgz" >/dev/null
rm -f "$TGZ"
CMD="flock /tmp/lor-build.lock bash -c 'source ~/lor-env.sh; rm -rf ~/w/$NAME/android/app/src; mkdir -p ~/w/$NAME && tar xzf ~/src-$NAME.tgz -C ~/w/$NAME && cd ~/w/$NAME/android && echo sdk.dir=\$ANDROID_HOME > local.properties && /usr/bin/time -v ./gradlew --console=plain $TASKS' > ~/build-$NAME.log 2>&1"
PID=$(boat exec "$B" --detach --json -- "$CMD" | python3 -c 'import json,sys; print(json.load(sys.stdin)["processId"])')
while :; do
  sleep 15
  ST=$(boat exec "$B" --status "$PID" --json 2>/dev/null || true)
  echo "$ST" | grep -q '"running":true' && continue
  echo "$ST" | grep -q '"running":false' && break
done
boat scp "$B:/home/user/build-$NAME.log" "$OUT/build.log" >/dev/null
boat exec "$B" -- "cd ~/w/$NAME/android/app/build && tar czf ~/out-$NAME.tgz outputs/apk/debug/app-debug.apk reports/tests test-results 2>/dev/null; true" >/dev/null
boat scp "$B:/home/user/out-$NAME.tgz" "$OUT/out.tgz" >/dev/null && tar xzf "$OUT/out.tgz" -C "$OUT" && rm "$OUT/out.tgz"
grep -E 'BUILD (SUCCESSFUL|FAILED)|Elapsed|Maximum resident|Percent of CPU' "$OUT/build.log" || true
grep -q 'BUILD SUCCESSFUL' "$OUT/build.log"
