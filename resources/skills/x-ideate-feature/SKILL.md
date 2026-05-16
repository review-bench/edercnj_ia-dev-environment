---
name: x-ideate-feature
description: "Transforms prose into a structured RA9 spec and opens a docs/feature-* PR for review."
model: opus
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Skill, Agent
argument-hint: "<prose-paragraph or file.txt|file.md>"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-ideate-feature (slim — ADR-0012)

## Purpose

Transform free-form ideation prose (or a text file) into a structured feature specification document (RA9 v2 minimal, 6 sections) and open a pull request for human review. The operator reviews and optionally edits the spec, then manually invokes `x-create-feature` to drive the full epic + story decomposition.

**No auto-chain (EPIC-0065 story-local rule):** This skill does NOT call `x-create-feature` automatically. It terminates after opening the PR with an explicit instruction to the operator. (Story-local invariant from EPIC-0065 — not the project-wide Rule 05.)

## Triggers

- `/x-ideate-feature "Implement CSV export with filters..."` — inline prose
- `/x-ideate-feature path/to/idea.txt` — text file as input
- User wants to convert an idea into a structured spec for engineer review

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `input` | String | Yes | Free-form prose (min 100 chars) or path to a `.txt`/`.md` file |

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | Spec created and PR opened |
| 1 | INPUT_TOO_SHORT | Prose < 100 characters |
| 2 | OPERATIONAL_ERROR | `gh` auth failure, push failure, or `git` error |
| 3 | WORKTREE_FAILED | `x-manage-worktrees` non-zero |

## Output Contract

| Artifact | Path |
|----------|------|
| Spec file | `docs/specs/SPEC-<slug>-v1.md` inside `.claude/worktrees/feature-ideation-<slug>/` |
| Branch | `docs/feature-<slug>` (base: `develop`) |
| PR | docs-label PR to `develop` with no auto-merge |
| Mandatory exit phrase | `"Spec pronta. Para criar a feature inteira, invoque \`/x-create-feature <PR-spec-path> --epic-id <NNNN>\`."` |

## Workflow Overview

```text
Phase 0: SETUP    -> Validate input ≥100 chars; derive kebab slug (first 5 words); Skill x-manage-worktrees create
Phase 1: ANALYZE  -> Agent(opus) extracts {domain, scopeIn, scopeOut, rules, stories, dor, dod, risks}
Phase 2: SPEC     -> Render RA9 v2 minimal (Sistema/Escopo/Regras/Histórias/DoR-DoD/Riscos) — each section ≥100 chars
Phase 3: PR       -> Skill x-commit-changes (docs:) + Skill x-create-pr (--no-auto-merge --label docs)
Phase 4: REPORT   -> Structured stdout + mandatory next-step phrase (no auto-chain to x-create-feature — operator invokes manually)
```

> **Spec-Driven Disciplines (`@spec-driven-kp`).** Phase 1 MUST run **Gray-Area
> Discovery** (§2): scan the prose for user-facing ambiguity and, if any, raise the
> single batched `AskUserQuestion` before emitting the spec (or, in
> `--non-interactive`, mark `UNRESOLVED — assumption:`). Phase 2 MUST assign a
> **Planning Depth Tier** (§1) and apply the **Knowledge Verification Chain** (§3):
> never fabricate technical facts — flag `UNVERIFIED`.

Detailed Phase 0–4 procedures, deep-reasoning subagent prompt with 8-key extraction contract, full RA9 v2 minimal template, validation rules, and report format live in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0** (§Phase 0): input-source detection (file vs inline); 100-char minimum guard; kebab-slug derivation (first 5 words, ASCII-only, max 40 chars); `Skill(x-manage-worktrees, model: haiku)` create with `feature-ideation-<slug>` identifier.
- **Phase 1** (§Phase 1): full subagent prompt with 8-element extraction (DOMAIN / SCOPE_IN / SCOPE_OUT / RULES / STORIES / DOR / DOD / RISKS); structured-JSON return contract.
- **Phase 2** (§Phase 2): RA9 v2 minimal Markdown template (6 mandatory sections: Sistema, Escopo, Regras, Histórias, DoR/DoD, Riscos); per-section ≥100-char validation rule.
- **Phase 3** (§Phase 3): `Skill(x-commit-changes, model: haiku)` invocation with `docs:` Conventional Commits prefix; `Skill(x-create-pr, model: haiku)` with `--no-auto-merge` and `--label docs`; skip CI-watch (planning artifacts — EPIC-0065 story-local rule, not project-wide Rule 03).
- **Phase 4** (§Phase 4): full structured-report template; mandatory next-step phrase format.

## Error Handling

| Scenario | Action |
|----------|--------|
| Input prose < 100 chars | Exit 1 (`INPUT_TOO_SHORT`) with message |
| File path provided but file not found | Exit 2 (`OPERATIONAL_ERROR`) |
| Worktree creation fails | Exit 3 (`WORKTREE_FAILED`) — no cleanup needed |
| Commit fails | Preserve worktree for diagnosis; exit 2 |
| PR creation fails | Exit 2; print worktree path for manual push |
| RA9 section < 100 chars | Expand inline before commit |
| `gh` not authenticated | Exit 2 with `gh auth login` instruction |

## Integration Notes

| Skill | Relationship | Notes |
|-------|-------------|-------|
| `x-manage-worktrees` | delegates (P0) | Creates isolated `feature-ideation-<slug>` worktree |
| `x-commit-changes` | delegates (P3) | Commits spec with `docs:` Conventional Commits prefix |
| `x-create-pr` | delegates (P3) | Opens PR to `develop` with label `docs`, no auto-merge |
| `x-create-feature` | NOT called | Operator manually invokes after reviewing the spec PR |

## Full Protocol

Minimum viable contract above. Detailed Phase 0–4 bash, subagent prompt with 8-key extraction, full RA9 v2 minimal Markdown template, per-section validation rules, structured-report template, and backward-compatibility note (EPIC-0065 hard-cut) live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
