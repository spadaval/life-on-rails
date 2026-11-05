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
