---
status: draft
last_changed: "2026-11-10: planning reviews A and B fixed (the down span is read from held status reports, not arrival gaps; the silence message names no cause, is recorded before it is sent and is never retried; a read in flight is abandoned on a network change; People's rule names short codes and keeps organisations; use recording waits as lor-4lj3); created from docs/making-lor-useful.md, which Sumeet agreed in outline the same day (the slice first; new features pause until the current ones are improved; LoR may message him itself over WhatsApp; the project tour is weekly), and from his real-Fold answers of that day"
---

# Sizing

- **Complexity: medium.** New moving parts (a status observation, a watch job, the sync split in two), across the phone, the server and the agent's instructions. No new subsystem.
- **Value: high.** The outcome needs it: data that silently fails to arrive is a current hazard for everything LoR is meant to do, and the wall's one sentence is the thing Sumeet looks at.
- **Risk: medium.** It reaches the owner (a message LoR sends itself) and the phone's background behaviour cannot be fully checked before merge. Both show quickly and can be undone.

Worth it against the cheaper path, which is switching on Android's Always-on VPN by hand and changing nothing: that would not tell anyone when the link fails again, and it leaves the wall line and the numbers as they are.

# Shape

- **Three increments, riskiest first.**
  1. *The phone's link:* status reports, collection without a network, retry on a network change, the watch and its message.
  2. *One line, honest numbers:* the pinned action, quiet flags, the wall's large type, and the fixes to Today, Goals, the wall and People.
  3. *Use:* `use` observations from the web, the wall and the phone, and `lor use`. It waits in backlog as lor-4lj3 with a provisional goal; its issues are written when increment 1 has merged, because its phone half touches the same files.
- **The route is decided from data, not now.** Sumeet's answer on 10 November to why Tailscale is off when it is off was "both, or not sure". So increment 1 reports the VPN's state with every sync, and whether the phone should use Always-on VPN or reach LoR without a VPN is his decision after a few days of that data (lor-tr2s, outside this mission). No public endpoint is added here.
- **No new table.** A status report and a use event are observations, queued and uploaded like the rest. "Last heard" is the newest `ingested_at` for the phone.
- **The pin is the wall line.** `wall_pin` and `lor wall pin` already put a sentence on the wall and on `/device/home`. The morning run uses them; nothing new stores the short line.
- **Unknown is not zero.** Where nothing was measured the screens say so in words. No score, target or cadence changes.
- **Simple wins.** One message, one threshold, no settings for either. If the message proves noisy the threshold changes in code.

# Design

**Status reports (phone).** Each sync pass queues one observation, `source=android`, `payload.kind="status"`, `t_start=t_end=` the pass time, id derived from the device and that time: `{vpn: bool, network: "wifi"|"cellular"|"none", queue_rows, queue_oldest_s, last_upload_error, battery_unrestricted: bool, background_restricted: bool, permissions: {<sensor>: bool}, app_version}`. `vpn` is whether the active network has the VPN transport (`ConnectivityManager`). It goes through the existing queue (`data/Queue.kt`), so reports made while the link is down arrive when it returns and give the timeline. `observation.kind` is `payload->>'kind'`, so no migration.

**Reading the link from reports (server).** A report's `t_start` is when it was made and its `ingested_at` is when it arrived, so an outage leaves no gap in arrivals once the queue has drained. A report that arrived more than 5 minutes after it was made was held: the link was down from the first held report's `t_start` until that batch arrived. A stretch of more than 45 minutes with no report made in it, by `t_start`, means the app was not collecting. `lor status` and `/status` show both kinds of span for the last 24 hours, the phone's last arrival, and the share of reports with `vpn: true`.

**Collection without a network (phone).** `sync/SyncWorker.kt` becomes two pieces of work. Collect: every 15 minutes with no network constraint, runs the collectors and queues the status report. Upload: unique work that needs a connection, enqueued after each collect and by the network callback, draining the queue as `sync/Sync.kt` does now. A row the server refuses stays in the queue marked refused with the server's reason, is not sent again by itself, and is counted on the Sensors screen with a Retry. That replaces dropping it (`Sync.kt`, "A rejected observation will never be accepted").

**Retry on a network change (phone).** `LorApp` registers one default-network callback for the process. When a network becomes available, or the active one gains the VPN transport, it enqueues the upload, resets `t3/T3Connection.kt`'s backoff and connects now (today it waits out a backoff of up to 30 s), and asks `net/LorReads.kt` to read now (today once a minute). A read already in flight is abandoned first: `LorReads` holds one mutex around each read and `net/LorClient.kt` blocks for up to 15 s to connect and 20 s to read, which cancelling a coroutine does not interrupt. So the reader keeps the connection of the read in flight and closes it (`HttpURLConnection.disconnect()`) when the callback fires, the new read starts at once, and the abandoned read's failure is not shown. It is one callback and three calls; no polling is added.

