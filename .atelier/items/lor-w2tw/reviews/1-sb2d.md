---
schema: atelier.review
schema_version: 3
round: 1
commit: 5be6dafd6c598055601135718db64cd07b6020ec
reviewers:
- thread:delegated-task:command%3Amcp%3Af4b4fa80-c34c-4f0e-9396-417c517549aa%3Adelegate-task%3Af7cba705-62f9-4527-b7de-fb91e9f5877a
model: gpt-6.1-sol
verdict: changes
created_at: 2026-11-26T00:06:01.711573941Z
---

## Summary

Complexity: low. The change repairs two existing browser flows and their feature maps, with no product change or new subsystem.

Value: high. These flows are the regression checks for the Today and Goals work that follows.

Risk of merging: low. Failures are visible in the verification command and the change is easy to revert; the problems below are false failures under supported test configuration.

The change is worth its size. Read the full diff against main, the two routes, IntentPicker, the clock and local-time helpers, the server's env loading, and verify-lor's launch and screen commands. `git diff --check main...5be6dafd -- . ':!.atelier'` passes. Built the isolated copy with `verify.mjs up` and ran `verify.mjs screen today` and `screen goals`: both pass at 1500×950 with no browser errors. The assertions cover the required interactions, including exact filtered task titles, Did it, selected rows, the editor, timer pause accounting and block relabelling. The documentation matches those flows.

Independently reproduced the Report's deliberately wrong expectations by changing only the temporary copy's predicates: expecting Meetings after relabelling fails Today, and expecting Book flights in the filtered list fails Goals. Restored the copy and reran Today successfully. Evidence is under `/tmp/lor-review-w2tw/`: `today`, `goals`, `today-negative`, `goals-negative`, and `today-restored`.

Also exercised the documented fixed-date configuration and a valid quoted timezone env entry, which expose the two problems below. Their logs are in `today-fake-date/today.log` and `today-quoted-tz/today.log`. No product source was changed. No Android or production-service checks are relevant to this scope, and I did not run them. The execution harness reaped the background server after `up`, so the screen runs used the same built copy's `scripts/serve.mjs` in a foreground tool session with the test database, port 3857, and jobs, channel and other listeners disabled.

The reported picker overlap is real: independently reproduced a mouse-click timeout and confirmed the lanes' hour row is the element at the result button's center. Filed it separately as lor-ug9r. Using the supported Enter action is reasonable for this item's explicit no-product-change scope.

## Finding 1: Use the fixed server date for Today's fixtures and database assertions
Recommendation: fix now

`scripts/ui-check.mjs:39` derives fixture dates and the `blocks()` query window from Postgres `now()`, but the new Today flow uses `?at=15:30`, `16:00` and `16:10` (`scripts/ui-check.mjs:372` onward). `fixedClock` resolves those time-only values against `LOR_FAKE_NOW` when the server has one. Launching the built test server with the skill's documented `LOR_FAKE_NOW=2026-11-17T15:00` on local 2026-11-25 therefore seeds events and queries blocks on November 25 while the timer writes November 17.

Reproduced: `screen today` reaches Pause, then fails `expected an open timer block from 15:30 for Ship Life on Rails, paused at 16:00; found` with an empty result. SQL confirms the correctly paused timer is actually at `2026-11-17 15:30`, paused at `2026-11-17 16:00`. This is a failure of the check against a correct page under a supported verify-lor setting. Resolve one local date consistently for the fixtures, browser clock and SQL assertions, including when the fixed date comes from the server's env file. Preserve local-day arithmetic rather than adding elapsed hours across day boundaries.

## Finding 2: Parse the timezone env entry with the server's dotenv rules
Recommendation: fix now

`scripts/ui-check.mjs:35-36` reads the raw text after `LOR_TZ=` and retains surrounding quotes. The server uses `process.loadEnvFile`, which removes those quotes. With a valid env file containing `LOR_TZ="America/New_York"`, the server uses America/New_York but the new flow inserts the double quotes into its SQL timezone name.

Reproduced with a temporary env file containing only that entry: `LOR_ENV_FILE=/tmp/lor-review-w2tw/quoted-timezone.env node .claude/skills/verify-lor/verify.mjs screen today /tmp/lor-review-w2tw/today-quoted-tz` fails immediately with `time zone ""America/New_York"" not recognized`. Node's `parseEnv` resolves the same file to `America/New_York`. Use Node's env parser/loading behavior with the same environment precedence as `scripts/serve.mjs`; the current claim that this regex reads the setting as the server does is incorrect.
