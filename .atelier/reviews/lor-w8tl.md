---
schema: atelier.review
schema_version: 2
id: lor-w8tl
title: approval on lor-2mh2 @44dff9a0
target: lor-2mh2
identity: reviewer-2mh2-1
commit: 44dff9a0ab588e8ff1291e39a5ffad0f5bee445a
created_at: 2026-11-08T02:19:44.543399837Z
event:
  event: approval
---

Reviewed main..44dff9a whole. Each action calls exactly what the web calls (taskDoneFn: completeTask with via ui + device as sourceRef / reopenTask; unflagFn: unflagTask; assignFn/bulk assign: queueTask / assignTask owner), with no rule of its own; the post-write SELECT only turns a silent unflag on an unknown id into 404. None of the five functions writes time_block, so lockBlocks is not needed. doneTodayTasks is the old inline rule verbatim (same commitment query on localDayBounds(LOR_TZ, date), same filter on graph.nodes, taskRow, same order), and getTodayFn maps it to done:true as before. /device/* is reachable only on 127.0.0.1:3847 (on-box) and the :3848 ownerOnly listener; the TRMNL listener routes only its firmware paths. Refusals: 409 google-task with intents.ts's messages, 400 bad-input for ZodError/BadInput, 404 not-found; replies ISO and JSON-safe. Spec text matches. Ran in the worktree against life_on_rails_test: npx vitest run 149 passed, vitest.db.config 140 passed, tsc --noEmit clean, git diff --check clean. One DEFER (lor-l96d, reopenTask's missing type check, pre-existing on the web).
