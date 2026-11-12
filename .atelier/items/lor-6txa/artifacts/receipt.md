# Receipt: verify-lor on the Mac, 2026-11-11

What ran, from this branch (bfe371e plus the uncommitted change), against the test server on buildbot:

1. `verify.mjs doctor` (default host `mac`): every surface ready, with `device host`, `emulator` and `tunnel 3857` ok on `mac`. Saved as `doctor.json`.
2. `verify.mjs build`: `BUILD SUCCESSFUL in 2m 5s` on boat, APK sha256 `d5fc326c4b10...`.
3. `verify.mjs install`: copied the APK to the Mac and installed it on `T3_Phone_API35` (`emulator-5554`), `server_url` = `http://10.0.2.2:3857` on the first attempt.
4. `device_open` (hostId `mac`), then `agent-device open com.spadaval.lor`: the timer card lists "Ship LoR", an intent in `life_on_rails_test`, so the app reads the test server through `lor-mac-tunnel`. Screenshot `mac-timer-screen.png` (here) and the `agent-device` snapshot.
5. `verify.mjs logs`: the app's logcat (`SyncWorker` result SUCCESS) and an empty crash buffer.

A failure found on the way: on the Mac's platform-tools (37.0.1), `adb exec-in` exits 0 and writes nothing, so the first `install` ended with "the app kept overwriting server_url". The prefs write now uses `adb shell` with stdin.

The full evidence is in `~/.local/share/lor-evidence/lor-6txa/` on buildbot:

```
e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855  crash-logcat.txt
44e5e9b3bb8d8276188b2ed4aebcab92656ed22e001feac09deeb8328c6a093b  doctor.json
5e722145e54515d5864205e717668c654eb95ef650e1762c2cdcb92814fc7cd5  launcher-logcat.txt
8b2cff7331e0a407f1da45170a97bab23e3a8f0d0cb8e86b6909733ee7c0a6ac  mac-timer-screen.png
45802e1ffdd18ce7be51b259f282d2a3a3c59f4ead8d7381c9eb4f97ef6b87f2  mac-timer-snapshot.txt
ed320ba243892024832b0bc4a8060c3796af4834c70081d40e93f74caaecc5f9  server.log
```
