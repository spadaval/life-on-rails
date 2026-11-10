---
created_at: "2026-11-10T06:11:16.059957388+00:00"
id: "lor-tijy"
issue_type: "spike"
labels:
- "chain"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Can a model account for a recorded day? Five days, read by Sumeet"
updated_at: "2026-11-10T06:11:16.059957388+00:00"
---

## Goal

Learn whether inferring where a day went is right enough and cheap enough to build into the nightly run, before building it. For five recorded days (5 to 9 November), an agent builds one evidence packet per day and writes an account of the waking day in spans.

## Done when

- [ ] For each of the five days there is an evidence packet (phone sessions, places, sleep and exercise, calendar, calls, the times Sumeet wrote to an agent in T3 and in which project, commits) and an account: spans with a category (a goal, or Work, Sleep, Leisure, Chores and admin, Travel), the evidence for each, and whether it is observed or a guess. Unknown is what is left.
- [ ] The accounts are on pages Sumeet can read (`drafts/time-trial/<date>`), with nothing written to `time_block` or `commitment`.
- [ ] A report gives, per day: the share of waking hours with a named category, the share marked as a guess, and the tokens the account took.
- [ ] The report says what the packet could not see, and names the cheapest missing evidence.

## Scope

A script outside `src/` and pages under `drafts/`. This is a spike: nothing merges into the product, and Atelier's review applies to the report only.

## Context

- `docs/making-lor-useful.md`, "Yesterday, accounted for, as a trial", and the contract it gives for inferred time if this goes ahead.
- T3's store is `~/.t3/userdata/statev2.sqlite` (read-only; `orchestration_v2_projection_messages`, role `user`).
- The work laptop is invisible, a place does not prove an activity, and an agent's commits are not his hours. Say where an account leans on any of these.
- Message bodies and `therapy/` pages are not read.
