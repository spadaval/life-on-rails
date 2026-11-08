---
schema: atelier.review
schema_version: 2
id: lor-zpsq
title: finding on lor-lz97 @0c1b635f
target: lor-lz97
identity: reviewer-lz97-1
commit: 0c1b635fd3a89160ea3ffbb11d840e487ff7b8b7
created_at: 2026-11-08T09:04:06.899758699Z
event:
  event: finding
  recommendation: no_action
---

low: /tmp/lor-verify/lor-wt-clgw/phone3/README.md (receipt lor-7p8t; no_action, without consequence) The README names the task id each text tap opened (20: 01a1ba26-27e5…, 22: 01a1ba26-281d…), but no saved file shows those ids. Logcat's START line masks dat= as http://10.0.2.2:3857/..., and Chrome's address field cuts off after '/tasks?id='. The Done when line asks for '/tasks?id=' and the launch line, and both are shown. Tap 21's web page highlights the tapped flag's row, and every row's path is the one-line LorRows.taskPath(id). Next time, drop the ids from the README or capture the full URL.
