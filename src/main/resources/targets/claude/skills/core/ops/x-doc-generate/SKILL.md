---
name: x-doc-generate
description: "Documentation automation v2: stack-aware generation consuming documentation.targets from ProjectConfig. Detects documentation type needed (API, README, ADR, changelog, system-architecture) from code changes and stack config, delegates to specialized skills or generates inline. Invokes x-arch-system-update on architectural change detection."
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[--type api|readme|adr|changelog|all] [--scope path] [--force] [--dry-run] [--legacy-v1]"
requires-capabilities: [governance.doc-as-dod]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Documentation Automation v2

## Purpose

Single entry point for generating and updating all project documentation for {{PROJECT_NAME}}. Operates in two modes:

- **`--target-stack-aware` (default, v2):** Reads `documentation.targets` from ProjectConfig (story-0071-0001) and generates only targets relevant to the project stack. Invokes `x-arch-system-update` (EPIC-0070) when architectural changes are detected.
- **`--legacy-v1` (deprecated, removed in 2 releases):** Original behavior — uniform auto-detection from `git diff` without stack awareness. Emits visible deprecation warning.

## Triggers

- `/x-doc-generate` — stack-aware auto-detect from `documentation.targets` (v2 default)
- `/x-doc-generate --type api` — generate or update API documentation (OpenAPI/AsyncAPI/gRPC)
- `/x-doc-generate --type readme` — update project README.md
- `/x-doc-generate --type adr` — delegate to `x-adr-generate`
- `/x-doc-generate --type changelog` — delegate to `x-release-changelog`
- `/x-doc-generate --type all` — process all applicable documentation targets
- `/x-doc-generate --type all --force` — regenerate all regardless of change status
- `/x-doc-generate --dry-run` — list what would be updated, without writing
- `/x-doc-generate --legacy-v1` — **DEPRECATED** v1 behavior (uniform auto-detect, no stack awareness)

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | Enum | No | Documentation type: `api`, `readme`, `adr`, `changelog`, `all`. Default: auto-detect from stack config + git diff. |
| `--scope` | String | No | Path to limit change analysis (e.g., `src/main/java/com/example/api/`). |
| `--force` | Flag | No | Regenerate even if no changes detected. |
| `--dry-run` | Flag | No | List what would be updated without writing any file. |
| `--legacy-v1` | Flag | No | **DEPRECATED** — emits WARNING: "v1 behavior deprecated; will be removed in 2 releases". Mutually exclusive with `--target-stack-aware`. |

**Flag conflict:** passing both `--legacy-v1` and `--target-stack-aware` in the same invocation exits with `FLAG_CONFLICT` and lists both flags as mutually exclusive.

## Workflow

```
1. PARSE      -> Parse arguments; detect mode (v2 default vs --legacy-v1)
2. LOAD       -> Load documentation.targets from ProjectConfig YAML
3. DETECT     -> Analyze git diff filtered by active targets
4. ARCH       -> Detect architectural changes; invoke x-arch-system-update if needed
5. DISPATCH   -> Delegate to specialized skills or generate inline
6. VERIFY     -> Idempotency check (skip if identical)
7. REPORT     -> Summary of documentation actions taken
```

### Step 1 — Parse Arguments

Detect mode:

1. If `--legacy-v1` AND `--target-stack-aware` both present → exit `FLAG_CONFLICT` immediately.
2. If `--legacy-v1` → emit to stderr:
   ```
   WARN [x-doc-generate] --legacy-v1 behavior deprecated (EPIC-0071, Rule 19 §Skill Renaming).
        Will be removed in 2 releases. Remove --legacy-v1 to use stack-aware v2 default.
   ```
   Then proceed with v1 workflow (§v1 Fallback below).
3. Default (no flag or `--target-stack-aware`) → proceed with v2 stack-aware workflow.

### Step 2 — Load documentation.targets (v2 mode)

Read the `documentation` block from project YAML (`iadev.yaml` or `profile.yaml`):

