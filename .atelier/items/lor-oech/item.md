---
schema: atelier.item
schema_version: 3
id: lor-oech
title: 'Named states: any launcher or app surface opens from a fixture, in the lab and in the debug app'
status: building
mission: lor-2803
labels:
- design-lab
- tool
- launcher
priority: medium
blocked_by:
- lor-vbfd
- lor-zj7x
implementers:
- thread:delegated-task:command%3Amcp%3Ac1ec7934-b773-472e-bc30-dc89caf2d5f4%3Adelegate-task%3A3caae086-9655-46a3-b6da-8dd6904af7fa
ready:
  by: lead, on the plan agreed in round 7
  at: 2026-11-17T06:57:12.926382409Z
trivial: null
caused_by: null
created_at: 2026-11-17T05:57:53.935176453Z
---

## Goal

Any screen, in any condition, is one name away: a first run, a full home, three approvals waiting, T3 unpaired, a failed sync, a thread open on the inner screen. The audit could not see many of these because reaching them needed a paired T3 server, a real Fold or a layout written by hand.

## Done when

- [ ] A fixture is plain data in `homecore`'s common code: the layout, the app list with placeholder or bundled icons, `/device/home`'s payload, T3's threads, cards and messages, timer state, sensor and sync status, settings, and what search and the drawer read: contacts, app shortcuts and conversations, the settings pages, launch counts, notification dots and the quick dial's ranking. A fixture also carries where the screen is: the cell, an open folder or menu and what it points at, a scroll position, typed search text, a held item. Fixtures are composed from small parts, so a new state is a few lines.
- [ ] The catalogue covers every surface in the audit's inventory that has moved into the module, each in its ordinary, empty, loading, stale, failed and overfull condition where the surface has one, and on both screen sizes. `android/homecore/STATES.md` lists them by name with one line each. The ten launcher and app states lor-q165 names are in it under those names, each added by the item that moves its surface.
- [ ] The lab's State dropdown and URL hash open any of them. A state can also be passed as JSON in the URL or dropped onto the page, for a one-off.
- [ ] The debug build of the app opens a named fixture on the device: `am start -n com.spadaval.lor/.home.HomeActivity --es lab_state <name>` draws that state from the same fixtures with no server, T3 or layout file touched, and a banner says it is a fixture. Release builds have no such path (a JVM test shows it).
- [ ] The demo dataset's `/device/home` payload (lor-zj7x) is checked in as the default fixture, so the lab and the dashboard show the same made-up day.

## Scope

`homecore`'s fixtures, the lab's state loader, a debug-only entry in the app. It grows with lor-xdoi, lor-rgx4 and lor-t8lq: each of those adds the fixtures for what it moves, and this item gives them the shape and the first set.

## Unknowns

- Whether widgets get a fixture (a snapshot image) or a labelled box in the lab.
