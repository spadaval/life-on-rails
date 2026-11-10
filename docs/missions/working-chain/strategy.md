---
status: draft
last_changed: "2026-11-10: created from docs/making-lor-useful.md, which Sumeet agreed in outline the same day (the slice first; new features pause until the current ones are improved; LoR may message him itself over WhatsApp; the project tour is weekly), and from his real-Fold answers of that day"
---

# Why

LoR's chain from sensing to saying something useful is broken at links nobody sees. The phone's data reached LoR on 10 of the 28 days to 9 November, and LoR has no check for a silent phone. The Fold kept collecting from 7 to 9 November while nothing uploaded for 55 hours, and Sumeet says the LoR and T3 panels take "an absolute eternity" to connect once Tailscale is switched on. The brief's first line runs 112 to 208 characters, so the wall cuts it off before the ask. Today says "14h remaining" at midnight and "23h recorded" for unlabelled time at home, and five of seven goals read "0 / Nh" where the truth is that nothing was measured. `docs/making-lor-useful.md` has the full account; this mission is its first slice.

# Outcome

After this mission, on the surfaces LoR already has:

- **The phone's link is seen and heals itself.** The phone collects with no network, uploads the moment one returns, and its panels reconnect within seconds of the tunnel coming up. Every sync reports the phone's state, including whether a VPN is up, so LoR can show when the link was down and why.
- **LoR says so when the phone goes quiet.** After 90 minutes of silence in waking hours it sends one WhatsApp message, at most one a day, and records when data returns.
- **The one thing fits the wall.** The morning run pins the day's action in 60 characters or fewer. The wall and the phone's home show it whole, in large type. Decisions waiting on Sumeet are under "Needs you", without a second message for each.
- **The numbers are honest.** Today, Goals, the wall and People stop showing a figure that is wrong or that reads as failure where nothing was measured.
- **LoR records its own use,** and `lor use` prints a week of it.

# Success Criteria

- link-heals: On `T3_Phone_API35` against the test server, with the emulator's network off while the app collects for 30 minutes and then on, the queued observations and status rows reach the server within one minute of the network returning, and `lor status` on the test server shows the phone's link as down for that span. Shown by the verify skill's launcher path, with its log and the API response as receipts.
- one-line: On the test server, a brief written and pinned the way the `agent` page then instructs gives a wall Today frame whose one thing is at most 60 characters and is drawn whole, and Today's "Needs you" lists each decision the brief names, with no `lor:flag` message sent for them. Shown by a rendered frame and the API response.

# Sizing

- **Complexity: medium.** New moving parts (a status observation, a watch job, the sync split in two), across the phone, the server and the agent's instructions. No new subsystem.
- **Value: high.** The outcome needs it: data that silently fails to arrive is a current hazard for everything LoR is meant to do, and the wall's one sentence is the thing Sumeet looks at.
- **Risk: medium.** It reaches the owner (a message LoR sends itself) and the phone's background behaviour cannot be fully checked before merge. Both show quickly and can be undone.

Worth it against the cheaper path, which is switching on Android's Always-on VPN by hand and changing nothing: that would not tell anyone when the link fails again, and it leaves the wall line and the numbers as they are.

# Shape

- **Three increments, riskiest first.**
  1. *The phone's link:* status reports, collection without a network, retry on a network change, the watch and its message.
  2. *One line, honest numbers:* the pinned action, quiet flags, the wall's large type, and the fixes to Today, Goals, the wall and People.
  3. *Use:* `use` observations from the web, the wall and the phone, and `lor use`. Its issues are written when increment 1 has merged, because its phone half touches the same files.
