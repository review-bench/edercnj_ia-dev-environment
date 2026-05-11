---
name: x-validate-docs
description: "Documentation freshness gate: validates 6 dimensions (readme, api, adr, etc.) per PR."
visibility: public
user-invocable: true
model: sonnet
allowed-tools: Read, Bash, Grep, Glob
argument-hint: "[--story-id STORY-ID] [--pr-number N] [--scope path] [--report-path path]"
requires-capabilities: [governance.doc-as-dod]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Documentation Freshness Validator (slim — ADR-0012)

> 🔒 Read-only skill — does NOT modify any files. Produces a structured report and exits with a status code.

## Purpose

Validates that documentation has been updated in sync with code changes introduced by a PR or branch. Consumes `documentation.targets` from `ProjectConfig` (introduced in story-0071-0001) to determine which dimensions to validate — only targets declared or auto-detected for the project stack are checked.

Produces a report at the path specified by `--report-path` (default: stdout). Exits `0` on pass; exits `1` (`DOC_VALIDATION_FAILED`) on any failed dimension.

## Triggers

- `/x-validate-docs` — validate all active dimensions from `documentation.targets`
- `/x-validate-docs --story-id story-XXXX-YYYY` — validate for a specific story's PR
- `/x-validate-docs --pr-number 123` — validate against PR #123 diff
- `/x-validate-docs --scope src/main/java/` — limit code change analysis to path
- `/x-validate-docs --report-path ai/epics/epic-XXXX/reports/doc-validate-STORY-ID.md` — persist report

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--story-id` | String | No | Story ID (e.g., `story-0071-0002`). Used to derive report path if `--report-path` omitted. |
| `--pr-number` | Integer | No | PR number. Limits diff to merged PR if specified. |
| `--scope` | String | No | Path prefix to limit change detection (e.g., `src/main/java/`). |
| `--report-path` | String | No | Output path for report. Default: print to stdout. |
| `--freshness-window-hours` | Integer | No | Override freshness window from ProjectConfig (0 = immediate, default). |

## Exit Codes

| Exit | Code | Condition |
|------|------|-----------|
| 0 | `OK` | All active dimensions passed or skipped. |
| 1 | `DOC_VALIDATION_FAILED` | At least one dimension detected staleness. |
| 2 | `OPERATIONAL_ERROR` | Missing dependency, invalid args, or I/O error. |

## Validation Dimensions

| Dimension | Trigger | What it validates |
|-----------|---------|-------------------|
| `readme` | New public class / SKILL / config key | README.md touched in diff |
| `api-specs` (openapi) | New `@GetMapping`/`@PostMapping`/etc | Endpoint declared in `openapi.yaml` |
| `api-specs` (asyncapi) | New `publish`/`emit`/`@KafkaListener` | Event in `asyncapi.yaml` |
| `grpc-proto` | New `@GrpcService` or `rpc X` | Matching `.proto` file in diff |
| `adr` | Story references ADR-XXXX or arch plan exists | ADR file present in `docs/adr/` |
| `skill-docs` | `SKILL.md` in diff | Required frontmatter + `## Triggers` section |
| `system-architecture` | New subdir in `application/`/`domain/`/`adapter/` | `docs/architecture/system.md` touched |

## Workflow Overview

```text
1. PARSE     -> Parse args; load documentation.targets from iadev.yaml (auto-detect on absent)
2. DETECT    -> Identify changed files (gh pr diff --pr-number OR git diff)
3. CLASSIFY  -> Map changed files to active dimensions
4. VALIDATE  -> Run each active dimension validator
5. WINDOW    -> Apply --freshness-window-hours grace (downgrade FAIL→WARN within window)
6. REPORT    -> Emit Markdown report; exit 0|1|2 per outcome
```

Per-dimension detection logic with `git diff` snippets, auto-detect rules per stack, freshness-window semantics, and full report template in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): YAML target loading; 5-row stack-aware auto-detect table; path-traversal rejection regex.
- **Step 2** (§Step 2): `gh pr diff --name-only` (when `--pr-number`) vs `git diff --name-only origin/develop..HEAD` fallback; `--scope` prefix filter; symlink-skip security rule.
- **Step 3** (§Step 3): 7-row change-classification table mapping file patterns to dimensions; docs-only PR degenerate case (overall PASS).
- **Step 4** (§Step 4 sub-dimensions): full bash for `readme`/`api-specs (openapi+asyncapi)`/`grpc-proto`/`adr`/`skill-docs`/`system-architecture`; PASS/FAIL/SKIP semantics per dimension; optional `x-update-system-architecture --validate-only` delegation.
- **Step 5** (§Step 5): `git log -1 --format="%ci"` against changed code; FAIL→WARNING downgrade within window; FAIL restored on expiry.
- **Step 6** (§Step 6): `_TEMPLATE-DOC-VALIDATE-REPORT.md` consumption; full report template with Overall verdict, per-dimension status, Failures + Warnings sections; security rule (no absolute paths in report).

## Error Handling

| Scenario | Action |
|----------|--------|
| No git repository | Exit 2 `OPERATIONAL_ERROR: not a git repository` |
| `--pr-number` but `gh` not on PATH | Fall back to `git diff HEAD~1..HEAD` with WARN |
| Project YAML not found | Use auto-detect mode for all targets with WARN |
| Target dimension validator throws error | Mark dimension `ERROR`, continue others, exit 1 |
| `documentation.targets` contains path traversal | Exit 2 `PATH_TRAVERSAL_REJECTED: {target}` |
| Symlink encountered in file scan | Skip symlink, log WARN, never follow |

## Performance Contract

- Total validation time: < 30s for repositories with up to 500 changed files and 6 active dimensions.
- No network calls during validation (all checks are local git + filesystem).
- Each dimension validator runs independently; no shared mutable state.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-implement-story` | called-by | Phase 3 — mandatory gate (story-0071-0006 wires this) |
| `x-generate-docs` | precedes (in Phase 3) | generate runs first, validate confirms |
| `x-update-system-architecture` | optional-delegates-to | system-architecture dimension, EPIC-0070 |
| `audit-doc-freshness.sh` | CI counterpart | story-0071-0005 wires CI version of same checks |

## Full Protocol

Minimum viable contract above. Detailed Step 1–6 bash (auto-detect rules, per-dimension `git diff` + grep patterns, freshness-window grace, full report template), error handling matrix, and performance contract live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
