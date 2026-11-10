---
schema: atelier.review
schema_version: 2
id: lor-4vs0
title: approval on lor-l9a7 @efb3a93d
target: lor-l9a7
identity: planreview-c
commit: efb3a93d6aecb748ec9cb2b05cd28939cccda194
created_at: 2026-11-10T06:26:32.429465988Z
event:
  event: approval
---

Independent fix-only re-review of 4d000ac..efb3a93d6aecb748ec9cb2b05cd28939cccda194, against all nine original findings on planning commit 5dfae08. The one author rebuttal supplied was independently checked. No plan edits and no subagents.

Verdict: merge. This planning fix makes the existing sensing-to-wall slice executable without expanding its outcome.
Sizes: complexity low (planning corrections using existing observations and transport); value high (removes acceptance and notification contract failures); risk low (records and strategy only, no runtime code).
Contract: pass for the fix diff; strategy, mission criteria and affected Done when lines agree on the revised contracts.
Fitness: pass for planning; no new FIX NOW finding.

Original findings:
- lor-dtrd: resolved. Strategy Design, mission link-heals and lor-gn1w now use report t_start versus ingested_at, not missing reports after backfill.
- lor-guog: resolved. lor-gn1w explicitly distinguishes delayed-report link spans from more than 45 minutes of missing collection reports and requires database cases for regular reports held two hours and a true collection gap.
- lor-i6bb: resolved. lor-4lj3 is a mission-linked backlog increment with a provisional Goal, Done when, Unknowns and a blocker on lor-sgl4, transitively lor-f86f and lor-gn1w.
- lor-gq72: resolved. Both watch specifications say only when data stopped, name no unsupported cause and retain the daily cap.
- lor-6y4m: resolved. Watch is a standalone registry job, records with channel off, skips sendMessage then, catches refusal and requires an ok run.
- lor-fxlg: resolved. Strategy and lor-sgl4 require abandoning the in-flight transport, immediate replacement, suppression of the abandoned failure, a blocked-client JVM case and a timed emulator in-flight restoration case. Read LorReads mutex, LorApp IO wrappers and LorClient 15s connect/20s read timeouts; this is a required implementation and acceptance test, not a claim that the current client already supports cancellation.
- lor-2hnd: resolved. Durable quiet observation precedes sending; messaged=true attempts count toward the 24h cap even on failure, never retry that silence, and a delivered-then-thrown port must show one attempt across three runs. Existing acceptance also covers second silence within the cap and return.
- lor-n6f1: resolved. Recent contact no longer rescues an entity identified only by short phone codes; page/cadence/priority and All remain. Places are excluded throughout, organisations explicitly preserved. Required database cases cover these branches; Acme remains supported in screen people.
- lor-46b0: author rebuttal confirmed; disposition no_action recorded separately after reading me history and the two relevant current-page lines. Versions 15-19 on 2026-11-10 corrected current/retired phone and WhatsApp visibility.

Proof: read git show efb3a93, the strategy, the full mission and all eight item records, both specified procedures, product spec and making-lor-useful requirements. Cross-checked src/server/channel/channel.ts (channelConfigured, sendMessage, send before channel observation), jobs/registry.ts and scheduler.ts (conditional channel jobs and interval retry), observations.ts and db/schema.ts (ingested_at defaultNow preserved on upsert), people.ts (important and filters), scripts/ui-check.mjs (Acme org selected under All and cadence set), and Android LorReads.kt, LorClient.kt, LorApp.kt and sync/Sync.kt (queued rows survive upload failure). SELECT-only live aggregates reproduced 142 healthy-day batches on 6/9 November, median 15.0405 minutes, p95 27.9192 and max 53.4302; arrivals on 10 of 28 local days through 9 November; one recently contacted person with exclusively short-code identifiers. No message bodies or therapy pages read.

Findings: none new.
Open questions: none blocking this diff; increment 3 implementation details remain provisional as designed.
Residual risk and tests not run: no runtime source changes, so no builds, tests or emulator runs were required for this planning review. Actual transport disconnect timing, network callback behaviour and end-to-end recovery remain future implementation checks; real Fold tunnel behaviour remains the after-merge lor-4rdv check. Human mission approval is still separate and outstanding.