- **The route is decided from data, not now.** Sumeet's answer on 10 November to why Tailscale is off when it is off was "both, or not sure". So increment 1 reports the VPN's state with every sync, and whether the phone should use Always-on VPN or reach LoR without a VPN is his decision after a few days of that data (lor-tr2s, outside this mission). No public endpoint is added here.
- **No new table.** A status report and a use event are observations, queued and uploaded like the rest. "Last heard" is the newest `ingested_at` for the phone.
- **The pin is the wall line.** `wall_pin` and `lor wall pin` already put a sentence on the wall and on `/device/home`. The morning run uses them; nothing new stores the short line.
- **Unknown is not zero.** Where nothing was measured the screens say so in words. No score, target or cadence changes.
- **Simple wins.** One message, one threshold, no settings for either. If the message proves noisy the threshold changes in code.

# Design

**Status reports (phone).** Each sync pass queues one observation, `source=android`, `payload.kind="status"`, `t_start=t_end=` the pass time, id derived from the device and that time: `{vpn: bool, network: "wifi"|"cellular"|"none", queue_rows, queue_oldest_s, last_upload_error, battery_unrestricted: bool, background_restricted: bool, permissions: {<sensor>: bool}, app_version}`. `vpn` is whether the active network has the VPN transport (`ConnectivityManager`). It goes through the existing queue (`data/Queue.kt`), so reports made while the link is down arrive when it returns and give the timeline. `observation.kind` is `payload->>'kind'`, so no migration.

**Collection without a network (phone).** `sync/SyncWorker.kt` becomes two pieces of work. Collect: every 15 minutes with no network constraint, runs the collectors and queues the status report. Upload: unique work that needs a connection, enqueued after each collect and by the network callback, draining the queue as `sync/Sync.kt` does now. A row the server refuses stays in the queue marked refused with the server's reason, is not sent again by itself, and is counted on the Sensors screen with a Retry. That replaces dropping it (`Sync.kt`, "A rejected observation will never be accepted").

**Retry on a network change (phone).** `LorApp` registers one default-network callback for the process. When a network becomes available, or the active one gains the VPN transport, it enqueues the upload, resets `t3/T3Connection.kt`'s backoff and connects now (today it waits out a backoff of up to 30 s), and asks `net/LorReads.kt` to read now (today once a minute). It is one callback and three calls; no polling is added.

**The watch (server).** A job `phone_watch` in `src/server/jobs/registry.ts`, every 300 s. The phone is the device with the newest `source=android` observation. It is quiet when its newest `ingested_at` is more than 90 minutes old. On healthy days the Fold's batches were 15 minutes apart at the median, 28 at the 95th percentile and 53 at most, over 148 batches. When the phone is quiet, the time is inside `LOR_WAKE_START` to `LOR_WAKE_END`, this silence has not been reported, and no silence message went out in the last 24 hours, it sends one message through `sendMessage` in `src/server/channel/channel.ts` (thread `lor:phone`): when data stopped, and to open Tailscale on the phone. It records the silence and the return as `source=system` observations (`payload.kind="phone_link"`, `state: "quiet"|"back"`), which is also how it knows what it has reported. `lor status` and `/status` show the phone's last arrival and, from status reports, the share of the last 24 hours the VPN was up. The spec's "Who is who" and "The channel" gain this one message that LoR sends without the agent.

**Quiet flags.** `lor task flag <id> "<why>" --quiet`, and `--flag "<why>" --quiet` on create, set `flagged_at` and `notified_at` together, so `sendFlags` skips the task: the brief is the message. One option through `cli/lor.mjs`, `src/server/api/v1.ts` and the flag function in `src/server/intents.ts`.

**The morning run.** The `agent` page's brief section changes: after writing the brief, pin its action in 60 characters or fewer with `lor wall pin "<text>" --until 22:00`; flag each decision that waits on him with `--quiet`. A quiet day pins nothing. Sumeet agreed the content on 10 November; the orchestrator makes the edit with `lor page update agent`.

**The wall (`src/server/trmnl/frame.ts`).** The one thing's type size steps with its length, so a line of 60 characters or fewer fills the box in two lines at most; longer text keeps today's size and clamp. "late" loses its bold. A goal with no committed time in the week reads "no data" instead of `0 / Nh` with an empty meter.

