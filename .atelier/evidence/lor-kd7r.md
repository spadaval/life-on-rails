---
created_at: "2026-11-09T08:30:44.860345836+00:00"
id: "lor-kd7r"
evidence_type: "validation"
commit: "9149fb450c77f75c02644c26876709400fe0c1c0"
result: "pass"
captured_at: "2026-11-09T08:30:34.474113397+00:00"
command: "python3 -c 'import subprocess, json, sys\njs = r'\"'\"''\"'\"''\"'\"'\nimport com.spadaval.lor.home.*;\nimport java.util.*;\nimport java.time.*;\nint failures = 0;\nvoid check(boolean ok, String label) { if (!ok) throw new AssertionError(label); System.out.println(\"PASS \" + label); }\nlong h = 3600000L;\nlong t = Instant.parse(\"2026-11-10T08:00:00Z\").toEpochMilli();\nvar zone = ZoneId.of(\"UTC\");\nvar saved = new AppOpens(Map.of(),0L,Map.of(),t,\"com.spadaval.lor\",h,3);\nvar disk = AppOpens.Companion.parse(saved.toJson());\ncheck(disk.getCursorBoot() == 3, \"R9 boot identity survives JSON\");\ncheck(disk.corrected(t+4*h,2*h,4).getCursor() == t, \"R9 reboot with longer uptime keeps original wall cursor\");\nvar afterBoot = QuickDialKt.withUsage(disk,List.of(new Resumed(\"mail\",t+2*h+h/2)),Set.of(\"com.spadaval.lor\"),t+4*h,2*h,4,zone);\ncheck(afterBoot.getCounts().containsKey(\"mail\"), \"R9 valid post-boot mail resume counted, original failure resolved\");\ncheck(afterBoot.getCursor() == t+4*h && afterBoot.getCursorBoot() == 4, \"R9 stores new cursor and boot\");\ncheck(disk.corrected(t+4*h,h/2,4).getCursor() == t, \"R9 reboot with shorter uptime keeps cursor\");\ncheck(disk.corrected(t+4*h,2*h,0).getCursor() == t, \"R9 unknown boot identity does not infer clock jump\");\nvar legacy = AppOpens.Companion.parse(saved.toJson().replace(\"\\\"cursorBoot\\\":3,\", \"\").replace(\",\\\"cursorBoot\\\":3\", \"\"));\ncheck(legacy.getCursorBoot() == 0 && legacy.corrected(t+4*h,2*h,4).getCursor() == t, \"R9 old JSON without boot identity remains readable\");\nlong month = 28*24*h;\nvar initial = new AppOpens(Map.of(),0L,Map.of(),t-1000,null,1000000L,1);\nvar advanced = QuickDialKt.withUsage(initial,List.of(new Resumed(\"mail\",t+month+4000)),Set.of(),t+month+5000,1005000L,1,zone);\nvar corrected = AppOpens.Companion.parse(QuickDialKt.withUsage(advanced,List.of(new Resumed(\"maps\",t+9000)),Set.of(),t+10000,1010000L,1,zone).toJson());\ncheck(corrected.getLast().get(\"mail\") == t+4000 && corrected.getAt() == t+9000, \"R10 corrected last and at share the cursor wall basis after JSON round trip\");\nvar weekLater = corrected.opened(\"chat\",t+7*24*h,zone);\ndouble mail = Arrays.stream(weekLater.getCounts().get(\"mail\")).sum();\nSystem.out.println(\"R10 expected ~0.707107 after 7 real days, actual=\"+mail);\ncheck(Math.abs(mail-Math.pow(0.5,0.5))<0.001, \"R10 original future timestamp and stalled decay failure resolved\");\nvar forwardStart = new AppOpens(Map.of(),0L,Map.of(),0,null,0L,0).opened(\"mail\",t-1000,zone).readTo(t,null,1000000L,1);\nvar forward = forwardStart.corrected(t+month+5000,1005000L,1);\ncheck(forward.getAt()==t+month-1000 && forward.getLast().get(\"mail\")==t+month-1000 && forward.getCursor()==t+month, \"R10 forward correction shifts all three stored times\");\ncheck(Math.abs(Arrays.stream(forward.getCounts().get(\"mail\")).sum()-1.0)<0.000001, \"R10 forward correction preserves established counts\");\nvar workTap = forward.opened(\"work:slack\",t+month+5000,zone);\nvar workDisk = AppOpens.Companion.parse(workTap.toJson());\nvar secondTap = workDisk.corrected(t+month+10000,1010000L,1).opened(\"work:slack\",t+month+10000,zone);\ncheck(secondTap.getCursor()==forward.getCursor() && secondTap.getLast().get(\"mail\").equals(forward.getLast().get(\"mail\")), \"R10 multiple work taps do not apply clock jump twice\");\nvar afterRead = QuickDialKt.withUsage(secondTap,List.of(new Resumed(\"mail\",t+month-1000),new Resumed(\"maps\",t+month+11000)),Set.of(),t+month+12000,1012000L,1,zone);\ncheck(Math.abs(Arrays.stream(afterRead.getCounts().get(\"work:slack\")).sum()-2.0)<0.001 && afterRead.getCounts().containsKey(\"maps\"), \"R10 work taps survive next usage read and new main-profile resume is counted\");\ncheck(Math.abs(Arrays.stream(afterRead.getCounts().get(\"mail\")).sum()-1.0)<0.001, \"R10 old shifted usage resume not recounted after work taps\");\nvar people = List.of(new Chat<String>(\"Alice\",t,\"messages:alice\",false,5,\"messages#0\"),new Chat<String>(\"Alice\",t-500,\"whatsapp:alice\",false,0,\"whatsapp#0\"),new Chat<String>(\"Bob\",t-2000,\"messages:bob\",false,1,\"messages#0\"));\nvar selected = QuickDialKt.recentPeople(people,t,4);\ncheck(selected.get(0).getValue().equals(\"messages:alice\") && selected.get(1).getValue().equals(\"messages:bob\") && selected.size()==2, \"R11 original cross-app older Alice failure resolved\");\nvar profiles = List.of(new Chat<String>(\"Alice\",t,\"messages:personal\",false,5,\"messages#0\"),new Chat<String>(\"Alice\",t-500,\"messages:work\",false,0,\"messages#10\"));\ncheck(QuickDialKt.recentPeople(profiles,t,4).get(0).getValue().equals(\"messages:personal\"), \"R11 ranks do not cross profiles of the same package\");\nvar batches = List.of(new Chat<String>(\"Alice\",t-500,\"messages:alice\",false,0,\"messages#0\"),new Chat<String>(\"Bob\",t,\"messages:bob\",false,5,\"messages#0\"),new Chat<String>(\"Carol\",t-200,\"whatsapp:carol\",false,0,\"whatsapp#0\"));\ncheck(QuickDialKt.recentPeople(batches,t,4).stream().map(c->c.getValue()).toList().equals(List.of(\"messages:alice\",\"messages:bob\",\"whatsapp:carol\")), \"R11 per-app batch order retained and distinct publishes ordered by newest time\");\n// Deliberate limit: the clock was wrong during boot 1 and corrected across boot 2.\nvar acrossBoot = QuickDialKt.withUsage(advanced,List.of(new Resumed(\"maps\",t+9000)),Set.of(),t+10000,10000L,2,zone);\nSystem.out.println(\"LIMIT backward clock change across reboot: maps counted on first read=\"+acrossBoot.getCounts().containsKey(\"maps\")+\", at still ahead=\"+(acrossBoot.getAt()>t+10000));\nvar acrossAgain = QuickDialKt.withUsage(acrossBoot,List.of(new Resumed(\"maps\",t+9000),new Resumed(\"chat\",t+19000)),Set.of(),t+20000,20000L,2,zone);\nSystem.out.println(\"LIMIT next same-boot read: maps recovered=\"+acrossAgain.getCounts().containsKey(\"maps\")+\", at=\"+acrossAgain.getAt()+\", chat counted=\"+acrossAgain.getCounts().containsKey(\"chat\"));\nvar acrossForward = QuickDialKt.withUsage(forwardStart,List.of(new Resumed(\"mail\",t+month-1000),new Resumed(\"maps\",t+month+4000)),Set.of(),t+month+5000,20000L,2,zone);\nSystem.out.println(\"LIMIT forward clock change across reboot: mail count=\"+Arrays.stream(acrossForward.getCounts().get(\"mail\")).sum()+\" (old weight prematurely decayed, shifted old resume counted again)\");\nSystem.out.println(\"RESULT: all independent fix checks passed\");\n/exit\n'\"'\"''\"'\"''\"'\"'\nremote = \"CP=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.20 -name '\"'\"'*.jar'\"'\"' | head -1); JSON=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.json/json -name '\"'\"'*.jar'\"'\"' | head -1); flock /tmp/lor-build.lock jshell --feedback concise --class-path \\\"/home/user/w/t3-e27663ba-9a90f76b/android/app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar:$CP:$JSON\\\" <<'\"'\"'REVIEW_JSHELL'\"'\"'\\n\"+js+\"\\nREVIEW_JSHELL\"\nr = subprocess.run(['\"'\"'boat'\"'\"','\"'\"'exec'\"'\"','\"'\"'bx_fxc6p2bw'\"'\"','\"'\"'--'\"'\"',remote],capture_output=True,text=True)\nprint(r.stderr,end='\"'\"''\"'\"')\ntry:\n d=json.loads(r.stdout)\n out=d.get('\"'\"'stdout'\"'\"','\"'\"''\"'\"'); err=d.get('\"'\"'stderr'\"'\"','\"'\"''\"'\"')\n print(out,end='\"'\"''\"'\"'); print(err,end='\"'\"''\"'\"')\n ok=d.get('\"'\"'exitCode'\"'\"')==0 and '\"'\"'RESULT: all independent fix checks passed'\"'\"' in out and '\"'\"'AssertionError'\"'\"' not in out and '\"'\"'Error:'\"'\"' not in out\nexcept ValueError:\n print(r.stdout); ok=False\nsys.exit(0 if ok else 1)\n'"
exit_status: "0"
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
title: "Independent JShell reproductions against unmodified boat-built classes at 9149fb45: R9 boot identity survives JSON and the longer-uptime reboot counts the previously lost mail open; R10 forward/backward clock correction moves cursor/at/last consistently, preserves counts and decays mail to 0.707108 after a week, including work-tap persistence and the next usage read; R11 newest Alice wins across apps and profiles, while per-app publish rank order remains. Legacy JSON/unknown boot checks pass. Deliberate combined clock-change/reboot limit also measured: the first backward read misses maps, the next read counts new chat; a forward jump can prematurely decay existing weight and recount shifted events (mail 1.249996 rather than about 1). This limitation affects the ranking cache only and is acceptable for this scoped fix. No emulator or phone used."
updated_at: "2026-11-09T08:30:44.889917031+00:00"
---

