# Launcher

The Android app's home screen (T3 Launcher, `com.spadaval.lor/.home.HomeActivity`) draws home from `layout.json`, a grid of panels holding the agent page, app stacks and the drawer.

## Reach it

- Code: `android/app/src/main/java/com/spadaval/lor/home/` (`HomeActivity.kt`, `HomeScreen.kt`, `GridHost.kt`, `HomeStore.kt`, `Layout.kt`), the agent page in `agent/`, the LoR app tabs in `MainActivity.kt` and `ui/`. The schema is `docs/missions/launcher-panels/layout-v2.md`; the parser `Layout.kt` is the truth.
- As a user: with LoR as the default home app, a home press shows home. The "Life on Rails" icon opens the LoR app instead, with tabs Timer, Sensors, Settings and Dashboard. While LoR is not the home app, a strip above the tabs offers Set as home.
- Settings tab: the "LoR address" field (`Prefs.serverUrl`, stored as `server_url` in `shared_prefs/lor.xml`), "This phone's name", Save, Test connection, and "T3 connection (debug)". With no `server_url` set, the app uses `Prefs.DEFAULT_SERVER`, `http://buildbot.tail47515a.ts.net:3847`, which is the REAL server.
- Home settings (`HomeSettingsActivity`): from the home menu, or Settings search. Sections: Hidden apps, Drawer sections, Look (icon size and shape, icon pack, labels, header, home and drawer columns for narrow and wide screens), Notification dots, Search (open the drawer with the keyboard up, contacts in search), then links to Default home app and LoR app settings.

## What it does

- **The grid.** One panel is on screen at a time. The default layout puts the agent page (`t3`) at `[0, 0]`, home's stack at `[1, 0]` (clock header, apps, dock), extra pages to the right, and the drawer at `[1, 1]`. A swipe follows the finger to the next panel that way; up and down wrap. A home press closes what is open and returns to the `start` cell. On a screen 600 dp or wider (the Fold's inner screen), two `half` panels share a screen and `full` panels stand alone.
- **Stacks.** A `clock` block (date or clock, next event, next alarm, weather), an `apps` block (up to 24 apps and folders, in reading order), Android `widget` blocks, and the `dock` (up to 5 apps, the same in every stack). Empty home says "Home is empty."
- **Folders** open full screen over a blurred wallpaper. Their menu has Rename…, Cover app… and Remove from home.
- **Drawer**, reached by a swipe up from home: three views (`sections`, `az`, `used`), with search, the view switch and Jump at the bottom. Search finds apps, settings pages, contacts (with permission), quick answers (calculator, units), and with no match offers the web, the app store or quick send.
- **Agent page**: Today, Needs you (flagged tasks, with Open in LoR), Running, and T3 cards with Approve, Deny, Answer and Open; a Quick send button; a pair card when T3 isn't paired. Its LoR data comes from `GET /device/home` on the server in `server_url`.
- **Long-press menus.** On an app: its shortcuts, App info, Uninstall, Add to or Remove from home, folder and dock, Move to section…, Hide. On empty home: Edit home, Add widget (not while `locked`), Wallpaper, Home settings. Edit home shows "Drag apps to move them. Drop one on another to make a folder." with Remove and Done. Every edit shows an Undo bar.
- **Home press while already on home** opens the quick-send sheet.
- **Set-up card** on a fresh install lists Home app, T3 pairing and LoR reached, until all three hold at once or Skip set-up is tapped, then never again (`setup_done` in `lor.xml`).
- **Broken layout.** If `layout.json` doesn't parse, home keeps the last good layout (`layout.good.json`) and shows a banner, "layout.json has an error. Showing the last good layout." (or "Tap to put it right." with none). Details shows the parser's error, which names the place, and restore moves the bad file to `layout.json.broken`.

## layout.json

It lives at `/data/data/com.spadaval.lor/files/layout.json` (with `layout.good.json`, `layout.json.broken` and `launch-stats.json` beside it). On the debug build:

```
adb -s <emulator> shell run-as com.spadaval.lor cat files/layout.json
adb -s <emulator> shell run-as com.spadaval.lor sh -c 'cat > files/layout.json' < layout.json
```