**Today, Goals, People (web).**
- `src/routes/index.tsx`: before the waking day starts the hero says when it starts, not the whole day as remaining (lines 137 to 145). A past day's figure is labelled time, with unlabelled phone and place time named as such (lines 99 to 103).
- Goals this week on Today, and `/goals`: a goal with no committed time in the window reads "no data yet", not `0 / Nh`.
- `src/server/people.ts`, `important()` (line 121): a contact is not important merely for not coming from Google, and places and organisations are not listed among people. Overdue days are drawn in the secondary ink, not the warning colour.
- `scripts/ui-check.mjs`'s today and goals flows are brought up to date first (lor-mxrc), since these changes are checked through them.

**Use (increment 3, provisional).** `payload.kind="use"` observations: from the web, one per page opened on the owner's listener (`:3848`), sent by a small beacon on route change; from the wall, one per middle tap in `src/server/trmnl/device.ts`; from the phone, one per agent page or panel action and quick send, through the launcher's existing launch queue. `lor use [--days N]` and `/api/v1/use` count them by surface and day, with replies to a brief counted from channel messages.

# Derisking

The riskiest assumption is that the upload gaps are the link (Tailscale down) and not Android stopping the app. The evidence for it: from 7 to 9 November the sync job kept running and recording locations while nothing uploaded. Status reports are the first item, so within days of the build reaching the Fold the timeline says which it is. If the app was being stopped, the reports themselves have gaps, and the next step is battery restrictions on One UI, not the route.

The second is that a network callback fires when Tailscale's tunnel comes up on One UI. The emulator cannot run Tailscale, so the stand-in before merge is the emulator's network switched off and on, and the real check is a loose end on the Fold (lor-4rdv).

# Scope

In: `android/` (sync, the queue, `LorApp`, the Sensors screen's counts), `src/server/` (device ingest, jobs, channel, intents, status, trmnl, people), `src/routes/index.tsx` and the goals route, `cli/lor.mjs`, `skill/SKILL.md`, `scripts/ui-check.mjs`, the spec, and the `agent` page's brief section.

Out: a route to LoR without a VPN; set-up changes on the phone; the Sensors screen's design; inferred time (spike lor-tijy); the launcher's redesign items (lor-qsi6, lor-hybv, lor-dx87, lor-2fkr) and small launcher bugs (lor-o8gd, lor-i83p, lor-swcg), which are loose backlog; the mail runner, already in hand on the `mail-runner` branch; a rebuild of Today or the time tracking page.

Environment: the test server (`:3857`, `life_on_rails_test`) and `T3_Phone_API35` through the verify skill; Android builds on boat. The real Fold only in items after merge, when Sumeet says to install.

# Authority

The orchestrator may change the increments' order and issues; choose the threshold, the message's wording, type sizes, names and payload fields within this Design; write increment 3's issues; and edit the `agent` page's brief section as this strategy says. These come back to Sumeet: installing on his phone; any second kind of message from LoR, or more than one a day; any endpoint reachable without Tailscale; and anything the Scope lists as out.

# Risks and Unknowns

- **The message could nag.** If Tailscale is off most days, the message arrives most days. It is capped at one a day, and that outcome is the evidence the route decision needs. If it bites before the decision, the orchestrator raises the threshold.
- **The process may not be alive when the tunnel comes up.** The callback lives in the app's process. LoR is the home app, so it usually is; when it is not, the next 15-minute collect enqueues the upload.
- **Refused rows could pile up.** They are counted on Sensors. If the count grows, the cause is a bug to fix, not a reason to drop them.
- **`ui-check`'s flows are stale** (lor-mxrc). If bringing them up to date is larger than it looks, the web fixes are checked with the verify skill's `screen` directly and lor-mxrc stays its own item.
