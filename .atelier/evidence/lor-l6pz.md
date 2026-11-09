---
created_at: "2026-11-09T08:21:38.324905359+00:00"
id: "lor-l6pz"
evidence_type: "validation"
commit: "44d7fe1cefecbfb8d727880c9124dc32206233ca"
result: "fail"
captured_at: "2026-11-09T08:21:32.121629035+00:00"
command: "python3 -c 'import subprocess,json,sys\nr=subprocess.run([\"boat\",\"exec\",\"bx_fxc6p2bw\",\"--\",\"CP=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.20 -name '\"'\"'*.jar'\"'\"' | head -1); JSON=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.json/json -name '\"'\"'*.jar'\"'\"' | head -1); flock /tmp/lor-build.lock jshell --feedback concise --class-path \\\"/home/user/w/t3-e27663ba-9a90f76b/android/app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar:$CP:$JSON\\\" <<'\"'\"'REVIEW_JSHELL'\"'\"'\\nimport com.spadaval.lor.home.*;\\nimport java.util.*;\\nimport java.time.*;\\nint failures = 0;\\nlong h = 3600000L;\\nlong t = Instant.parse(\\\"2026-11-10T08:00:00Z\\\").toEpochMilli();\\nvar zone = ZoneId.of(\\\"UTC\\\");\\nvar saved = new AppOpens(Map.of(),0L,Map.of(),t,\\\"com.spadaval.lor\\\",h);\\nvar disk = AppOpens.Companion.parse(saved.toJson());\\nSystem.out.println(\\\"R9 reboot: saved cursor=\\\"+t+\\\" uptime=1h; next read=t+4h uptime=2h, so boot occurred at t+2h and no wall-clock change occurred.\\\");\\nSystem.out.println(\\\"R9 expected cursor=\\\"+t+\\\" actual=\\\"+disk.readFrom(t+4*h,2*h));\\nvar afterBoot = QuickDialKt.withUsage(disk,List.of(new Resumed(\\\"mail\\\",t+2*h+h/2)),Set.of(\\\"com.spadaval.lor\\\"),t+4*h,2*h,zone);\\nif (!afterBoot.getCounts().containsKey(\\\"mail\\\")) { System.out.println(\\\"FAIL R9 valid mail open at t+2.5h discarded and cursor advanced beyond it.\\\"); failures++; }\\nlong month = 28*24*h;\\nvar initial = new AppOpens(Map.of(),0L,Map.of(),t-1000,null,1000000L);\\nvar advanced = QuickDialKt.withUsage(initial,List.of(new Resumed(\\\"mail\\\",t+month+4000)),Set.of(),t+month+5000,1005000L,zone);\\nvar corrected = AppOpens.Companion.parse(QuickDialKt.withUsage(advanced,List.of(new Resumed(\\\"maps\\\",t+9000)),Set.of(),t+10000,1010000L,zone).toJson());\\nvar weekLater = corrected.opened(\\\"chat\\\",t+7*24*h,zone);\\ndouble mail = Arrays.stream(weekLater.getCounts().get(\\\"mail\\\")).sum();\\nSystem.out.println(\\\"R10 clock advanced 28d then corrected after 5s; mail last=\\\"+corrected.getLast().get(\\\"mail\\\")+\\\" current time=\\\"+(t+10000));\\nif (Math.abs(mail - Math.pow(0.5,0.5)) > 0.001) { System.out.println(\\\"FAIL R10 after 7 real days mail count expected~0.707 actual=\\\"+mail+\\\"; decay at remains \\\"+weekLater.getAt()); failures++; }\\nvar people = List.of(new Chat<String>(\\\"Alice\\\",t,\\\"messages:alice\\\",false,5),new Chat<String>(\\\"Alice\\\",t-500,\\\"whatsapp:alice\\\",false,0),new Chat<String>(\\\"Bob\\\",t-2000,\\\"messages:bob\\\",false,1));\\nString alice = QuickDialKt.recentPeople(people,t,4).get(0).getValue();\\nif (!alice.equals(\\\"messages:alice\\\")) { System.out.println(\\\"FAIL R11 newest Alice expected=messages:alice actual=\\\"+alice+\\\"; unrelated app ranks compared.\\\"); failures++; }\\nSystem.out.println(\\\"RESULT: \\\"+failures+\\\" contract failures against unmodified compiled branch classes.\\\");\\nSystem.exit(failures == 0 ? 0 : 1);\\nREVIEW_JSHELL\"],capture_output=True,text=True)\nprint(r.stderr,end=\"\")\ntry:\n d=json.loads(r.stdout); print(d.get(\"stdout\",\"\"),end=\"\"); print(d.get(\"stderr\",\"\"),end=\"\"); sys.exit(1 if \"RESULT: 3 contract failures\" in d.get(\"stdout\",\"\") else 2)\nexcept ValueError:\n print(r.stdout); sys.exit(2)\n'"
exit_status: "1"
agent_identity: "gpt-6.1-sol"
target:
  kind: "issue"
  id: "lor-2imw"
  role: "validates"
relationships:
  blocks: []
  children: []
  attachments:
  - kind: "issue"
    id: "lor-2imw"
    role: "validates"
  relates: []
schema: "atelier.evidence"
schema_version: 2
status: "recorded"
title: "Independent JShell checks of the unmodified boat-built branch classes at 44d7fe1c: R9 reboot cursor discards a valid post-boot open after a quick-dial.json round trip; R10 a forward/backward clock correction leaves at/last in the future after persistence and stops a week of decay; R11 independent apps' ranks select the older of two Alice conversations. All three expected behaviours fail; transcript includes exact code and outputs. No device used."
updated_at: "2026-11-09T08:21:38.354047023+00:00"
---

