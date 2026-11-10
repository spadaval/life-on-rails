---
schema: atelier.review
schema_version: 2
id: lor-fxlg
title: finding on lor-l9a7 @5dfae082
target: lor-l9a7
identity: planreview-a
commit: 5dfae0828d1f873057bcacdba992275be8555e53
created_at: 2026-11-10T06:20:52.448892914Z
event:
  event: finding
  recommendation: fix_now
---

R2 (medium) FIX NOW: The reconnect proof misses the in-flight request that can exceed five seconds. .atelier/issues/lor-sgl4.md:37 waits until both displays have greyed their last replies before restoring the network; its fake callback test only checks that three calls were made. Neither checks recovery while a read is still blocked, the exact case noted conditionally at line 51. LorReads.read holds its single mutex across fetch (android/app/src/main/java/com/spadaval/lor/net/LorReads.kt:156-175); LorApp wraps the blocking LorClient in withContext(IO), and LorClient.request waits up to 15 seconds for connect and 20 seconds for read, disconnecting only on exit (net/LorClient.kt:65-80). A new refresh queues behind that fetch, and cancelling its coroutine alone does not interrupt HttpURLConnection. Make a blocked in-flight request at network restoration part of the acceptance proof, and explicitly include the transport cancellation or stale-generation replacement needed to let the fresh read start within five seconds. The current all-network-off stand-in can pass while the tunnel-on-with-Wi-Fi failure remains. Android documents cooperative coroutine cancellation at https://developer.android.com/kotlin/coroutines/coroutines-best-practices .
