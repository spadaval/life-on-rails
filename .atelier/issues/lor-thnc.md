---
created_at: "2026-11-10T06:11:15.864911794+00:00"
id: "lor-thnc"
issue_type: "feature"
labels:
- "chain"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "issue"
    id: "lor-l9a7"
    type: "advances"
  - kind: "issue"
    id: "lor-mxrc"
    type: "blocked_by"
schema: "atelier.issue"
schema_version: 2
status: "todo"
title: "Today, Goals, the wall and People stop showing wrong or failure-shaped numbers"
updated_at: "2026-11-10T06:11:15.864911794+00:00"
---

## Goal

The figures LoR shows are true, and where nothing was measured it says so. Seen on 10 November: Today said "14h remaining of the waking day" at 00:02; a past day said "23h 4m recorded" where the time was unlabelled place fill; five of seven goals read "0 / Nh this week"; the wall set "late" in bold; People's default list held carrier short codes, places and organisations, with "1385 days past the cadence" in the warning colour.

## Done when

- [ ] Before the waking day starts, Today's hero says when it starts and shows no remaining amount. A pure test covers 00:02, 07:59 and 08:00.
- [ ] A past day's hero figure is labelled time, and unlabelled phone and place time is shown beside it by that name. A pure test covers a day of place fill only.
- [ ] A goal with no committed time in the window reads "no data yet" on Today's Goals this week and on `/goals`, and "no data" on the wall's Goals frame with no empty meter. A goal with any committed time reads as now. Tests cover both, and a Goals frame is rendered and looked at (receipt).
- [ ] The wall's Tasks and Goals frames draw "late" in the regular weight.
- [ ] `important()` in `src/server/people.ts` no longer counts a contact as important for not coming from Google. An entity whose only identifiers are short codes (phone numbers of six digits or fewer) is not important for contact within 90 days either; it still lists with a page, a cadence or priority, and under All. Places are left out of every people filter. Organisations are unchanged. A database test covers: a contact not from Google with no page, cadence or recent contact (out); a short code texted yesterday (out of the default list, in All); a short code with a cadence (in); a place (out); a person with a cadence (in); an organisation in touch yesterday (in, as today).
- [ ] Overdue days on People are drawn in the secondary ink.
- [ ] `verify-lor screen today` and `screen people` pass on the test server, with screenshots as receipts. The people flow's organisation (Acme, `scripts/ui-check.mjs`) still lists and takes a cadence.
- [ ] `pnpm test`, `pnpm test:db` and `pnpm typecheck` pass.

## Scope

`src/routes/index.tsx`, the goals route and its components, `src/server/people.ts`, the people route's styling, `src/server/trmnl/frame.ts`, tests. No change to scores, targets or cadences.

## Context

- The hero's branches are in `src/routes/index.tsx` lines 137 to 145 (remaining) and 99 to 103 (recorded).
- `scripts/ui-check.mjs`'s today flow fails on main (lor-mxrc), which this item waits on.
- DESIGN.md names the inks.
