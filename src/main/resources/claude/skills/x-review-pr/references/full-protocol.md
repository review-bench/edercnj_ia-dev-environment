# x-review-pr — Full Protocol Reference

Supplementary details carved out of SKILL.md to satisfy the orchestrator size contract (ADR-0007).

## State File Schema (Step 8.4)

**Path:** `plans/review/<pr-number>/state.json`

**Schema (Rule 20 §State File Schema — version 1.0):**

```json
{
  "phase": "GATE_FIX_PR",
  "lastPhaseCompletedAt": "<ISO-8601 UTC>",
  "lastGateDecision": "<PROCEED|FIX_PR|ABORT|null>",
  "fixAttempts": [
    {
      "at": "<ISO-8601 UTC>",
      "delegateSkill": "x-fix-pr",
      "prNumber": 123,
      "outcome": "applied"
    }
  ],
  "schemaVersion": "1.0"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `phase` | String | Yes | Always `"GATE_FIX_PR"` for this skill |
| `lastPhaseCompletedAt` | String (ISO-8601 UTC) | Yes | Updated on each write |
| `lastGateDecision` | String \| null | Yes | One of `PROCEED`, `FIX_PR`, `ABORT`, or `null` before first interaction |
| `fixAttempts` | Array | Yes | Always present; `[]` before first fix; max 3 items |
| `schemaVersion` | String | Yes | Literal `"1.0"` |

**`fixAttempts` entry fields:** `at` (ISO-8601 UTC), `delegateSkill` (always `"x-fix-pr"`), `prNumber` (PR number), `outcome` (`applied` \| `no_comments` \| `compile_regression` \| `aborted`).

**Lifecycle:**
- Written atomically (write to `<path>.tmp`, rename) when slot 2 (FIX-PR) is selected
- Not written for PROCEED or ABORT selections
- Not written on non-interactive path (default)

**`--resume-review <pr>` flag:**

When present, reads the state file at `plans/review/<pr>/state.json` and restores `gateAttempts` from `fixAttempts.size()`. If the state file satisfies the schema (Rule 20), the gate loop resumes from the last decision point. If the state file is absent or invalid, the gate starts fresh (gateAttempts = 0) with a warning:
```
WARNING: State file not found at plans/review/<pr>/state.json. Starting gate from scratch.
```
If the state file fails schema validation, emit `GATE_SCHEMA_INVALID` with the path and the missing/malformed field name.

## Phase 5 — Frontmatter YAML Template

Full frontmatter block conforming to `governance/schemas/review-frontmatter-1.0.json`:

```
<!-- template-version: 1.0 -->
---
schema-version: "1.0"
generated-by: x-review-pr@<git rev-parse HEAD>
story-id: <STORY_ID>
epic-id: <EPIC_ID>
date: <date -u +%Y-%m-%dT%H:%M:%SZ>
decision: <GO|NO-GO|GO-WITH-RESERVATIONS>
score: <integer 0-55>
score-max: 55
severity-counts:
  critical: <count>
  high: <count>
  medium: <count>
  low: <count>
  info: <count>
blocking-findings:
<YAML list of critical/high findings, empty list [] if none>
checklist:
  passed: <integer 0-45>
  total: 45
  failed-sections:
<YAML list of failed section IDs, empty list [] if none>
---
# Tech Lead Review — <STORY_ID>
...existing prose body...
```

**Note:** `x-review-pr` does NOT emit the `reviewers` field — the Tech Lead is the sole
reviewer; `checklist` replaces `reviewers` as the optional field per schema spec.

After writing the artifact, validate:

    Bash command: `$CLAUDE_PROJECT_DIR/.claude/scripts/audit-review-frontmatter.sh --story <STORY_ID>`

If the script returns exit ≠ 0, abort with `REVIEW_FRONTMATTER_INVALID`. No fallback.
