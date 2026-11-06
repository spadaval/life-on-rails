# T3 Launcher: device checks

Results of the device checks in [plan.md](plan.md). Screenshots stay in the working thread, not in git.

## Increment 1.1: minimal home, on the emulator

2026-11-05. `T3_Phone_API35` (a plain phone, Android 15, Pixel Launcher as stock home). The first pass found the task-affinity bug below. After the review fixes (the commit after `ffdc121`), every row was run again on a fresh boat build of that tree. The results below are from that run.

| Check | Result | Note |
| --- | --- | --- |
| Settings offers it as the home app | pass | Settings > Default apps > Home app lists "Pixel Launcher, Life on Rails". Choosing it goes straight to the LoR home. |
| Stays default after `adb install -r` of a new build | pass | `cmd role get-role-holders android.app.role.HOME` and `get-default-launcher` still name LoR. |
| Home press opens it | pass | From Settings, home lands on `com.spadaval.lor/.home.HomeActivity` (`dumpsys activity`). |
| Tapping an app opens it | pass | Clock opened `DeskClock`. |
| Still the default after a reboot | pass | `cmd shortcut get-default-launcher` names `HomeActivity` after `adb reboot`, and the device boots to it. The first pass's reboot showed one "System UI isn't responding" dialog (`am_anr` for `com.android.systemui`: "failed to complete startup"). It went away after Wait, and the re-run's reboot had no `am_anr` or `am_crash`. Probably SystemUI starting slowly on a loaded host (inferred, not shown). |
| The LoR icon still opens the tabs | pass, after a fix | At first, tapping "Life on Rails" only brought home back: home and `MainActivity` shared the package's task affinity. Home now has `android:taskAffinity=""`, as Launcher3 does. After that it opens `MainActivity`, and Timer, Sensors, Settings and Dashboard each open. |
| `testDebugUnitTest` | pass | 5 tests, 0 failures (boat). |

Notes:
- The list is white text straight on the wallpaper, so the top rows are hard to read on a light wallpaper. That is fine for a probe; Increment 3 replaces the screen.
- A foldable AVD is cheap to make: the SDK's AVD tooling has the `pixel_9_pro_fold` and `pixel_fold` device profiles, and the API 35 image is installed. One wasn't started here, because a second emulator would load this host. It's worth starting for Increment 3's two-screen work.

## Increment 1.2: the real Fold

Waits for Sumeet.

## Increment 3.3: the agent page, on the emulators

2026-11-05. The LoR test server (`:3857`, two flagged tasks) and the scratch T3 server (`:16021`, Replay threads), on the final boat build. The phone rows were first run by the implementer before its last two fixes, then the approve, question and open-in-T3 rows were run again on the final build. The scratch server had restarted in between, so the approval was a new Replay thread in `approval-required` mode.