```bash
cat iadev.yaml 2>/dev/null | grep -A 20 "^documentation:" | head -20
```

If `documentation.targets` is empty or YAML not found → apply auto-detect defaults based on stack:

| Stack indicator | Auto-detected targets |
|-----------------|----------------------|
| `interfaces[].spec=openapi` | `readme`, `openapi`, `adr`, `skill-docs`, `system-architecture`* |
| `interfaces[].broker` present | `readme`, `asyncapi`, `adr`, `skill-docs`, `system-architecture`* |
| `interfaces[].spec=proto3` | `readme`, `grpc-proto`, `adr`, `skill-docs`, `system-architecture`* |
| CLI-only | `readme`, `adr`, `skill-docs`, `system-architecture`* |

*`system-architecture` only auto-detected when `docs/architecture/system.md` exists.

**Security validation:** reject any target containing `..` or absolute path. Exit `OPERATIONAL_ERROR` if path traversal detected in targets list.

### Step 3 — Detect Changes (stack-filtered)

```bash
git diff --name-only HEAD 2>/dev/null
git diff --name-only --cached
git ls-files --others --exclude-standard
```

Filter changed files to only those relevant to active targets. If `--scope` is specified, further limit to that path prefix. If `--force` is NOT set and no relevant changes found → log "No documentation updates needed for active targets" and exit 0.

### Step 4 — Architectural Change Detection

Detect new architectural components added in the diff:

```bash
# New subdirectories in architectural layers
git diff --name-only HEAD | grep -E "^src/.*(application/|domain/|adapter/inbound/|adapter/outbound/)" | \
  awk -F'/' '{print $1"/"$2"/"$3"/"$4}' | sort -u
```

A **new architectural component** is: a new subdirectory (not previously in `git log HEAD~1`) under `application/`, `domain/`, `adapter/inbound/<protocol>/`, or `adapter/outbound/<technology>/` containing ≥ 1 file matching `*.java|*.ts|*.py|*.go`.

Exclude rename-only moves (detect via `git diff --diff-filter=R`).

**If new architectural component detected AND `system-architecture` is in active targets:**

Invoke `x-arch-system-update` via INLINE-SKILL (Rule 13 Pattern 1):

```
Invoke the `x-arch-system-update` skill via the Skill tool:

    Skill(skill: "x-arch-system-update", model: "sonnet", args: "--component {new_component_name}")
```
[conditional: flag.arch_change_detected]

If `x-arch-system-update` is unavailable (skill not found, EPIC-0070 not present):
```
WARN [x-doc-generate] x-arch-system-update unavailable — system.md not updated. Install EPIC-0070 to enable.
```
Proceed without blocking.

### Step 5 — Dispatch Documentation Generation

#### 5A — Changelog (`--type changelog`)

Invoke `x-release-changelog` via the Skill tool:

```
Invoke the `x-release-changelog` skill via the Skill tool:

    Skill(skill: "x-release-changelog", model: "sonnet", args: "--unreleased")
```
[required]

#### 5B — ADR (`--type adr`)

Invoke `x-adr-generate` via the Skill tool:

```
Invoke the `x-adr-generate` skill via the Skill tool:

    Skill(skill: "x-adr-generate", model: "sonnet", args: "{architecture-plan-path} {story-id}")
```
[optional]

If no architecture plan found: log "No architecture plan found for ADR generation".

#### 5C — API Documentation (`--type api`)

Generate or update API documentation based on active targets:

**OpenAPI** (when `openapi` in active targets):
1. Detect new REST endpoints in diff (Spring: `@GetMapping`, `@PostMapping`, `@RequestMapping`; Express: `app.get()`, `app.post()`).
2. For each new endpoint, check if it appears in `openapi.yaml` / `openapi.json` / `docs/api/*.yaml`.
3. If missing: add the endpoint stub to the OpenAPI spec.

