---
name: x-handle-incident
description: "Guides SEV1-SEV4 incident response with checklists, comms templates, and postmortems."
user-invocable: true
argument-hint: "[severity SEV1|SEV2|SEV3|SEV4] [--postmortem] [--notify]"
allowed-tools: Read, Write, Bash, Grep, Glob, Agent
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Incident Response (slim — ADR-0012)

## Purpose

Provides an interactive incident response guide for {{PROJECT_NAME}} that walks the team through the complete process from detection to resolution. Classifies severity, loads severity-specific checklists, coordinates communication, triggers postmortems, and tracks action items.

## Triggers

- `/x-handle-incident` — start interactive severity classification
- `/x-handle-incident SEV1` — start SEV1 critical incident response
- `/x-handle-incident SEV2 --postmortem` — SEV2 incident with postmortem generation
- `/x-handle-incident SEV3 --notify` — SEV3 incident with communication templates
- `/x-handle-incident SEV1 --postmortem --notify` — full incident response workflow

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `severity` | positional | (interactive) | Severity level: `SEV1`, `SEV2`, `SEV3`, or `SEV4` |
| `--postmortem` | boolean | `false` | Generate postmortem document (auto-enabled for SEV1/SEV2) |
| `--notify` | boolean | `false` | Generate communication templates for all channels |

## Severity Quick Reference

| Severity | Label | Response Time | Update Frequency | Postmortem |
|----------|-------|---------------|------------------|------------|
| **SEV1** | Critical | 15 min | Every 30 min | Yes (always) |
| **SEV2** | High | 30 min | Every 1 hour | Yes (always) |
| **SEV3** | Medium | 4 hours | Every 4 hours | Only if `--postmortem` |
| **SEV4** | Low | Next business day | Daily | Only if `--postmortem` |

## Workflow Overview

```text
1. CLASSIFY    -> Validate severity arg OR ask user about impact and suggest classification
2. LOAD        -> Load severity-specific checklist from .claude/knowledge/sre-practices/
3. GUIDE       -> Walk team through Detection → Triage → Mitigation → Resolution
                  (dispatch sre-engineer agent for reliability expertise)
4. COMMUNICATE -> Generate templates (Status Page / Slack / Email) per --notify
5. POSTMORTEM  -> Generate _TEMPLATE-POSTMORTEM.md (auto SEV1/SEV2 or --postmortem)
6. TRACK       -> Register action items table (ID / Desc / Owner / Deadline / Priority / Status)
```

Each step's detailed checklists (per-severity), agent dispatch prompt, communication templates (Status Page / Slack / Email), postmortem template structure, and action-item tracking format live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): full severity classification table with criteria (User Impact / Financial / Data / Scope decision questions).
- **Step 2** (§Step 2): per-SEV checklists (SEV1 — 6 items: IC within 5 min, response team, war room, status page, exec notify within 15 min, all-hands; SEV2 — 5 items; SEV3 — 4 items; SEV4 — 3 items).
- **Step 3** (§Step 3): `sre-engineer` agent dispatch prompt with return contract (`rcaHypothesis`, `mitigationPlan`, `checklistGaps`); Detection → Triage → Mitigation → Resolution sub-workflows.
- **Step 4** (§Step 4): 3 communication templates (Status Page with status state machine, Slack/Teams with timeline format, Email with stakeholder fields); update-frequency table per severity.
- **Step 5** (§Step 5): postmortem trigger rules (auto SEV1/SEV2 + `--postmortem` flag); pre-filled fields from incident timeline; inline fallback when template absent.
- **Step 6** (§Step 6): action-item field table (ID / Description / Owner / Deadline / Priority / Status); Markdown output format.

## Error Handling

| Scenario | Action |
|----------|--------|
| Severity not provided | Ask the user about the impact and suggest classification based on description |
| Invalid severity (e.g., SEV5) | Reject with message: "Invalid severity. Use SEV1, SEV2, SEV3, or SEV4" |
| `--postmortem` without template | Generate inline postmortem with basic structure |
| No Incident Commander available | Assign the requesting user as IC and recommend finding a replacement |
| Incomplete information | Proceed with available data, note gaps in postmortem |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-troubleshoot-operations` | called-by | Escalates to this skill when an issue becomes a production incident |
| `sre-engineer` (agent) | calls | Delegates reliability expertise and checklist validation via Agent tool |
| sre-practices (KP) | reads | References `.claude/knowledge/sre-practices/` for incident management processes |

- Uses `_TEMPLATE-POSTMORTEM.md` for postmortem document generation. Fallback: inline postmortem with basic structure when template is absent.
- Uses `_TEMPLATE-INCIDENT-RESPONSE.md` for severity classification reference.
- Can be used standalone or as part of on-call response workflow.

## Full Protocol

Minimum viable contract above. Detailed per-severity checklists, `sre-engineer` agent dispatch prompt, 3 communication templates (Status Page / Slack / Email), postmortem template structure with inline fallback, and action-item tracking format live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
