---
schema: atelier.review
schema_version: 3
round: 1
commit: 9c71339f2a4afe7d2d90331a475a3ab9751998a7
reviewers:
- thread:delegated-task:command%3Amcp%3A4388dd76-819a-49ea-9727-06f8f52c331b%3Adelegate-task%3A923833a7-7c7e-4ff0-bed5-e430481318ee
model: gpt-6.1-sol
verdict: approve
created_at: 2026-11-17T07:12:06.088810767Z
---


## Summary

- Complexity: low. One plugin version changes, with twelve redundant casts or null assertions removed and no new build machinery.
- Value: high. Kotlin 2.3.20 is a prerequisite for the planned shared browser module, and upgrading separately keeps compiler fallout isolated.
- Risk of merging: medium. The compiler and runtime library change throughout the installed Android app; failures are visible and reverting the upgrade is straightforward.

The value warrants this small change. I found no merge blocker at 9c71339f2a4afe7d2d90331a475a3ab9751998a7.

I read the full source diff against main, the affected expressions in context, the build configuration, the item and Report, and the evidence README. The only build edit is the Compose plugin version. The two production edits retain their previous conditions: the shortcut value has already been cast in the same expression, and a non-null jpeg implies the immutable capture is non-null. The test edits remove redundant checks without weakening assertions. The baseline commit 6dcaa4a has no Android difference from main.

I checked each Done when line against the supplied artifacts:

- Both boat logs show successful clean builds, assembleDebug and testDebugUnitTest, with both Kotlin compile tasks executed. buildEnvironment resolves AGP's kotlin-gradle-plugin request to 2.2.21 before and 2.3.20 after, through the Compose plugin and Kotlin plugins BOM as described. The wrapper and other build configuration are unchanged.
- I normalized and compared the compiler diagnostics: the same 13 warnings occur in both builds, with no new warnings or errors.
- I parsed all 56 JUnit XML suites in each retained build output. Both contain the same 422 test names, with zero failures, errors or skipped tests. I independently hashed and sized both APKs: their hashes match the README prefixes, and their sizes are 43,200,217 and 43,249,665 bytes. The 49,448-byte increase and the 51,772-byte growth in classes14.dex match the Report. The added Kotlin classes are present there; the other library dex entries are unchanged.
- I inspected the four screenshots and independently decoded all three screenshot sets with ffmpeg, then compared every RGB pixel in Python. Home, the folder and both drawer tabs are identical below y=80. All changed pixels are at y=47–79, in the system clock or signal indicator. The baseline repeat also changes only clock pixels. All four before/after accessibility snapshots are identical. The home screenshot explicitly shows the test endpoint at 10.0.2.2:3857.

I did not rerun Gradle or drive the emulator in this review; build and device execution are supported by the retained logs, APKs, XML results and screenshots. I did not reproduce the author's stopped-boat failure or disturb the shared VM. I confirmed the obsolete Atelier evidence command reported by the author; it is the deferred documentation finding below. The directory README supplies the requested device, commit and screenshot receipt despite that command being unavailable.

## Finding 1: Update the verify skill's obsolete Atelier commands
Recommendation: defer

This is pre-existing and does not block the Kotlin upgrade. `.claude/skills/verify-lor/SKILL.md:149` directs authors to `atelier evidence record`, and line 153 directs them to `atelier review request`. The installed Atelier has no evidence subcommand: `atelier evidence --help` returns "unrecognized subcommand 'evidence'". Its workflow uses `atelier submit` and `atelier review <id>` instead. Following the skill therefore fails when recording evidence or submitting work. Update those instructions to the current workflow and explain how to attach a receipt directory in the item's Report.
