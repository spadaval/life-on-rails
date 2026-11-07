---
created_at: "2026-11-07T22:16:39.563187546+00:00"
id: "lor-g4ta"
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
    id: "lor-kqhy"
    type: "derived_from"
  - kind: "issue"
    id: "lor-sbth"
    type: "related"
  - kind: "issue"
    id: "lor-uj0r"
    type: "advances"
schema: "atelier.issue"
schema_version: 2
closed_at: "2026-11-07T22:16:42.456179579+00:00"
status: "done"
title: "How the weather reaches the clock block"
updated_at: "2026-11-07T22:16:42.456179579+00:00"
---

## Question

How does the weather that lor-kqhy chose reach the phone's clock block: which env variable holds the key, what LoR's server calls and how often, where the location comes from, and what `/device/home` carries. It is a contract between the server and the phone, and names a credential.

## Options

- **Delivery.** A `weather` field on `/device/home`, which the agent page already reads each minute and the clock already reads its next event from; or a new `/device/weather` endpoint the clock polls. The field needs no new request on the phone; the endpoint keeps `/device/home` free of an outside call.
- **When the server calls Google.** On a read of `/device/home` when its cache is stale, or from a scheduler job. A job calls Google when no one is looking and needs `LOR_RUN_JOBS`; the test server has no jobs.
- **Units.** Ask Sumeet, or send Celsius and let the phone's locale choose.

## Decision

A `weather` field on `/device/home`, fetched on read and cached (planner, 2026-11-07, within lor-kqhy and the strategy's authority):

- **The key** is `GOOGLE_MAPS_KEY` in the env file (`~/.config/life-on-rails/env`, 0600), read through `src/server/env.ts` with a default of `""`. Unset, there is no weather and nothing calls Google. It is never logged.
- **The call.** `src/server/weather.ts` calls `GET https://weather.googleapis.com/v1/currentConditions:lookup` with `key`, `location.latitude`, `location.longitude` and `unitsSystem=METRIC`, with a 3 s timeout. One call, current conditions only: no forecast.
- **The location** is the latest `location` observation's `payload.lat` and `payload.lon` (what `loadFixes` in `attribution/fill.ts` reads), at any age. No observation, no weather. Coordinates are not logged.
- **The cache.** One entry in memory, refreshed on age alone: a successful reply is kept 30 minutes from when it was fetched, whatever the phone's location does meanwhile, and the next fetch takes the latest location then. A read with a fresh entry calls nothing; otherwise `deviceHome` awaits the call alongside its other reads. That bounds it at about 1,440 calls a month, inside the free use lor-kqhy checked (Google lists 10,000 free Weather events a month). A failed, non-200 or timed-out call gives `weather: null` and is remembered for 5 minutes, during which reads call nothing and answer `null`, so an outage adds its 3 s to at most one read in 5 minutes. A restart empties the cache. No table.
- **The log.** One line per Google call, with its HTTP status (or `timeout`, or the error's name) and its duration in ms; never the key or the coordinates.
- **The field.** `weather: { temp_c: number, condition: string, is_day: boolean, observed_at: string } | null`, from the reply's `temperature.degrees`, `weatherCondition.description.text`, `isDaytime` and `currentTime`. Null when unset, without a location, or after a failure.
- **Units** are the phone's: it shows Fahrenheit when the locale's measurement system is US (`android.icu.util.LocaleData.getMeasurementSystem`), else Celsius. Nothing to ask.
- Consequences: the spec's Config list gains `GOOGLE_MAPS_KEY`. Sumeet puts his key there (today it is at `~/projects/gmaps_key`, mode 0664), the step no agent takes for him.

Rejected: a `/device/weather` endpoint (a second poll on the phone for one line of the clock); a scheduler job (calls Google while no one looks, and the test server runs no jobs); a table for the cache (one value, refetched in a second after a restart).

## Report

`/device/home` carries `weather` (Celsius, condition, day, time) from Google's current conditions at the phone's last fix, cached 30 min in memory; key `GOOGLE_MAPS_KEY` in the env file