**AsyncAPI** (when `asyncapi` in active targets):
1. Detect new event publishers/consumers (`@KafkaListener`, `@RabbitListener`, `publish()`, `emit()`).
2. Verify or add event schema entries in `asyncapi.yaml` / `docs/events/*.yaml`.

**gRPC Proto** (when `grpc-proto` in active targets):
1. Detect new service methods in `*.proto` files or gRPC service classes.
2. Update or create the corresponding `.proto` service definition.

**Security:** Normalize all file paths. Never follow symlinks. Reject paths with `..`.

#### 5D — README (`--type readme`)

Update project README.md:
1. Read current README.md (create from template if missing).
2. Detect what changed: new skills → update Skills section; new endpoints → update API section; new config → update Configuration section.
3. Apply targeted updates without removing existing content.
4. Idempotency: compare before writing — skip if no change.

#### 5E — All (`--type all`)

Process all active documentation targets. For each target in `documentation.targets` (or auto-detected list), dispatch as described above. Skip targets with no applicable changes (unless `--force`).

### Step 6 — Verify (Idempotency)

After each generation:
1. Confirm no duplicate content was introduced.
2. Confirm files were actually modified (or correctly skipped).
3. For markdown files: basic structural validation (no broken headings, no unclosed code blocks).

### Step 7 — Report

```
Documentation generation complete:
  Mode:          stack-aware (v2) | legacy-v1 DEPRECATED
  Targets:       {active targets list}
  Files updated:
    - {relative/path/file.md} ({type})
  Delegated to:
    - x-release-changelog (changelog)
    - x-adr-generate (adr)
    - x-arch-system-update (system-architecture)
  Skipped:       {types skipped with reason}
  Warnings:      {any warnings emitted}
  Duration:      {time}s
```

## v1 Fallback (`--legacy-v1` mode)

When `--legacy-v1` is active, the skill uses the original v1 detection logic:

1. Run `git diff --name-only` for all files (no stack filtering).
2. Apply original auto-detection rules:

| File Pattern | Inferred Type |
|-------------|---------------|
| `*Controller*`, `*Resource*`, `*Handler*`, `*Endpoint*`, `*Route*` | `api` |
| `*ADR*`, `*Decision*`, `architecture*`, `*adr*` | `adr` |
| Any commits since last tag (Conventional Commits) | `changelog` |
| `SKILL.md`, `README*`, `config*`, `setup*`, `*.yaml` (config) | `readme` |
| No matches | No action |

3. Process all detected types (no stack filter, no `x-arch-system-update`).
4. No Highlights block in changelog (v1 format only).

The `--legacy-v1` flag and v1 code path will be **removed in 2 releases** per Rule 19 §Skill Renaming. Operators on automated pipelines should migrate to the v2 default before the removal release.

## Error Handling

| Scenario | Action |
|----------|--------|
| No git repository | Exit `OPERATIONAL_ERROR: not a git repository` |
| `--legacy-v1` and `--target-stack-aware` both present | Exit `FLAG_CONFLICT` listing both flags |
| Project YAML not found | Use auto-detect mode with WARN |
| Delegated skill not available | Log warning, skip that type, continue |
| Path traversal in `documentation.targets` | Exit `PATH_TRAVERSAL_REJECTED` |
| Symlink encountered | Skip, log WARN, never follow |
| Target file is read-only | Report error for that file, continue with others |
| `x-arch-system-update` unavailable | Log WARN, continue without arch update |

## Performance Contract

- Total generation time: < 60s for projects with 20+ targets and 10+ skill files.
- `--dry-run` adds no I/O overhead (read-only diff analysis).

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-release-changelog` | delegates-to | Changelog generation is fully delegated |
| `x-adr-generate` | delegates-to | ADR generation from architecture plans |
| `x-arch-system-update` | delegates-to (conditional) | On architectural change detection; EPIC-0070 |
| `x-doc-validate` | followed-by (Phase 3) | generate runs first; validate confirms freshness |
| `x-story-implement` | called-by | Phase 3 (Documentation) — story-0071-0006 wires both generate + validate |
