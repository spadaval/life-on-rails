---
schema: atelier.mission
schema_version: 3
id: lor-l9a7
title: 'LoR''s chain works end to end: the phone''s link, one line on the wall, honest numbers'
status: draft
labels:
- chain
lead: null
ready: null
created_at: 2026-11-10T06:11:15.540367025Z
---

## Why

LoR's chain from sensing to saying something useful is broken at links nobody sees. The phone's data reached LoR on 10 of the 28 days to 9 November, and LoR has no check for a silent phone. The Fold kept collecting from 7 to 9 November while nothing uploaded for 55 hours, and Sumeet says the LoR and T3 panels take "an absolute eternity" to connect once Tailscale is switched on. The brief's first line runs 112 to 208 characters, so the wall cuts it off before the ask. Today says "14h remaining" at midnight and "23h recorded" for unlabelled time at home, and five of seven goals read "0 / Nh" where the truth is that nothing was measured. `docs/making-lor-useful.md` has the full account; this mission is its first slice.

## Outcome

After this mission, on the surfaces LoR already has:

- **The phone's link is seen and heals itself.** The phone collects with no network, uploads the moment one returns, and its panels reconnect within seconds of the tunnel coming up. Every sync reports the phone's state, including whether a VPN is up, so LoR can show when the link was down and why.
- **LoR says so when the phone goes quiet.** After 90 minutes of silence in waking hours it sends one WhatsApp message, at most one a day, and records when data returns.
- **The one thing fits the wall.** The morning run pins the day's action in 60 characters or fewer. The wall and the phone's home show it whole, in large type. Decisions waiting on Sumeet are under "Needs you", without a second message for each.
- **The numbers are honest.** Today, Goals, the wall and People stop showing a figure that is wrong or that reads as failure where nothing was measured.
- **LoR records its own use,** and `lor use` prints a week of it.

## Success criteria

- link-heals: On `T3_Phone_API35` against the test server, with the emulator's network off while the app collects for 30 minutes and then on, the queued observations and status rows reach the server within one minute of the network returning, and `lor status` on the test server shows the phone's link as down for that span, worked out from status reports that arrived more than five minutes after they were made. Shown by the verify skill's launcher path, with its log and the API response as receipts.
- one-line: On the test server, a brief written and pinned the way the `agent` page then instructs gives a wall Today frame whose one thing is at most 60 characters and is drawn whole, and Today's "Needs you" lists each decision the brief names, with no `lor:flag` message sent for them. Shown by a rendered frame and the API response.

## Scope

In: `android/` (sync, the queue, `LorApp`, the Sensors screen's counts), `src/server/` (device ingest, jobs, channel, intents, status, trmnl, people), `src/routes/index.tsx` and the goals route, `cli/lor.mjs`, `skill/SKILL.md`, `scripts/ui-check.mjs`, the spec, and the `agent` page's brief section.

Out: a route to LoR without a VPN; set-up changes on the phone; the Sensors screen's design; inferred time (spike lor-tijy); the launcher's redesign items (lor-qsi6, lor-hybv, lor-dx87, lor-2fkr) and small launcher bugs (lor-o8gd, lor-i83p, lor-swcg), which are loose backlog; the mail runner, already in hand on the `mail-runner` branch; a rebuild of Today or the time tracking page.

Environment: the test server (`:3857`, `life_on_rails_test`) and `T3_Phone_API35` through the verify skill; Android builds on boat. The real Fold only in items after merge, when Sumeet says to install.

## Authority

The orchestrator may change the increments' order and issues; choose the threshold, the message's wording, type sizes, names and payload fields within this Design; write increment 3's issues; and edit the `agent` page's brief section as this strategy says. These come back to Sumeet: installing on his phone; any second kind of message from LoR, or more than one a day; any endpoint reachable without Tailscale; and anything the Scope lists as out.
