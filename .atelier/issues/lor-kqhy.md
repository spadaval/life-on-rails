---
created_at: "2026-11-07T03:15:37.674481341+00:00"
id: "lor-kqhy"
issue_type: "decision"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T03:15:54.463496866+00:00"
status: "done"
title: "Where the clock block's weather comes from"
updated_at: "2026-11-07T03:15:54.463496866+00:00"
---

## Question

Where the clock block's weather comes from. LoR had no weather data, and a source needing an account, a key or money was Sumeet's call (the strategy's Authority).

## Decision

Google's Weather API (`weather.googleapis.com/v1`), Sumeet, 2026-11-07: free at this use, and already enabled on his Google Cloud key. Checked the same day: `currentConditions:lookup` for Milpitas returned 200 with the current conditions. Consequences, for lor-sbth to plan: LoR's server calls it, not the phone, so the key stays on the box. Under LoR's rules the key goes in the env file, which is 0600 (today it sits at `~/projects/gmaps_key`, which is 0664). The phone gets weather through `/device/home` or a new read endpoint, cached server-side. The location is the phone's last known place, which LoR already records.

## Close Reason

Google's Weather API, called by LoR's server with the key in the env file (Sumeet, 2026-11-07)
