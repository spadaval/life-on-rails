---
created_at: "2026-11-07T22:17:28.602205167+00:00"
id: "lor-iabt"
issue_type: "feature"
labels:
- "launcher"
priority: "P2"
relationships:
  blocks: []
  children: []
  attachments: []
  relates: []
schema: "atelier.issue"
schema_version: 2
status: "in_progress"
title: "The T3 panel's wide form: every thread beside the chosen one, with a reply box"
updated_at: "2026-11-08T09:41:51.185371833+00:00"
---

## Goal

On the inner screen the T3 panel is a place to work with the agent, not a stretched phone page: the agent page's column, with every listed thread, on the left, and the chosen thread on the right with its last messages, what it waits on, and a reply box. It replaces lor-el51's interim two-column full layout. The narrow form stays the agent page as it is.

## Done when

On the unfolded `T3_Fold_API35`, with the `t3` panel full, against the test server and a scratch T3 server holding an approval, a question, a running Replay thread and at least three idle threads, unless a line says otherwise:
- [x] The wide form (600 dp and up) is two columns, each scrolling on its own and reporting `scrollsInGrid`: on the left, the agent page's column as it is (Today, the T3 state, Running, Needs you), then a Threads section listing the other listed threads, most recently updated first, at most 20; on the right, the chosen thread. The left column is at most 420 dp wide. The quick-send button sits at the left column's bottom-end corner. As a half, and on `T3_Phone_API35`, the panel is today's single column, with no Threads section. Screenshots of both.
- [x] Tapping a card's or a row's text in the wide form chooses its thread for the right column, shown with the selection row; the card's icon still opens the T3 app at the thread. A LoR flag in Needs you has no thread: tapping its text opens the dashboard at `/tasks?id=`, as its Open does today, and chooses nothing; the right column keeps its thread. With nothing chosen, the first T3 card's thread is shown, else the most recently updated thread. The choice is kept in memory and survives a fold and an unfold (folded, the narrow page; unfolded again, the same thread).
- [x] The right column shows the thread's title, project and state; its last 6 messages, user and assistant, from `orchestration.getThreadProjection`'s `messages` (not system ones), oldest at top, each as plain text with who wrote it and when; and what the thread waits on, with the card's actions (Approve and Deny, or the question's choices). An approval approved there clears from the left column too. A JVM test decodes a recorded projection reply into those messages, leaving out system and streaming-empty ones.
- [x] The thread is read again when its shell row's `updatedAt` changes, one read at a time (a change during a read gives one more read after it), and not otherwise. Shown by a `T3Thread` log line per read while the Replay thread runs. The handoff names the size of the projection reply for the longest thread on the scratch server.
- [x] The reply box at the right column's bottom sends to the chosen thread with `T3Connection.send` (`message.dispatch`), in the process scope, so leaving the panel doesn't cancel it: a reply typed to an idle thread starts its turn, and the scratch server's web UI shows the message. While sending, the box can't send again; a failure shows its reason under the box and keeps the text. With the keyboard up, the box rests on it and the thread's end stays visible above (lor-sqfe's `adjustResize`). Screenshots.
- [x] The decision note on lor-ph30 about the agent page's interim full layout is marked as replaced by this item, and the spec's agent page paragraph describes the wide form.
- [x] `assembleDebug testDebugUnitTest` passes on boat.

## Scope

