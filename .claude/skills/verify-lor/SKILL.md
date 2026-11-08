---
name: verify-lor
description: "Drive Life on Rails on throwaway targets and prove a change works: the API and web screens on a test server (port 3857, database life_on_rails_test) built from a copy of the checkout, and the Android launcher on the T3_Phone_API35 emulator. Use before `atelier review request` on any LoR change that touches the server, the web UI or the Android app, and whenever asked to check, screenshot or verify LoR."
---

# Verify LoR

Run this on the surfaces your change touches before `atelier review request`,
and attach what it collects. Reviewers caught launcher bugs that authors' own
checks missed (lor-glg9, lor-aow8); this makes the author's check cheap.

The control CLI does every repeatable step with one command:

```
V=.claude/skills/verify-lor/verify.mjs      # from the checkout root
node $V help
```

stdout is JSON. Exit codes: 0 ok, 1 failed, 2 usage, 3 refused.
`references/features/` says, for each area, how to reach it, how to drive it
and what result proves it works: `tasks`, `goals`, `people`, `notes`, `today`
(the web screens), `api` and `launcher`. Read the file for the area you changed.

## Safety

Every command targets only throwaway things, and refuses (exit 3, with what to
do instead) when pointed elsewhere:

- **Database:** `life_on_rails_test` only. `seed`, `api` and `up` refuse the env
  file's `DATABASE_URL` and any other database. No child process starts with
  another `DATABASE_URL`, or inherits one from your shell.
- **Server:** the throwaway server on `127.0.0.1:3857`, built and served from a
  copy of the checkout (`/tmp/lor-ui-<checkout name>`), so `pnpm build` never
  replaces the `dist/` that `lor-web` serves. `api` and `screen` refuse lor-web's
  ports (3847, 3848, 3849 and whatever the env file sets) and any listener that
  is not `scripts/serve.mjs` on the test database.
- **Phone:** the `T3_Phone_API35` emulator only. `install` refuses a serial that
  is not `emulator-N`. Sumeet's phones may be connected over adb; never install
  to, drive or change them.
- **App data:** `install` uses `adb install -r` and never `pm clear`. Clearing
  data brings back `Prefs.DEFAULT_SERVER`, the real server.
- **Dependencies:** the copy's `node_modules` is a symlink to the checkout's
  (in a worktree, the main checkout's). Never run `pnpm install` in the copy.
- **Builds:** `build` runs `android/boat-build.sh` on the boat VM. Never run
  an Android Gradle task on this machine; only `:homecore`'s wasm development
  tasks run here, capped (AGENTS.md, "The phone app").

`tests/verify-lor.test.ts` (in `pnpm test`) shows each refusal. Never weaken a
guard to test it with a real target: every case there is a `--dry-run`.

## Doctor

```
node $V doctor
```

Reports, per surface (`api`, `web`, `launcher`), whether it is ready, and for
each check that is not, the `fix`: the test database and pending migrations (`migrations_from_other_branches`
counts ones applied by another branch's checks, which is harmless),
port 3857, the copy, Chrome, the emulator, the app's `server_url` on it, the
debug APK and the boat CLI. It also lists lor-web's ports, which nothing here
touches. Exit 0 only when every surface is ready.

## Launch

```
node $V up                 # copy, build in the copy, migrate the test database, start :3857
node $V seed admin         # optional: a graph seed from seeds/ (admin, deep-work, health)
node $V build              # launcher only: the debug APK, on boat (2 to 5 min warm)
node $V install            # launcher only: install on the emulator, server_url = http://10.0.2.2:3857
```

`up` rebuilds the copy from your working tree each time, so run it again after
every change. Its build output goes to `/tmp/lor-verify/<checkout>/up.log`, the
server's to `/tmp/lor-ui-server.log`. If the boat VM is stopped, `build` says
so: `boat resume bx_fxc6p2bw` (it stops itself 30 minutes after use).

For the launcher, start or open the emulator with T3 Code's device tools, not a
shell: load them with ToolSearch
`select:mcp__t3-code__device_list,mcp__t3-code__device_open,mcp__t3-code__device_screenshot,mcp__t3-code__device_close`,
then `device_list`, and `device_open` with hostId `local` and the id
`device_list` shows for T3_Phone_API35 (`T3_Phone_API35` while it is off,
`emulator-5554` once booted). Run `install` once it is booted.

## Drive

- **API:** `node $V api GET /status`, or
  `node $V api POST /intents --body '{"type":"task","title":"x"}'`. Paths are
  under `/api/v1`. Writes get `"thread":"verify-lor"` when the body has none.
  It prints `{method, path, status, body}`.
- **Web:** `node $V screen <tasks|goals|people|notes|today> <out-dir>` runs that
  flow of `scripts/ui-check.mjs` (each one first deletes rows in the test
  database; the feature file says which). `node $V screen /goals <out-dir>`
  takes one 1500x950 screenshot of a path and changes nothing. On main the
  `today` flow is out of date and fails at its first step (lor-mxrc); use
  `screen /` and the API for Today.
- **Launcher:** drive only through the `agent-device` command that `device_open`
  returned: `open com.spadaval.lor`, `snapshot -i`, `press @eN --settle`,
  `fill`, `home`, `back`, `screenshot <file>` (`agent-device help` lists the
  rest). Home draws placeholder icons for a second or two after `home`, so wait
  for a label (`wait text "<app name>"`) before a screenshot. Use
  `device_screenshot` to look. Never raw `adb shell input`. adb is only for
  what the CLI does: install, prefs, logs. If `open` says another session holds
  the device (an earlier thread's), follow its advice once
  (`open com.spadaval.lor --wait 30000`); never reuse or close that session.
  Close the device with `device_close` when you finish.

Drive the behaviour your change is about, including the motion or state change
a reviewer would watch (a drag, a swipe, a picked-up item), not just the end
screen.

## Evidence

Write evidence under `/tmp/lor-verify/<checkout>/` (or anywhere outside the
copy, which `down` deletes; `screen`, `api --out` and `logs` refuse a path
inside it):

- API: `node $V api GET /intents/<id> --out <dir>/api-intent.json` saves the
  response with the commit it ran against.
- Web: the PNGs and `<flow>.log` that `screen` writes.
- Launcher: `agent-device screenshot <dir>/<name>.png`, the snapshot text, and
  `node $V logs <dir>` (the app's logcat, the crash buffer and the server log).
  The server log holds startup and errors, not requests: logcat's `LorReads`
  line ("read /device/home: ok") is what shows the app reached the server.
- `node $V evidence <dir>` lists every file with its size and sha256.

Record one receipt per surface you checked, naming the commit, the device or
server and what each file shows. `--path` is accepted once, so point it at the
surface's directory and name the files in the summary. Don't record `pnpm test` or a boat build
passing; name those tests in the handoff instead:

```
atelier evidence record --target issue/<id> --kind validation --result pass \
  --path <dir> --producer <you> "<surface>, <commit>: <what the evidence shows>"
```

Attach the same paths to `atelier review request` so the reviewer can open them.

## Cleanup

```
node $V down               # stop the :3857 server and delete the copy; evidence stays
node $V evidence <dir>     # confirm the evidence survived
```

Then close the emulator in T3 Code (`device_close`; add `shutdown: true` only if
you booted it). Leave the boat VM to its own auto-stop: other agents' builds may
be queued there. Leave the app installed and pointed at the test server.
