---
created_at: "2026-11-07T22:01:01.675076771+00:00"
id: "lor-b1dx"
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
closed_at: "2026-11-07T22:01:08.451638263+00:00"
status: "done"
title: "How a panel says full or half on the inner screen"
updated_at: "2026-11-07T22:01:08.451638263+00:00"
---

## Question

How does `layout.json` say whether a panel is `full` or `half` on the inner screen, what is each panel type's default, and which panels share an inner screen? The strategy's Shape fixes the idea (a `full` panel is one inner screen, two neighbouring `half` panels share one, a lone `half` uses its full layout; `t3` and `lor` full, `drawer` half) but not the field's name, the stack's default, or what "neighbouring" means in a sparse grid. It is a persisted contract that lor-f3vn's minimap writes and an agent or hand edit reads.

## Options

- **Field name.** `wide` (`"wide": "full"` or `"half"`), matching the screen class names `start.wide` and `look.grid.wide` already use; or `width`, or `inner`. `width` reads like a number, and `inner` names a device rather than the screen class the file uses everywhere else.
- **The stack's default.** `full`: each page fills the inner screen in its two-column form. `half`: neighbouring stacks pair, so home and page 2 sit side by side, as Pixel Launcher and One UI Home show home's pages on a Fold's inner screen, and a lone stack still fills it.
- **Neighbouring.** The next column only (a gap breaks a pair), or the next panel in the row past any gap, as lor-qdmy's moves across already treat gaps.

## Decision

`wide`, stacks `half`, and neighbours past gaps (planner, 2026-11-07, within the strategy's authority over field names and defaults).

- A panel of any type may carry `"wide": "full"` or `"wide": "half"`. Any other value is an error naming its place (`layouts[0].panels[2].wide: "Full" is not "full" or "half"`). Absent, it is the type's default: `stack` half, `t3` full, `drawer` half, and `lor` full when lor-clgw adds that type. The phone writes `wide` only when it differs from the type's default, as it writes the other optional fields.
- A row's inner screens are built left to right by column: a `full` panel is one inner screen; a `half` shares one with the next panel in the row, past any gap, when that panel is also a `half` and not already paired; otherwise it is alone and drawn in its full layout. So no grid is invalid.
- The version 1 conversion writes no `wide`, so a converted file's inner screens are: the agent page; home and page 2; pages 3 and 4; and so on, with the drawer alone (full width) on row 1.
- Consequences: no `version` bump (an added optional field); lor-ph30 parses, writes and documents it in `layout-v2.md`; lor-f3vn's minimap sets it.

## Report

A panel's `wide` is "full" or "half"; stack half, t3 full, drawer half, lor full; a half pairs with the next half in its row, past any gap
