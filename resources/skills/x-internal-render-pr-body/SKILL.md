---
name: x-internal-render-pr-body
description: "Renders PR body Markdown from disk artifacts (--kind=implementation|backlog)."
visibility: internal
user-invocable: false
model: haiku
allowed-tools: [Read, Write, Bash, Skill]
argument-hint: "--kind=implementation|backlog --story-id <story-XXXX-YYYY> --out <abs-path>"
category: internal-pr
context-budget: light
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

> 🔒 **INTERNAL SKILL** — Invoked only by other skills via the Skill tool. Not user-invocable.

# Skill: x-internal-render-pr-body (slim — ADR-0012)

## Purpose

Renders a PR body Markdown file from existing on-disk artifacts (review, verify, telemetry,
commits) and writes it to `--out`. Consumed by `x-create-pr` (`kind=implementation`) and
`x-create-feature` (`kind=backlog`). Fail-open: any missing source produces a human-readable
placeholder — never an audit-sentinel string recognized by `audit-pr-evidence.sh`.

## Parameters

| Parameter | Required | Description |
| :--- | :--- | :--- |
| `--kind` | Yes | `implementation` or `backlog` |
| `--story-id` | When `--kind=implementation` | Pattern `story-XXXX-YYYY` |
| `--epic-id` | When `--kind=backlog` | Pattern `epic-XXXX` or `EPIC-XXXX` |
| `--out` | Yes | Absolute path for output Markdown file (must stay within `${REPO_ROOT}` — Rule 06 J6 path-traversal guard) |

## Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | OK | Body rendered and written successfully |
| 1 | INVALID_KIND | `--kind` absent or not `implementation`/`backlog` |
| 2 | OPERATIONAL_ERROR | template missing, write permission denied, path traversal detected |
| 3 | INVALID_SCOPE | `--story-id` absent for `--kind=implementation`; `--epic-id` absent for `--kind=backlog` |

## Fail-Open Contract (RULE-004) — Quick Reference

Permitted placeholders (when source artifacts are absent):
- `(pending)` — review artifacts not yet produced
- `(not yet run)` — verify gate not yet executed
- `(no telemetry available)` — events.ndjson absent or empty
- `(none)` — no tasks or commits found
- `(no commits)` — git log returns empty

**Forbidden placeholder values:** `story-XXXX-YYYY`, `EPIC-XXXX`, any `story-[0-9]{4}-[0-9]{4}` literal — these would trigger `audit-pr-evidence.sh`.

Full forbidden-pattern list in [`references/full-protocol.md`](references/full-protocol.md) §Fail-Open Contract.

## Triggers

This skill is internal — never typed by a user. Callers use Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-internal-render-pr-body",
      args: "--kind=implementation --story-id story-XXXX-YYYY --out ${REPO_ROOT}/ai/epics/epic-XXXX/reports/pr-body-story-XXXX-YYYY.md")
```

## Workflow Overview

```text
Phase 0: ARGS       -> validate --kind/--story-id/--epic-id/--out; canonicalize --out vs REPO_ROOT
Phase 1: GATHER     -> read 7 sources (story md, specialist review, tech-lead review, verify envelope, completion report, commits, orchestrator metadata)
Phase 1.5: BACKLOG  -> when --kind=backlog: read epic md, IMPLEMENTATION-MAP, spec, coordinations (replaces sources of Phase 1)
Phase 2: TELEMETRY  -> scripts/telemetry-consolidate.sh; fail-open on exit 1/2
Phase 3: RENDER     -> select _TEMPLATE-PR-{IMPLEMENTATION|BACKLOG}.md; delegate to x-internal-write-report
Phase 4: WRITE      -> atomic mv from .claude/tmp/ to --out; emit final path on stdout
```

Each phase's detailed bash (argument parsing with regex validation, 7-source gather with YAML-when-available + grep fallback, backlog source variants, telemetry consolidation with NO_TELEMETRY fail-open, template selection + INLINE-SKILL delegation, atomic mv) lives in [`references/full-protocol.md`](references/full-protocol.md):

- **Phase 0** (§Phase 0): argument parsing; `--kind` enum validation; scope-required check; `^story-[0-9]{4}-[0-9]{4}$` regex; `realpath` canonicalization with path-traversal guard.
- **Phase 1** (§Phase 1): 7 sources (Story markdown → AC list; Specialist review → verdict via YAML-when-available + regex fallback; Tech-lead review → decision; Verify envelope → testsResult/coverage; Completion report → task IDs; `git log` → commits; Orchestrator metadata → sha/skill/artifacts). String escaping rules for JSON injection.
- **Phase 1.5** (§Phase 1.5): backlog-specific gather (Epic md → summary/stories/successMetrics/outOfScope; IMPLEMENTATION-MAP → critical path/parallelism/phases; Spec → path/sha; Coordinations → in-progress epics scan).
- **Phase 2** (§Phase 2): `telemetry-consolidate.sh` invocation with 3-state exit handling (0=parse, 1=NO_TELEMETRY placeholder, 2=OPERATIONAL_ERROR placeholder); ms→minutes floor conversion.
- **Phase 3** (§Phase 3): template selection per `--kind`; `x-internal-write-report` Skill INLINE-SKILL invocation; temp file under `.claude/tmp/`.
- **Phase 4** (§Phase 4): atomic `mv` with disk-full / permission error handling; stdout = final path.

## Error Handling

| Scenario | Action |
| :--- | :--- |
| `--kind` absent/invalid | Exit 1 `INVALID_KIND` |
| `--story-id` absent for implementation | Exit 3 `INVALID_SCOPE` |
| `--epic-id` absent for backlog | Exit 3 `INVALID_SCOPE` |
| `--out` path traversal | Exit 2 `OPERATIONAL_ERROR: path traversal` |
| Template file missing | Exit 2 `OPERATIONAL_ERROR: template missing` |
| Write permission denied / disk full | Exit 2 `OPERATIONAL_ERROR: write permission denied` |
| `telemetry-consolidate.sh` exit 1/2 | WARNING + placeholder (continue) |
| Any artifact absent | Placeholder per RULE-004 (continue) |

## Integration Notes

| Skill | Relationship |
| :--- | :--- |
| `x-create-pr` | Primary caller for `--kind=implementation` |
| `x-create-feature` | Caller for `--kind=backlog` (story-0066-0006) — RULE-007: backlog PR `## Orchestrator Evidence` is reduced (no Story IDs; accrues in subsequent story PRs) |
| `x-internal-write-report` | Delegate for template rendering (Phase 3) |
| `scripts/telemetry-consolidate.sh` | Telemetry aggregator (Phase 2, story-0066-0002) |
| `_TEMPLATE-PR-IMPLEMENTATION.md` | Template consumed by Phase 3 (story-0066-0001) |
| `_TEMPLATE-PR-BACKLOG.md` | Template for backlog kind (story-0066-0001) |

**Haiku eligibility (Rule 23 §criterion a):** utility without design reasoning — reads files, formats data, writes output. No architectural choices. Upgrade to sonnet/opus must be justified with concrete rationale.

## Full Protocol

Minimum viable contract above. Detailed bash for all 5 phases (argument validation, 7-source implementation gather, 4-source backlog gather, telemetry consolidation with fail-open, template selection + INLINE-SKILL delegation, atomic write) and Fail-Open contract live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
