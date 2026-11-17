---
schema: atelier.item
schema_version: 3
id: lor-h3e9
title: A root vercel.json turns off Vercel's Git deploys, so a push stops failing a deploy and mailing
status: in_review
mission: null
labels: []
priority: medium
blocked_by: []
implementers:
- mcp:816a30dc-c40d-47a3-bd6a-bec897473252
ready: null
trivial: null
caused_by: null
created_at: 2026-11-17T08:59:15.146752894Z
---

## Goal

A push to the LoR repo no longer starts a Vercel deploy. A Vercel project from 2023, `life-on-rails-hxia`, is still connected to `spadaval/life-on-rails`. LoR is not hosted on Vercel, and the project's Node 18.x setting makes every deploy fail, so each push to `main` (and each pushed branch) fails a deploy and mails Sumeet: 53 deploys between 7 and 17 Nov 2026, the latest for f7617fa.

This is a stopgap. It hides that the Vercel project still exists and is still connected to the repo; disconnecting or deleting it needs Sumeet's Vercel login (LoR task 01a1c3e4-4187). When that is done, `vercel.json` should be deleted.

## Done when

- [x] `vercel.json` at the repo root sets `git.deploymentEnabled` to `false`, the documented switch for turning off all automatic deployments (https://vercel.com/docs/project-configuration/git-configuration).
- [x] `AGENTS.md` says what the file is for and when to delete it, since JSON cannot carry a comment.

## Scope

`vercel.json` and one line of `AGENTS.md`. Nothing LoR runs reads `vercel.json`.

Whether Vercel obeys the file can only be seen after the merge is pushed, so it is not a Done-when line: the implementer then checks that GitHub's deployments list (`gh api repos/spadaval/life-on-rails/deployments`) shows no `vercel[bot]` deployment for the merge commit, and records the result in LoR task 01a1e8b8.

## Report

Added `vercel.json` with `{"git": {"deploymentEnabled": false}}` (plus Vercel's `$schema` line) and a rule line in `AGENTS.md` naming it a stopgap.

Why the repo root is the right place: Vercel reads `vercel.json` from the project's root directory, and I have no Vercel login to read that setting. The evidence is indirect. GitHub's deployment list shows this project deploying in June 2023 (31 deployments), when the only app in the repo was the Next.js one at the root (now `archive/2023/`), and no subdirectory app existed. So its root directory is almost certainly the repo root. If it is not, the file is ignored and harmless, and the check after the push (see Scope) catches it.

Not checked here: that Vercel obeys the file, which needs the push. Branches that do not contain this commit may still start a preview deploy when pushed, until they pick up `main`; Vercel's docs do not say which commit the setting is read from.

No tests: nothing in LoR reads the file. `git diff --check` passes.