Home rereads it on resume when its modified time changed, so leave home (open another app) and come back after a write. Version 1 files are converted in memory and become version 2 on the phone's next write. Unknown fields are errors. While `"locked": true`, the phone refuses its own edits to `active` or `layouts`; hand and agent edits of the file still go through. A missing file is written from the default layout on the first read.

## Drive it

1. Build: `node .claude/skills/verify-lor/verify.mjs build`. It runs `android/boat-build.sh` on the boat VM. Never run Gradle on this machine.
2. Install: `node .claude/skills/verify-lor/verify.mjs install`. It runs `adb install -r` of the debug APK on the `T3_Phone_API35` emulator only and sets `server_url` to `http://10.0.2.2:3857`, before and after the install, without clearing data. The test server must be up for anything that reads LoR.
3. Interact only through T3 Code's device tools: `device_open` with hostId `local` and id `T3_Phone_API35`, then the `agent-device` command it returns to open the app, snapshot, tap, type and take screenshots. Use `device_screenshot` to look. Never raw `adb shell input`, and never the physical phones.
4. Logs: `node .claude/skills/verify-lor/verify.mjs logs <out>`.

For anything about width or folding, the missions also used `T3_Fold_API35`. Agent page T3 cards need a paired scratch T3 server (`docs/missions/t3-launcher/plan.md`, "The scratch T3 server"), never the live one.

## Proves it works

- A screenshot of the state the change is about, and the `agent-device` snapshot text that names the elements (labels, menu items, banner text).
- Logcat lines from the tags the code logs: `LorHome` (activity created, configuration changes, "read layout.json", "wrote layout.json (an edit from the phone)", "layout edit refused: …", "undid the last edit", "restored layout.json …", "loaded the app list (N activities)"), `LorSearch`, `LorWidgets`, `LorGrid` (debug), `LorIcons`, `LorLook`, `LorDots`, `LorMotion`, `LorReads` ("read /device/home: ok in N ms") and `LorLive`. No crash in `logcat -b crash`.
- For an edit, a `run-as` read of `files/layout.json` after it, showing exactly the field that should change.
- `docs/missions/t3-launcher/device-checks.md` shows the shape of a check: the device, the server it pointed at, a table of check, result and note, and what was not checked.

## Gotchas

- Never `pm clear` LoR. It resets `server_url` to `Prefs.DEFAULT_SERVER`, the real server, and as the emulator's default home it is restarted the moment it is cleared. To simulate a first run, force-stop LoR and use `run-as` to delete `setup_done` from `shared_prefs/lor.xml` and `files/layout.json*`, keeping `server_url`.
- A cold start was checked with `am force-stop com.spadaval.lor` and then a home press.
- Check `server_url` on any new AVD first: the Fold AVD was once still on the real server.
- `10.0.2.2` once failed minutes after an emulator reboot. The fallback was `adb reverse tcp:<port> tcp:<port>` with `http://127.0.0.1:<port>`.
- The clock block's event and weather, the agent page's Today and Needs you, and the set-up card's LoR row all need the test server (`scripts/ui-server.sh`, port 3857). It has no standing seed; `scripts/ui-check.mjs`'s `tasks` flow makes a flagged task and its `today` flow makes calendar events, but those flows wipe and reseed the test database.
- With the test server stopped, the set-up card's LoR row reads "Can't reach 10.0.2.2:3857." That is the expected failure state, not a bug.
- The emulator's code scanner module never downloads, so pairing by scan can't be checked there; paste a pairing link.
- Home rereads `layout.json` only on resume, so a file written while home is in front shows after you leave and come back.
- `install` writes `server_url` before `adb install -r` as well as after, because the install restarts LoR at once when it is the default home. On 2026-11-08 the emulator's app had no `server_url` at all, so it was on the real server until `install` ran; `doctor`'s "app on emulator" check reports this.
- `agent-device open` can fail with a hint that another session holds the device (an earlier thread's, left open). Follow its advice once (`open <app> --wait 30000`); never reuse or close the other session.
- Settings > Test connection reading "Connected. LoR sees you as this box." proves the app reaches the server in `server_url`.