**The watch (server).** A job `phone_watch` in `src/server/jobs/registry.ts`, every 300 s. The phone is the device with the newest `source=android` observation. It is quiet when its newest `ingested_at` is more than 90 minutes old. On the Fold's healthy days (6 and 9 November) batches were 15 minutes apart at the median, 28 at the 95th percentile and 53 at most, over 142 batches. When the phone is quiet, the time is inside `LOR_WAKE_START` to `LOR_WAKE_END`, this silence has not been reported, and no silence message went out in the last 24 hours, it reports the silence. It first writes a `source=system` observation (`payload.kind="phone_link"`, `state: "quiet"`, `messaged: true|false`), and only then sends one message through `sendMessage` in `src/server/channel/channel.ts` (thread `lor:phone`), when `channelConfigured()` says the channel is on, catching a refusal. The message says when data stopped. It names no cause, because none is known until reports arrive again. A send that fails is logged and not tried again for that silence, so a timeout after delivery can never produce a second message; the daily cap counts `quiet` observations with `messaged: true`. The return is recorded as `state: "back"`. The job stands alone in the registry and is not one of the channel's jobs, so it records with the channel off. The spec's "Who is who" and "The channel" gain this one message that LoR sends without the agent.

**Quiet flags.** `lor task flag <id> "<why>" --quiet`, and `--flag "<why>" --quiet` on create, set `flagged_at` and `notified_at` together, so `sendFlags` skips the task: the brief is the message. One option through `cli/lor.mjs`, `src/server/api/v1.ts` and the flag function in `src/server/intents.ts`.

**The morning run.** The `agent` page's brief section changes: after writing the brief, pin its action in 60 characters or fewer with `lor wall pin "<text>" --until 22:00`; flag each decision that waits on him with `--quiet`. A quiet day pins nothing. Sumeet agreed the content on 10 November; the orchestrator makes the edit with `lor page update agent`.

**The wall (`src/server/trmnl/frame.ts`).** The one thing's type size steps with its length, so a line of 60 characters or fewer fills the box in two lines at most; longer text keeps today's size and clamp. "late" loses its bold. A goal with no committed time in the week reads "no data" instead of `0 / Nh` with an empty meter.

**Today, Goals, People (web).**
- `src/routes/index.tsx`: before the waking day starts the hero says when it starts, not the whole day as remaining (lines 137 to 145). A past day's figure is labelled time, with unlabelled phone and place time named as such (lines 99 to 103).
- Goals this week on Today, and `/goals`: a goal with no committed time in the window reads "no data yet", not `0 / Nh`.
- `src/server/people.ts`, `important()` (line 121): a contact is not important merely for not coming from Google. An entity known only by short codes (phone numbers of six digits or fewer) is not important for recent contact either, so carrier and bank senders leave the default list; it still shows with a page, a cadence or priority, and under All. Places are left out of the people lists. Organisations stay as they are. Overdue days are drawn in the secondary ink, not the warning colour.
- `scripts/ui-check.mjs`'s today and goals flows are brought up to date first (lor-mxrc), since these changes are checked through them.

**Use (increment 3, provisional).** `payload.kind="use"` observations: from the web, one per page opened on the owner's listener (`:3848`), sent by a small beacon on route change; from the wall, one per middle tap in `src/server/trmnl/device.ts`; from the phone, one per agent page or panel action and quick send, through the launcher's existing launch queue. `lor use [--days N]` and `/api/v1/use` count them by surface and day, with replies to a brief counted from channel messages.

# Derisking

The riskiest assumption is that the upload gaps are the link (Tailscale down) and not Android stopping the app. The evidence for it: from 7 to 9 November the sync job kept running and recording locations while nothing uploaded. Status reports are the first item, so within days of the build reaching the Fold the timeline says which it is. If the app was being stopped, the reports themselves have gaps, and the next step is battery restrictions on One UI, not the route.

The second is that a network callback fires when Tailscale's tunnel comes up on One UI. The emulator cannot run Tailscale, so the stand-in before merge is the emulator's network switched off and on, and the real check is a loose end on the Fold (lor-4rdv).

# Risks and Unknowns

- **The message could nag.** Phone data arrived on 10 of the 28 days to 9 November, so until the route is decided this message may come on most days. It is capped at one a day, it names no cause it cannot know, and that outcome is the evidence lor-tr2s needs. If it bites before the decision, the orchestrator may raise the threshold but not lift the cap.
- **The process may not be alive when the tunnel comes up.** The callback lives in the app's process. LoR is the home app, so it usually is; when it is not, the next 15-minute collect enqueues the upload.
- **Refused rows could pile up.** They are counted on Sensors. If the count grows, the cause is a bug to fix, not a reason to drop them.
- **`ui-check`'s flows are stale** (lor-mxrc). If bringing them up to date is larger than it looks, the web fixes are checked with the verify skill's `screen` directly and lor-mxrc stays its own item.
