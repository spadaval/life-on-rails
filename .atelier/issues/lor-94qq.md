---
created_at: "2026-11-08T05:50:57.525563633+00:00"
id: "lor-94qq"
issue_type: "task"
labels:
- "verification"
priority: "P3"
relationships:
  blocks: []
  children: []
  attachments: []
  relates:
  - kind: "review"
    id: "lor-7zgz"
    type: "discovered_from"
schema: "atelier.issue"
schema_version: 2
status: "backlog"
title: "Correct lor-sfh9's claim that atelier-l58v already added the verify standard"
updated_at: "2026-11-08T05:50:57.525563633+00:00"
---

## Goal

lor-sfh9's Goal and Context say atelier-l58v, in `~/projects/atelier`, adds the Agent Factory verify-skill standard to the skill, and that the gardener procedure it added keeps the feature map current. Neither is true yet. atelier-l58v is still todo, and only on that repo's `master`: the atelier checkout is detached, `atelier issue show atelier-l58v` fails there, and there is no `.atelier/issues/atelier-l58v.md` in that tree. The agent-factory skill has no `procedures/garden.md` and no verify-skill reference. An author who follows the citation finds an unstarted plan, or nothing on the checked-out tree, and can treat the map's upkeep as already someone else's job.

The Done when lines of lor-sfh9 already say what this skill has to do, so this does not change that build.

## Done when

- [ ] lor-sfh9's Goal and Context no longer say the verify-skill standard or the gardener has already been added. They either drop the atelier-l58v citation, or say both are what atelier-l58v will add when it lands, and that lor-sfh9's Done when lines are the standard to build until then.

## Scope

Those two sentences in lor-sfh9. Not the verify skill, and not atelier-l58v itself.
