---
name: x-generate-release-changelog
description: "Hybrid changelog: narrative Highlights + Keep-a-Changelog sections from v2 epics."
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[version-tag | --unreleased | --full] [--format hybrid|keep-a-changelog|conventional-only]"
requires-capabilities: [governance.doc-as-dod]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Changelog Generator v2 (slim — ADR-0012)

## Purpose

Generates or updates `CHANGELOG.md` for {{PROJECT_NAME}} in **hybrid format** (default): a narrative `### Highlights` block derived from `## Entrega de Valor` sections of merged EPIC-0070 v2 epics, followed by rigorously structured Keep-a-Changelog sections populated from Conventional Commits.

Format is configurable via `documentation.changelog.format` in the project YAML:
- `hybrid` (default): Highlights block + Keep-a-Changelog sections
- `keep-a-changelog`: Keep-a-Changelog only (no Highlights), same as v1 behavior
- `conventional-only`: Raw Conventional Commit grouping without KaC normalization

When v2 epic content is unavailable, degrades gracefully per **D-R10**: empty Highlights + visible WARN + exit 0 (never blocks release).

## Triggers

- `/x-generate-release-changelog` — generate hybrid changelog for unreleased changes
- `/x-generate-release-changelog v3.10.0` — generate entry for version v3.10.0
- `/x-generate-release-changelog --unreleased` — unreleased changes since last tag
- `/x-generate-release-changelog --full` — regenerate entire changelog from all tags
- `/x-generate-release-changelog --format keep-a-changelog` — v1 format (no Highlights)
- `/x-generate-release-changelog --format conventional-only` — raw commit grouping

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| Version tag | No | Target version (e.g., `v3.10.0`). Default: unreleased. |
| `--unreleased` | No | Only unreleased changes since last tag. |
| `--full` | No | Regenerate entire changelog from all tags. |
| `--format` | No | Output format: `hybrid` (default), `keep-a-changelog`, `conventional-only`. Overrides `documentation.changelog.format` from YAML. |
| `--highlights-min` | No | Minimum Highlights paragraphs. Default: 3. |
| `--highlights-max` | No | Maximum Highlights paragraphs before truncation. Default: 8. |

**Format validation:** if `documentation.changelog.format` or `--format` contains a value outside `{hybrid, keep-a-changelog, conventional-only}`, exit `INVALID_CHANGELOG_FORMAT` and list valid values on stderr.

## Output Contract

| Artifact | Path |
|----------|------|
| Changelog | `CHANGELOG.md` (created if absent, otherwise updated in place) |
| Section order | Breaking Changes → Highlights (hybrid only) → Added → Changed → Deprecated → Removed → Fixed → Security |
| Empty sections | Omitted |
| `[Unreleased]` | Cleared when entries move into the new version |
| Comparison links | Appended to file footer (`[unreleased]`, `[X.Y.Z]`) |

## Workflow Overview

```text
1. DETECT     -> Resolve version range from git tags; load documentation.changelog.format (default: hybrid)
2. EPICS      -> Collect EPIC-0070 v2 epics in range; extract `## Entrega de Valor` (hybrid mode only)
3. PARSE      -> git log + Conventional Commit parsing (type/scope/description/breaking flag)
4. HIGHLIGHTS -> Generate 3–8 paragraph narrative (truncate >8 with WARN; expand with feat: when <3)
5. GROUP      -> Map commit types to KaC sections (feat→Added, fix→Fixed, refactor/perf→Changed, etc.)
6. GENERATE   -> Render new version section into CHANGELOG.md (append or insert after [Unreleased])
7. REPORT     -> Summary with version / format / epic count / paragraph count / entry counts / warnings
```

Detailed step bash, EPIC-0070 detection rules, Conventional Commit type → KaC section mapping table, content sanitization rules, Highlights paragraph budget, and CHANGELOG.md template live in [`references/full-protocol.md`](references/full-protocol.md):

- **Step 1** (§Step 1): `git tag --list 'v*' --sort=-v:refname`; range computation per argument; YAML format load with validation.
- **Step 2** (§Step 2): `git log --diff-filter=AM --name-only -- 'ai/epics/epic-*/epic-*.md'`; v2 detection via `## 1. Visão & Problema` marker; `awk` extraction of `## Entrega de Valor` capped at 50 lines per epic.
- **Step 3** (§Step 3): `git log --format` parsing; Conventional Commit regex; breaking-flag detection (`!` after type or `BREAKING CHANGE:` in body).
- **Step 4** (§Step 4): per-epic 2-paragraph budget (value statement + metric); 3-min / 8-max paragraph clamp with WARN-on-truncate; content sanitization rules (strip CI paths `/home/`, `/tmp/`, `/var/`, `/Users/`; remove `$VAR`; escape `<`/`>`/`&`).
- **Step 5** (§Step 5): full 10-row Commit Type → KaC Section mapping; section ordering.
- **Step 6** (§Step 6): `_TEMPLATE-CHANGELOG-ENTRY.md` rendering; CHANGELOG.md header creation for new repos; insert-after-Unreleased semantics; entry format (PR-linked vs commit-hash); comparison link generation.
- **Step 7** (§Step 7): summary report template.
- **D-R10 Degraded Mode** (§D-R10): WARN message and degraded-output template when EPIC-0070 unavailable.

## Error Handling

| Scenario | Action |
|----------|--------|
| No tags found | Use all commits from initial commit |
| No Conventional Commits in range | Report "No conventional commits found in range" |
| Non-conventional commit | Skip with WARN |
| CHANGELOG.md has unexpected format | Append new section at the end with WARN |
| Version tag already in CHANGELOG | Exit 0 "Version already documented" |
| `format` value invalid | Exit 1 `INVALID_CHANGELOG_FORMAT`; list valid values on stderr |
| `## Entrega de Valor` absent in v2 epic | Skip epic for Highlights; use feat: summaries as filler |
| Content sanitization fails | Strip offending content; log WARN; proceed |

## Performance Contract

- Total generation time: < 60s for ranges with 100+ commits and 10 merged epics.
- Epic file reading: ≤ 50 lines per epic file (only `## Entrega de Valor` section).

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release` | called-by | Release Step 5 changelog generation |
| `x-generate-docs` | called-by | When `--type changelog` delegated |
| `x-internal-write-report` | uses | Template rendering for CHANGELOG entry |

## Full Protocol

Minimum viable contract above. Detailed bash for all 7 steps, EPIC-0070 detection rules, Conventional Commit → KaC section mapping, content sanitization, Highlights paragraph budget with 3–8 clamp, CHANGELOG.md template (header + version section + entry format + comparison links), and D-R10 degraded-mode output live in [`references/full-protocol.md`](references/full-protocol.md) per ADR-0012 (skill body slim-by-default).
