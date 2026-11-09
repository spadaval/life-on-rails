---
created_at: "2026-11-09T03:17:00.593190821+00:00"
id: "lor-184c"
evidence_type: "validation"
commit: "9723a4ab34281ed020b0afbcd336fc781827c1ac"
result: "fail"
captured_at: "2026-11-09T03:17:00.593123826+00:00"
agent_identity: "gpt-6.1-sol"
target:
  kind: "issue"
  id: "lor-bdus"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-bdus"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Independent gpt-6.1-sol review reproductions on 9723a4ab, using JShell on boat against the classes from this checkout's fresh assembleDebug testDebugUnitTest build, under /tmp/lor-build.lock. No emulator or phone used. (1) v2 home apps [p.p], dock [p.p/p.p.B], installed label order [p.p.A,p.p.B]: converted allItems=[p.p]; HomeEditKt.resolve shows retained p.p resolves to A and former dock ref resolves to B; toJson has no B ref or dock, so the next write loses B. (2) v2 narrow start [0,0] stack [dock], five stacks at [1,0] through [5,0] with apps [p.p1] through [p.p5], dock [d.d]: parse throws 'layouts[0].panels: row 0 holds 6 stacks with an apps block, at most 5' despite 115 free slots in the original pages. (3) v2 five pages of 24 distinct refs plus dock [d.d], seeded identically in temporary layout.json and layout.good.json: read(null) throws dock[0] home-is-full; readGood() returns null; source bytes unchanged=true. Temporary fixture files removed. These fail the no-loss, valid-old-file conversion and last-good-display claims. Separately, the fresh standard boat build passed all 402 tests (0 failures/errors/skips), including all 8 NoDockTest cases and LayoutV1Test.eachFixtureConvertsToTheDefaultGrid; those tests do not cover these combinations."
updated_at: "2026-11-09T03:17:00.622241542+00:00"
---

Independent gpt-6.1-sol review reproductions on 9723a4ab, using JShell on boat against the classes from this checkout's fresh assembleDebug testDebugUnitTest build, under /tmp/lor-build.lock. No emulator or phone used. (1) v2 home apps [p.p], dock [p.p/p.p.B], installed label order [p.p.A,p.p.B]: converted allItems=[p.p]; HomeEditKt.resolve shows retained p.p resolves to A and former dock ref resolves to B; toJson has no B ref or dock, so the next write loses B. (2) v2 narrow start [0,0] stack [dock], five stacks at [1,0] through [5,0] with apps [p.p1] through [p.p5], dock [d.d]: parse throws 'layouts[0].panels: row 0 holds 6 stacks with an apps block, at most 5' despite 115 free slots in the original pages. (3) v2 five pages of 24 distinct refs plus dock [d.d], seeded identically in temporary layout.json and layout.good.json: read(null) throws dock[0] home-is-full; readGood() returns null; source bytes unchanged=true. Temporary fixture files removed. These fail the no-loss, valid-old-file conversion and last-good-display claims. Separately, the fresh standard boat build passed all 402 tests (0 failures/errors/skips), including all 8 NoDockTest cases and LayoutV1Test.eachFixtureConvertsToTheDefaultGrid; those tests do not cover these combinations.