| Check | Result | Note |
| --- | --- | --- |
| Phone: the page draws Today, Next, Needs you and the T3 cards | pass | Over a light wallpaper too, after the veil was added. |
| Phone: Approve clears the card | pass | The thread went on running in T3. |
| Phone: a question card opens its choices; Send answer clears it | pass | Send answer stays disabled until an option is chosen. The Replay fixture then asks again, as a new request. |
| Phone: quick send starts a thread in `assistant` | pass | "Started in assistant.", and the thread showed under Running. |
| Phone: a T3 card's icon opens the T3 app at that thread | pass | Full screen. Before the fix the adjacent flag split the screen with the last app used. |
| Phone: a LoR card opens the dashboard | pass | Chrome at `/tasks?id=…`. |
| Fold unfolded: the page | pass | 2076x2152. Content keeps to the left 520 dp. The page dots are centred on the whole screen. |
| Fold unfolded: a T3 card opens the thread beside | partly | T3 opened at the thread in a split pane, but the other pane was the last app used (LoR's Settings), not home. Home can't be in a split. |
| Fold folded: the page, and a T3 card | pass | 1080x2424. T3 opened full screen. |
| Fold: the home process across fold and unfold | pass | Same pid. No crashes in `logcat -b crash`. |

Notes:
- The Fold AVD's LoR app was still on the default (real) server address. It was pointed at `http://10.0.2.2:3857` before these checks. Check this first on any new AVD.
- The T3 app on the Fold AVD was copied from the phone AVD and paired through its Settings > Environments > Add environment, with a token from `node apps/server/src/bin.ts pair --base-dir .t3`. A `t3code://pair` link on a cold start showed "Route not found".

## Increment 4: quick send, on the emulator

2026-11-05. `T3_Phone_API35` against the scratch T3 server (`:16021`; projects `assistant`, `launcher-scratch`, `server`), the LoR app pointed at `http://10.0.2.2:3857`. Each thread was read back from the server's shell.

| Check | Result | Note |
| --- | --- | --- |
| Home press while on home opens the sheet | pass | Over home, with the home key (KEYCODE_HOME). Run again 2026-11-05 for review R1, below. |
| `@launcher-scratch …` from that sheet | pass | Label "To launcher-scratch · named". Thread in `launcher-scratch`, titled without the mention. |
| Agent page's button opens the sheet | pass | "To assistant · inbox". |
| One tap on another project | pass | Tapping `server` showed "To server · picked"; tapping `assistant` back and sending started the thread in `assistant`. |
| Ask tile, over the Settings app | pass | Added with the app's "Add the Ask tile" button. Collapsed the shade, the sheet opened over Settings, the thread started in `assistant`, and Settings was back in front. |
| LoR offered and chosen as the digital assistant | pass | Settings > Default digital assistant app listed "Life on Rails"; the role holder is `com.spadaval.lor`. |
| Assistant over T3's Android app at a `launcher-scratch` thread | partly | The sheet opened with the screenshot (labelled "T3 Code"), but "To assistant · inbox": the app gives no thread link or text (plan, Increment 4 Facts). Invoked with a corner swipe (gesture navigation). |
| Assistant over T3 web in Chrome at a `launcher-scratch` thread | pass | "To launcher-scratch · on screen". The thread started there; its first message carries `Screen - Chrome.jpg` (image/jpeg, 85,547 bytes) in the projection, and the file in the server's attachments directory is the Chrome screen without the sheet. |
| Side key from inside another app | waits | **[real Fold]** |

**Review R1 and R2, 2026-11-05** (`T3_Phone_API35`, gesture navigation on, the APK with Increment 5). Whether a swipe home from another app could pop the sheet, since gesture navigation's recents animation can show home before the HOME intent arrives:

| Check | Result | Note |
| --- | --- | --- |
| Swipe home from Settings, Chrome and Clock | pass | Home, no sheet: a quick fling and a slow 0.9 s swipe from each. `logcat` shows no start of `QuickSendActivity`. |
| Swipe home with the drawer open | pass | The drawer closes, no sheet. |
| Home key on home, at rest | pass | The sheet opens. |
| Swipe home on home, at rest | no sheet | The HOME intent does arrive (`START u0 … HOME`, flags `0x10000000`), but the sheet doesn't open. Most likely home doesn't have window focus during the gesture's transition, so the press counts as an ordinary one. Not a misfire. With gesture navigation, home on home opens the sheet only from the home key. Recheck on the Fold (One UI handles the gesture itself) before deciding whether to loosen the rule; loosening it is what could cause the misfire R1 was about. |
| Rotate the sheet with a half-typed prompt (R2) | pass | Typed "half typed prompt", rotated to landscape: the text was still there. Picked `launcher-scratch`, rotated back: "To launcher-scratch · picked", with the text. |

## Increment 5: launches, on the emulator

2026-11-05. `T3_Phone_API35`, the LoR app pointed at `http://10.0.2.2:3857` (the test server, rebuilt from `/tmp/lor-ui-33`).

| Check | Result | Note |
| --- | --- | --- |
| A launch from search is queued with its source | pass | Drawer, typed "clo", tapped Clock: `{kind: "launch", from: "search", query: "clo", package: "com.google.android.deskclock", …}`, `t_start = t_end`. |
| A launch from home | pass | Life on Rails on the home grid: `from: "home"`, activity `com.spadaval.lor.MainActivity`. |
| Uploaded and stored | pass | Sync now: "2 new on the server". Both rows in `life_on_rails_test` with `kind = 'launch'`. |
| On the day's timeline | pass | `dayTimeline` for today, read directly: `Clock · opened from search "clo"`, `Life on Rails · opened from home`. The Today page draws the timeline only for days gone by. |
| Agent card and icon launches, dock, folder | not run | Covered by `LaunchesTest` and the `launches` db case. |

### Real Fold setup (2026-11-05)

Galaxy Z Fold7 (SM-F966U1), paired over wireless adb at `192.168.1.151`. Installed `main` at `c0f0421` (built on boat), made default home with `cmd role add-role-holder android.app.role.HOME com.spadaval.lor 0`, battery exempt via `dumpsys deviceidle whitelist +com.spadaval.lor` and `appops RUN_ANY_IN_BACKGROUND allow`. Process at setup: pid 21534, started 18:18:52 EST. Check 2 compares against this on 2026-11-06.

## Increment 6: running turns as Live Updates, on the emulator

2026-11-05. `T3_Phone_API36` (Pixel 9 profile, Android 16, the `android-36.1` Google APIs image, already on this host), LoR as the default home, the LoR app pointed at `http://10.0.2.2:3857`. The scratch T3 server (`:16021`, project `launcher-scratch`), turns started with `node apps/server/src/bin.ts thread start --base-dir .t3 --provider devReplay`, so every turn came from another client. Read back with `dumpsys notification --noredact` and screenshots.

| Check | Result | Note |
| --- | --- | --- |
| Settings' Live Updates section | pass | With notifications denied it said "Off: LoR may not post notifications." with Allow notifications; the system prompt's Allow turned it to "On." (`POST_PROMOTED_NOTIFICATIONS` is granted at install.) |
| A running turn posts a Live Update | pass | A `showcase` turn, home in front: one notification, channel `running_turns`, flags `ONGOING_EVENT\|ONLY_ALERT_ONCE\|LOCAL_ONLY\|PROMOTED_ONGOING`, "launcher-scratch · Running". |
| It clears when the turn ends | pass | The `showcase` run took about 6 s; `LorLive` logged 1, then 0 six seconds later, and the notification was gone. |
| The status bar chip | pass, not on home | Over the Settings app: a `question` turn showed the chip "Answer" (`shortCriticalText`), and a running turn the icon alone. On home there was no chip, only the small icon: Android hides an app's own chip while that app is in front, and LoR is home. |
| The promoted notification in the shade | pass | At the top, above the Silent section, though the channel is low importance: "live update question", "launcher-scratch · Waiting for your answer". |
| A turn started while the launcher is in the background | pass | Settings in front: a `showcase` turn showed (screenshot 1.5 s in: the chip with the icon) and cleared when it ended, leaving the waiting question's update. |
| A tap opens the T3 app at the thread | pass | The T3 app copied there earlier was not paired at first and opened at "No environments connected". Paired with Add environment (`http://10.0.2.2:16021` and a code from `t3 pair --base-dir .t3`; a bare `10.0.2.2:16021` says "Pairing URL is invalid."). Then the tap opened "live update question" in `launcher-scratch` with its question. Answering it there cleared the Live Update. |
| The connection in the background, without a foreground service | pass, over 15 minutes | Settings in front from 22:55 to 23:09, screen on, nothing else touching LoR. Then a `question` turn from the CLI posted its Live Update within 2 s, chip "Answer". The same pid (3797) throughout, no foreground service. |
| After the review fixes | pass | The final build installed over it: the waiting question's update came back in the new process (the start sweep cleared the old one), and a new `showcase` turn over Settings showed with its tap target and cleared when it ended. No crashes in `logcat -b crash`, and no tickets or tokens in logcat. |

Notes:
- The home process is `mHomeProcess` with process state HOME, not cached, so the cached-app freezer leaves it alone; that's why the socket lives with no foreground service. An app that isn't home would not get this.
- Doze was not tried: the emulator was plugged in and awake. A day on the Fold is in plan.md's Handed on, item 6.
