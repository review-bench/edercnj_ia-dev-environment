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

# Skill: Changelog Generator v2

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

## Workflow

```
1. DETECT      -> Determine version range from tags; load format config
2. EPICS       -> Collect merged EPIC-0070 v2 epics in range (for Highlights)
3. PARSE       -> Extract commits in range, parse Conventional Commits
4. HIGHLIGHTS  -> Generate narrative Highlights block (hybrid mode only)
5. GROUP       -> Map commit types to KaC sections
6. GENERATE    -> Create or update CHANGELOG.md (incremental)
7. REPORT      -> Summary of entries added
```

### Step 1 — Detect Version Range and Load Config

```bash
# List all version tags, sorted by version
git tag --list 'v*' --sort=-v:refname

# Determine range
# If version tag given: previous_tag..version_tag
# If --unreleased: latest_tag..HEAD
# If --full: process all tags
# If no argument: latest_tag..HEAD (same as --unreleased)
```

Load `documentation.changelog.format` from project YAML:

```bash
cat iadev.yaml 2>/dev/null | grep "changelog" | grep "format"
```

If not found: default to `hybrid`. Apply `--format` flag override if present.

Validate format value: if not in `{hybrid, keep-a-changelog, conventional-only}` → exit 1 `INVALID_CHANGELOG_FORMAT`.

### Step 2 — Collect EPIC-0070 v2 Epics in Range (hybrid mode only)

Identify epic markdown files added/modified in the git range:

```bash
git log {previous_tag}..{target} --diff-filter=AM --name-only -- 'ai/epics/epic-*/epic-*.md' 2>/dev/null
```

For each epic file found, read the `## Entrega de Valor` section (or `## 2. Persona & Cenário de Uso` + `## 3. Hipótese de Valor` as fallback):

```bash
# Extract Entrega de Valor content from epic markdown
awk '/^## (2\. )?Entrega de Valor/,/^## [0-9]/' {epic_file} | head -50
```

Apply v2 template detection: epic uses v2 if it contains `## 1. Visão & Problema` (not `## Packages — Hexagonal`).

**D-R10 — Fallback when no v2 epics found:**
- No epic files in range, OR
- All epic files found are v1 (contain `## Packages — Hexagonal`), OR
- `ai/epics/` directory absent:

```
WARN [x-generate-release-changelog] No EPIC-0070 v2 epics found in range {range} — Highlights will be empty.
     Install/merge EPIC-0070 epics to enable narrative Highlights. (D-R10 degraded mode)
```

Set Highlights to empty; proceed without blocking.

### Step 3 — Parse Conventional Commits

```bash
# Extract commits in range
git log {previous_tag}..{target} --format="%H|%s|%b|%an|%ai" --no-merges
```

Parse each commit as Conventional Commit:
```
<type>(<scope>): <description>
```

Extract: type, scope, description, breaking flag (`!` after type or `BREAKING CHANGE:` in body), body.

Skip commits that don't follow the convention (log WARN, do not abort).

### Step 4 — Generate Highlights Block (hybrid mode only)

When `format = hybrid` and ≥ 1 v2 epic was found:

1. For each v2 epic with `## Entrega de Valor`, extract the section content (sanitized).
2. Consolidate across epics into a narrative block targeting 3–8 paragraphs:
   - Each epic contributes up to 2 paragraphs (value statement + metric).
   - Remaining paragraph budget filled with top `feat:` entries summarized by scope.
3. **Minimum (3 paragraphs):** if fewer than 3 paragraphs can be derived from epic content, expand with bullet-point summaries of `feat:` commits until minimum is reached.
4. **Maximum (8 paragraphs):** if more than 8 paragraphs would be generated, truncate with trailing `...` and emit:
   ```
   WARN [x-generate-release-changelog] Highlights truncated at 8 paragraphs — review manually.
   ```

**Content sanitization:** strip markdown link syntax (`[text](url)`) that references internal CI paths. Escape `<`, `>`, `&` characters. Never include paths matching `/home/`, `/tmp/`, `/var/`, `/Users/` (CI runner internals). Remove environment variables (`$VAR`, `${VAR}`).

**Highlights format:**
```markdown
### Highlights

{paragraph_1 — value statement for epic 1}

{paragraph_2 — metric/impact for epic 1}

{paragraph_3 — value statement for epic 2 or feat: summary}
```

When `format = keep-a-changelog` or `conventional-only`: skip this step (no Highlights block).

### Step 5 — Map to Changelog Sections

| Commit Type | Hybrid / KaC Section | Conventional-only |
|------------|---------------------|-------------------|
| `feat` | **Added** | `feat` |
| `fix` | **Fixed** | `fix` |
| `refactor` | **Changed** | `refactor` |
| `perf` | **Changed** | `perf` |
| `docs` | **Documentation** (optional) | `docs` |
| `chore` | *(skip)* | `chore` |
| `build`, `ci`, `test`, `style` | *(skip)* | *(skip)* |
| `BREAKING CHANGE` / `feat!` / `fix!` | **Breaking Changes** (top) | `breaking` |
| `deprecate` | **Deprecated** | `deprecate` |
| `revert` | **Removed** | `revert` |

Section order (hybrid + keep-a-changelog): Breaking Changes > Highlights (hybrid only) > Added > Changed > Deprecated > Removed > Fixed > Security.

### Step 6 — Generate CHANGELOG.md

**Template:** uses `_TEMPLATE-CHANGELOG-ENTRY.md` to render each version entry.

**If CHANGELOG.md does not exist**, create with header:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
```

**If CHANGELOG.md exists**, insert new version section after header / after `[Unreleased]`:

```markdown
## [{version}] — YYYY-MM-DD

### Highlights

{highlights_block}

### Breaking Changes

- {breaking entries}

### Added

- {feat entries}

### Changed

- {refactor/perf entries}

### Fixed

- {fix entries}
```

Only include sections that have entries (omit empty sections). Update `[Unreleased]` section (clear entries that moved to this version).

**Entry format:**
```markdown
- {description} ([#{PR_number}]({PR_url})) — when PR linkable
- {description} ({commit_hash_short}) — when no PR
```

**Version comparison links** (bottom of CHANGELOG):
```markdown
[unreleased]: https://github.com/{owner}/{repo}/compare/{latest_tag}...HEAD
[3.10.0]: https://github.com/{owner}/{repo}/compare/v3.9.0...v3.10.0
```

### Step 7 — Report

```
CHANGELOG.md updated:
  Version:    {version or Unreleased}
  Format:     {hybrid | keep-a-changelog | conventional-only}
  Epics v2:   {count found} (Highlights: {paragraphs} paragraphs)
  Entries:    {count}
  Sections:   Breaking ({n}), Added ({n}), Changed ({n}), Fixed ({n})
  Range:      {from_tag}..{to_ref}
  Warnings:   {any D-R10 or truncation warnings}
```

## D-R10 — Degraded Mode (EPIC-0070 Unavailable)

When v2 epic content is absent (no `ai/epics/` directory, no v2 epics in range, or EPIC-0070 not installed):

1. Emit visible WARN: `"EPIC-0070 not available or no v2 epics in range — Highlights manual or degraded"`.
2. Generate changelog WITHOUT Highlights block (or with empty Highlights placeholder).
3. Proceed with Keep-a-Changelog sections from Conventional Commits (always available).
4. **Exit 0 — never block release.**

D-R10 degraded output:
```markdown
## [v3.10.0] — YYYY-MM-DD

<!-- WARN: Highlights not generated (EPIC-0070 v2 epics not found in range). Update manually. -->

### Added
- {feat entries}
```

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
