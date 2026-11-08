---
schema: atelier.review
schema_version: 2
id: lor-wc8z
title: finding on lor-iabt @5e9c7031
target: lor-iabt
identity: reviewer-iabt-1
commit: 5e9c7031353bb618c8caf9c0a3b81ebeeca5d03d
created_at: 2026-11-08T10:12:50.254962789Z
event:
  event: finding
  recommendation: fix_now
---

medium: android/app/src/main/java/com/spadaval/lor/agent/ThreadColumn.kt:226-228 (lor-iabt) The thread column can stay empty with 'Can't read the thread: Not connected to T3.' after T3 reconnects. The read fires as soon as the column composes, whatever the connection's state, and runs again only when the thread or its updatedAt changes. While the connection is Connecting or Retrying, T3Connection.call throws at once (calls is null, T3Connection.kt:184), and the shell keeps the last thread list while it reconnects (T3Connection.kt:241), so the column composes with a thread and its one read fails. When the socket comes back, the new snapshot carries the same updatedAt, so nothing reads again: the right column shows no messages, only the error, until that thread changes or another is chosen. Path: wake the Fold after the socket dropped in Doze (pings every 20 s, backoff up to 30 s) and unfold; or a read in flight when the socket drops leaves the error line under the old messages. This breaks line 3 (the right column shows the last 6 messages) on an everyday path; the receipts never ran a read while T3 was not Live. Fix: key the read on (id, updatedAt, live) and read only while the status is Live, so a read waits for the connection and a failed read is retried once it is back.