## Summary

Independent JShell checks of the unmodified boat-built branch classes at 44d7fe1c: R9 reboot cursor discards a valid post-boot open after a quick-dial.json round trip; R10 a forward/backward clock correction leaves at/last in the future after persistence and stops a week of decay; R11 independent apps' ranks select the older of two Alice conversations. All three expected behaviours fail; transcript includes exact code and outputs. No device used.

## Command

```console
python3 -c 'import subprocess,json,sys
r=subprocess.run(["boat","exec","bx_fxc6p2bw","--","CP=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.20 -name '"'"'*.jar'"'"' | head -1); JSON=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.json/json -name '"'"'*.jar'"'"' | head -1); flock /tmp/lor-build.lock jshell --feedback concise --class-path \"/home/user/w/t3-e27663ba-9a90f76b/android/app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar:$CP:$JSON\" <<'"'"'REVIEW_JSHELL'"'"'\nimport com.spadaval.lor.home.*;\nimport java.util.*;\nimport java.time.*;\nint failures = 0;\nlong h = 3600000L;\nlong t = Instant.parse(\"2026-11-10T08:00:00Z\").toEpochMilli();\nvar zone = ZoneId.of(\"UTC\");\nvar saved = new AppOpens(Map.of(),0L,Map.of(),t,\"com.spadaval.lor\",h);\nvar disk = AppOpens.Companion.parse(saved.toJson());\nSystem.out.println(\"R9 reboot: saved cursor=\"+t+\" uptime=1h; next read=t+4h uptime=2h, so boot occurred at t+2h and no wall-clock change occurred.\");\nSystem.out.println(\"R9 expected cursor=\"+t+\" actual=\"+disk.readFrom(t+4*h,2*h));\nvar afterBoot = QuickDialKt.withUsage(disk,List.of(new Resumed(\"mail\",t+2*h+h/2)),Set.of(\"com.spadaval.lor\"),t+4*h,2*h,zone);\nif (!afterBoot.getCounts().containsKey(\"mail\")) { System.out.println(\"FAIL R9 valid mail open at t+2.5h discarded and cursor advanced beyond it.\"); failures++; }\nlong month = 28*24*h;\nvar initial = new AppOpens(Map.of(),0L,Map.of(),t-1000,null,1000000L);\nvar advanced = QuickDialKt.withUsage(initial,List.of(new Resumed(\"mail\",t+month+4000)),Set.of(),t+month+5000,1005000L,zone);\nvar corrected = AppOpens.Companion.parse(QuickDialKt.withUsage(advanced,List.of(new Resumed(\"maps\",t+9000)),Set.of(),t+10000,1010000L,zone).toJson());\nvar weekLater = corrected.opened(\"chat\",t+7*24*h,zone);\ndouble mail = Arrays.stream(weekLater.getCounts().get(\"mail\")).sum();\nSystem.out.println(\"R10 clock advanced 28d then corrected after 5s; mail last=\"+corrected.getLast().get(\"mail\")+\" current time=\"+(t+10000));\nif (Math.abs(mail - Math.pow(0.5,0.5)) > 0.001) { System.out.println(\"FAIL R10 after 7 real days mail count expected~0.707 actual=\"+mail+\"; decay at remains \"+weekLater.getAt()); failures++; }\nvar people = List.of(new Chat<String>(\"Alice\",t,\"messages:alice\",false,5),new Chat<String>(\"Alice\",t-500,\"whatsapp:alice\",false,0),new Chat<String>(\"Bob\",t-2000,\"messages:bob\",false,1));\nString alice = QuickDialKt.recentPeople(people,t,4).get(0).getValue();\nif (!alice.equals(\"messages:alice\")) { System.out.println(\"FAIL R11 newest Alice expected=messages:alice actual=\"+alice+\"; unrelated app ranks compared.\"); failures++; }\nSystem.out.println(\"RESULT: \"+failures+\" contract failures against unmodified compiled branch classes.\");\nSystem.exit(failures == 0 ? 0 : 1);\nREVIEW_JSHELL"],capture_output=True,text=True)
print(r.stderr,end="")
try:
 d=json.loads(r.stdout); print(d.get("stdout",""),end=""); print(d.get("stderr",""),end=""); sys.exit(1 if "RESULT: 3 contract failures" in d.get("stdout","") else 2)
except ValueError:
 print(r.stdout); sys.exit(2)
'
```

Exit status: 1

## Stdout

Bytes: 874
Truncated: no
SHA-256: e5827abbad6b5eaae76feb36135a4f2ec7b75bb2e865f249a77b8efd06ef57c8

```text
jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> jshell> R9 reboot: saved cursor=1794297600000 uptime=1h; next read=t+4h uptime=2h, so boot occurred at t+2h and no wall-clock change occurred.
jshell> R9 expected cursor=1794297600000 actual=1794308400000
jshell> jshell> FAIL R9 valid mail open at t+2.5h discarded and cursor advanced beyond it.
jshell> jshell> jshell> jshell> jshell> jshell> jshell> R10 clock advanced 28d then corrected after 5s; mail last=1796716804000 current time=1794297610000
jshell> FAIL R10 after 7 real days mail count expected~0.707 actual=1.0; decay at remains 1796716804000
jshell> jshell> jshell> FAIL R11 newest Alice expected=messages:alice actual=whatsapp:alice; unrelated app ranks compared.
jshell> RESULT: 3 contract failures against unmodified compiled branch classes.
jshell> |  State engine terminated.
jshell>
```

## Stderr

Bytes: 0
SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