## Summary

Independent JShell reproductions against unmodified boat-built classes at 9149fb45: R9 boot identity survives JSON and the longer-uptime reboot counts the previously lost mail open; R10 forward/backward clock correction moves cursor/at/last consistently, preserves counts and decays mail to 0.707108 after a week, including work-tap persistence and the next usage read; R11 newest Alice wins across apps and profiles, while per-app publish rank order remains. Legacy JSON/unknown boot checks pass. Deliberate combined clock-change/reboot limit also measured: the first backward read misses maps, the next read counts new chat; a forward jump can prematurely decay existing weight and recount shifted events (mail 1.249996 rather than about 1). This limitation affects the ranking cache only and is acceptable for this scoped fix. No emulator or phone used.

## Command

```console
python3 -c 'import subprocess, json, sys
js = r'"'"''"'"''"'"'
import com.spadaval.lor.home.*;
import java.util.*;
import java.time.*;
int failures = 0;
void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); System.out.println("PASS " + label); }
long h = 3600000L;
long t = Instant.parse("2026-11-10T08:00:00Z").toEpochMilli();
var zone = ZoneId.of("UTC");
var saved = new AppOpens(Map.of(),0L,Map.of(),t,"com.spadaval.lor",h,3);
var disk = AppOpens.Companion.parse(saved.toJson());
check(disk.getCursorBoot() == 3, "R9 boot identity survives JSON");
check(disk.corrected(t+4*h,2*h,4).getCursor() == t, "R9 reboot with longer uptime keeps original wall cursor");
var afterBoot = QuickDialKt.withUsage(disk,List.of(new Resumed("mail",t+2*h+h/2)),Set.of("com.spadaval.lor"),t+4*h,2*h,4,zone);
check(afterBoot.getCounts().containsKey("mail"), "R9 valid post-boot mail resume counted, original failure resolved");
check(afterBoot.getCursor() == t+4*h && afterBoot.getCursorBoot() == 4, "R9 stores new cursor and boot");
check(disk.corrected(t+4*h,h/2,4).getCursor() == t, "R9 reboot with shorter uptime keeps cursor");
check(disk.corrected(t+4*h,2*h,0).getCursor() == t, "R9 unknown boot identity does not infer clock jump");
var legacy = AppOpens.Companion.parse(saved.toJson().replace("\"cursorBoot\":3,", "").replace(",\"cursorBoot\":3", ""));
check(legacy.getCursorBoot() == 0 && legacy.corrected(t+4*h,2*h,4).getCursor() == t, "R9 old JSON without boot identity remains readable");
long month = 28*24*h;
var initial = new AppOpens(Map.of(),0L,Map.of(),t-1000,null,1000000L,1);
var advanced = QuickDialKt.withUsage(initial,List.of(new Resumed("mail",t+month+4000)),Set.of(),t+month+5000,1005000L,1,zone);
var corrected = AppOpens.Companion.parse(QuickDialKt.withUsage(advanced,List.of(new Resumed("maps",t+9000)),Set.of(),t+10000,1010000L,1,zone).toJson());
check(corrected.getLast().get("mail") == t+4000 && corrected.getAt() == t+9000, "R10 corrected last and at share the cursor wall basis after JSON round trip");
var weekLater = corrected.opened("chat",t+7*24*h,zone);
double mail = Arrays.stream(weekLater.getCounts().get("mail")).sum();
System.out.println("R10 expected ~0.707107 after 7 real days, actual="+mail);
check(Math.abs(mail-Math.pow(0.5,0.5))<0.001, "R10 original future timestamp and stalled decay failure resolved");
var forwardStart = new AppOpens(Map.of(),0L,Map.of(),0,null,0L,0).opened("mail",t-1000,zone).readTo(t,null,1000000L,1);
var forward = forwardStart.corrected(t+month+5000,1005000L,1);
check(forward.getAt()==t+month-1000 && forward.getLast().get("mail")==t+month-1000 && forward.getCursor()==t+month, "R10 forward correction shifts all three stored times");
check(Math.abs(Arrays.stream(forward.getCounts().get("mail")).sum()-1.0)<0.000001, "R10 forward correction preserves established counts");
var workTap = forward.opened("work:slack",t+month+5000,zone);
var workDisk = AppOpens.Companion.parse(workTap.toJson());
var secondTap = workDisk.corrected(t+month+10000,1010000L,1).opened("work:slack",t+month+10000,zone);
check(secondTap.getCursor()==forward.getCursor() && secondTap.getLast().get("mail").equals(forward.getLast().get("mail")), "R10 multiple work taps do not apply clock jump twice");
var afterRead = QuickDialKt.withUsage(secondTap,List.of(new Resumed("mail",t+month-1000),new Resumed("maps",t+month+11000)),Set.of(),t+month+12000,1012000L,1,zone);
check(Math.abs(Arrays.stream(afterRead.getCounts().get("work:slack")).sum()-2.0)<0.001 && afterRead.getCounts().containsKey("maps"), "R10 work taps survive next usage read and new main-profile resume is counted");
check(Math.abs(Arrays.stream(afterRead.getCounts().get("mail")).sum()-1.0)<0.001, "R10 old shifted usage resume not recounted after work taps");
var people = List.of(new Chat<String>("Alice",t,"messages:alice",false,5,"messages#0"),new Chat<String>("Alice",t-500,"whatsapp:alice",false,0,"whatsapp#0"),new Chat<String>("Bob",t-2000,"messages:bob",false,1,"messages#0"));
var selected = QuickDialKt.recentPeople(people,t,4);
check(selected.get(0).getValue().equals("messages:alice") && selected.get(1).getValue().equals("messages:bob") && selected.size()==2, "R11 original cross-app older Alice failure resolved");
var profiles = List.of(new Chat<String>("Alice",t,"messages:personal",false,5,"messages#0"),new Chat<String>("Alice",t-500,"messages:work",false,0,"messages#10"));
check(QuickDialKt.recentPeople(profiles,t,4).get(0).getValue().equals("messages:personal"), "R11 ranks do not cross profiles of the same package");
var batches = List.of(new Chat<String>("Alice",t-500,"messages:alice",false,0,"messages#0"),new Chat<String>("Bob",t,"messages:bob",false,5,"messages#0"),new Chat<String>("Carol",t-200,"whatsapp:carol",false,0,"whatsapp#0"));
check(QuickDialKt.recentPeople(batches,t,4).stream().map(c->c.getValue()).toList().equals(List.of("messages:alice","messages:bob","whatsapp:carol")), "R11 per-app batch order retained and distinct publishes ordered by newest time");
// Deliberate limit: the clock was wrong during boot 1 and corrected across boot 2.
var acrossBoot = QuickDialKt.withUsage(advanced,List.of(new Resumed("maps",t+9000)),Set.of(),t+10000,10000L,2,zone);
System.out.println("LIMIT backward clock change across reboot: maps counted on first read="+acrossBoot.getCounts().containsKey("maps")+", at still ahead="+(acrossBoot.getAt()>t+10000));
var acrossAgain = QuickDialKt.withUsage(acrossBoot,List.of(new Resumed("maps",t+9000),new Resumed("chat",t+19000)),Set.of(),t+20000,20000L,2,zone);
System.out.println("LIMIT next same-boot read: maps recovered="+acrossAgain.getCounts().containsKey("maps")+", at="+acrossAgain.getAt()+", chat counted="+acrossAgain.getCounts().containsKey("chat"));
var acrossForward = QuickDialKt.withUsage(forwardStart,List.of(new Resumed("mail",t+month-1000),new Resumed("maps",t+month+4000)),Set.of(),t+month+5000,20000L,2,zone);
System.out.println("LIMIT forward clock change across reboot: mail count="+Arrays.stream(acrossForward.getCounts().get("mail")).sum()+" (old weight prematurely decayed, shifted old resume counted again)");
System.out.println("RESULT: all independent fix checks passed");
/exit
'"'"''"'"''"'"'
remote = "CP=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.3.20 -name '"'"'*.jar'"'"' | head -1); JSON=$(find /home/user/.gradle/caches/modules-2/files-2.1/org.json/json -name '"'"'*.jar'"'"' | head -1); flock /tmp/lor-build.lock jshell --feedback concise --class-path \"/home/user/w/t3-e27663ba-9a90f76b/android/app/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar:$CP:$JSON\" <<'"'"'REVIEW_JSHELL'"'"'\n"+js+"\nREVIEW_JSHELL"
r = subprocess.run(['"'"'boat'"'"','"'"'exec'"'"','"'"'bx_fxc6p2bw'"'"','"'"'--'"'"',remote],capture_output=True,text=True)
print(r.stderr,end='"'"''"'"')
try:
 d=json.loads(r.stdout)
 out=d.get('"'"'stdout'"'"','"'"''"'"'); err=d.get('"'"'stderr'"'"','"'"''"'"')
 print(out,end='"'"''"'"'); print(err,end='"'"''"'"')
 ok=d.get('"'"'exitCode'"'"')==0 and '"'"'RESULT: all independent fix checks passed'"'"' in out and '"'"'AssertionError'"'"' not in out and '"'"'Error:'"'"' not in out
except ValueError:
 print(r.stdout); ok=False
sys.exit(0 if ok else 1)
'
```

Exit status: 0

## Stdout

Bytes: 1907
SHA-256: a98819d6f5cc1e925156edf0541848d3b16f92abc3c14f566dd78daa90f8c073

## Stderr

Bytes: 0
SHA-256: e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