`agent/AgentPage.kt`, `agent/AgentModel.kt`, a new file in `agent/` for the thread column, `t3/Commands.kt` (decoding the projection's messages), `t3/T3Connection.kt` only if a call it lacks is needed, and the spec. Not quick send's sheet, which still starts new threads. Not T3 itself, which the app never changes.

## Context

- The left column's LoR flag cards and Today's timer carry lor-rz50's writes (Done, Seen, the timer) from the shared composables; this item adds no write of its own to LoR.
- lor-el51 draws the interim full layout (Today left, the rest right). This item replaces it; the narrow column it keeps is what the left column reuses.
- `T3Connection.send(threadId, text)` and `Commands.message` exist (used today only by the debug screen); `Commands.questions` already reads `getThreadProjection`. T3's `OrchestrationV2ConversationMessage` (`packages/contracts/src/orchestrationV2.ts` in `/home/supadava/projects/t3code`, read only) has `role`, `text`, `streaming` and `createdAt`.
- Messages are plain text: Markdown marks show as written. The T3 app, a tap away, renders them in full.
- The scratch T3 server and its Replay provider: `docs/missions/t3-launcher/plan.md`, "The scratch T3 server". Never the live T3 server on this box.

## Notes

- 2026-11-07 [decision] The T3 panel's reply box is its own field in the thread column, sending to the chosen thread with T3Connection.send (message.dispatch, deliveryIntent auto), not T3's quick send. Quick send keeps starting new threads.
  Reason: The reply goes to a thread already open beside it, and the send already exists (T3Connection.send, used by the debug screen). Quick send is a separate sheet activity that launches a new thread in a project; replying through it would add a second kind of destination to its sheet, and the sheet would cover the thread being answered on the inner screen.
  Rejected: Quick send's sheet with the chosen thread as its destination: a new mode in QuickSendActivity, and it hides the thread while you write
- 2026-11-07 [decision] The chosen thread's end is read with orchestration.getThreadProjection when the thread is chosen and again when its shell row's updatedAt changes, not by orchestration.subscribeThread; messages are shown as plain text.
  Reason: The app already calls getThreadProjection for questions and already receives each thread's updatedAt in the shell, so no new subscription or stream decoding is needed. Plain text keeps a Markdown renderer out of the launcher; the T3 app, a tap away, renders the thread in full. If a long thread's projection proves slow on the real Fold (lor-l2zj measures it), subscribeThread or a smaller read is the follow-up.
  Rejected: orchestration.subscribeThread: a second live stream per chosen thread and its event decoding, before a read on change is shown to be too slow
- 2026-11-08 [decision] The Threads section lists the listed threads not already shown above it (not a T3 card's thread in Needs you, not in Running), so each thread is on the left once. In the wide form a Running row's tap chooses its thread like a card's text; the T3 app is reached from the thread column's Open in T3 and from a card's icon.
  Reason: The Done when line asks for 'the other listed threads' and for a row's text to choose; a Running row is all text with no icon, so Open in T3 moves to the chosen thread's head, where it opens the thread being read.
  Rejected: Every listed thread in Threads: the approval and running threads twice on one screen
- 2026-11-08 [decision] The narrow page and the wide form's left column share one scroll state, and FormScroll (lor-ozj2's mapping between the two forms' scrolls) is deleted with its test.
  Reason: The left column is now the narrow column plus Threads, so the same offset means the same place in both forms; a fold keeps the place with no mapping. FormScroll mapped between Today-left/queue-right columns that no longer exist.
  Rejected: Keep FormScroll and map the old two-column offsets: maps a layout this item removes

## Report

All seven Done when lines hold on `6ac3851`. The checks ran on the unfolded `T3_Fold_API35` and on `T3_Phone_API35` with APK `iabt-b3`, against the test server (`:3857`) and a scratch T3 server holding an approval, a question, four running Claude Haiku turns and over 40 idle threads.
- **Wide form, choosing, the right column, reads and the reply box (lines 1 to 5):** receipt lor-wwdo, whose README names the file behind each claim.
- **Narrow form on the phone:** receipt lor-fjoi.
- **Build:** `assembleDebug testDebugUnitTest` passed on boat for `6ac3851` (384 tests, 0 failures).
- **Tests:** `CommandsTest.theThreadColumnReadsTheLastMessagesOldestFirst` decodes the recorded projection (`projection-messages.ndjson`). `ThreadChoiceTest` covers which thread shows, what Threads lists, and the reply box's two failure wordings.
- **Projection size for the longest thread:** after the checks, the largest of the scratch server's 51 listed threads is the Replay showcase thread at 242,807 characters of JSON (about 243 KB). The Fold read it in 120 to 217 ms. Before the checks the largest was "Back up the notes" at 134,979 characters.

### Deviations

- **One fix the checks asked for (`6ac3851`, fail receipt lor-65ew on `iabt-b2`).** A reply that timed out while the scratch server was paused said "Not sent" and kept its text, but it arrived when the server resumed. That wording invites a second copy. The box now says "Not confirmed: … It may still reach the thread, so check it before sending again." It says "Not sent" only when no socket was open (`Replies.failure`). The same commit trims trailing blank lines from messages, which left a gap under some of them.
- **`FormScroll` and its test are deleted** (`AgentStates.kt`, outside the listed Scope; decision note).

### Decisions

- Two decision notes of 2026-11-08 on this item:
  - Threads lists the threads not already in Running or Needs you. A Running row's tap chooses its thread, and Open in T3 sits in the thread's head.
  - The narrow page and the left column share one scroll.

### Surprises

- **Reads when the column is shown again.** Besides choosing a thread and `updatedAt` changes, the column reads once each time it comes back into composition with `updatedAt` unchanged: after an unfold, on returning to the panel, and after a layout change. I count those as "when the thread is chosen" (the decision note's first read). They're in lor-wwdo's `logcat-T3Thread.txt`.
- **The emulator's keyboard.** agent-device replaces it with a headless input method by default, so the keyboard shot needed `open --no-test-ime` after closing the session.
- **Chrome and T3 open beside home.** Both open in adjacent windows on the inner screen, so after a link, home is behind them.
- **The test server exited mid-run,** as it did for lor-lz97. I restarted it with `verify.mjs up` from main `266f3c3`.

### Open items

- **Not observed:** the right column handing a vertical swipe to the grid at its edge. It reports `scrollsInGrid` through `listTakes(reversed = true)` because it scrolls from its end. The left column's hand-off was seen on both emulators.
- **Quick send overlaps the left column.** With the keyboard up, or on a long column, the quick-send button sits over the left column's last rows. Its place is the line's ("bottom-end corner"), and the column keeps 120 dp of bottom padding so the rows can scroll clear of it.
- **Real Fold:** lor-l2zj measures a long thread's projection read on the real device after merge.
