---
name: x-generate-docs
description: "Documentation automation v2: stack-aware generation from documentation.targets."
user-invocable: true
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[--type api|readme|adr|changelog|all] [--scope path] [--force] [--dry-run]"
requires-capabilities: [governance.doc-as-dod]
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Documentation Automation v2

## Purpose

Single entry point for generating and updating all project documentation for {{PROJECT_NAME}}. Operates in two modes:

- **`--target-stack-aware` (default, v2):** Reads `documentation.targets` from ProjectConfig (story-0071-0001) and generates only targets relevant to the project stack. Invokes `x-update-system-architecture` (EPIC-0070) when architectural changes are detected.

## Triggers

- `/x-generate-docs` — stack-aware auto-detect from `documentation.targets` (v2 default)
- `/x-generate-docs --type api` — generate or update API documentation (OpenAPI/AsyncAPI/gRPC)
- `/x-generate-docs --type readme` — update project README.md
- `/x-generate-docs --type adr` — delegate to `x-generate-adr`
- `/x-generate-docs --type changelog` — delegate to `x-generate-release-changelog`
- `/x-generate-docs --type all` — process all applicable documentation targets
- `/x-generate-docs --type all --force` — regenerate all regardless of change status
- `/x-generate-docs --dry-run` — list what would be updated, without writing

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `--type` | Enum | No | Documentation type: `api`, `readme`, `adr`, `changelog`, `all`. Default: auto-detect from stack config + git diff. |
| `--scope` | String | No | Path to limit change analysis (e.g., `src/main/java/com/example/api/`). |
| `--force` | Flag | No | Regenerate even if no changes detected. |
| `--dry-run` | Flag | No | List what would be updated without writing any file. |
| `--target-stack-aware` | Flag | No | **(default)** Reads `documentation.targets` from ProjectConfig and generates only targets relevant to the project stack. Invokes `x-update-system-architecture` when architectural changes detected. |

## Workflow

```
1. PARSE      -> Parse arguments
2. LOAD       -> Load documentation.targets from ProjectConfig YAML
3. DETECT     -> Analyze git diff filtered by active targets
4. ARCH       -> Detect architectural changes; invoke x-update-system-architecture if needed
5. DISPATCH   -> Delegate to specialized skills or generate inline
6. VERIFY     -> Idempotency check (skip if identical)
7. REPORT     -> Summary of documentation actions taken
```

### Step 1 — Parse Arguments

Detect mode:

1. Default (no flag or `--target-stack-aware`) → proceed with v2 stack-aware workflow.

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

Invoke `x-update-system-architecture` via INLINE-SKILL (Rule 13 Pattern 1):

```
Invoke the `x-update-system-architecture` skill via the Skill tool:

    Skill(skill: "x-update-system-architecture", model: "sonnet", args: "--component {new_component_name}")
```
[conditional: flag.arch_change_detected]

If `x-update-system-architecture` is unavailable (skill not found, EPIC-0070 not present):
```
WARN [x-generate-docs] x-update-system-architecture unavailable — system.md not updated. Install EPIC-0070 to enable.
```
Proceed without blocking.

### Step 5 — Dispatch Documentation Generation

#### 5A — Changelog (`--type changelog`)

Invoke `x-generate-release-changelog` via the Skill tool:

```
Invoke the `x-generate-release-changelog` skill via the Skill tool:

    Skill(skill: "x-generate-release-changelog", model: "sonnet", args: "--unreleased")
```
[required]

#### 5B — ADR (`--type adr`)

Invoke `x-generate-adr` via the Skill tool:

```
Invoke the `x-generate-adr` skill via the Skill tool:

    Skill(skill: "x-generate-adr", model: "sonnet", args: "{architecture-plan-path} {story-id}")
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
  Mode:          stack-aware (v2)
  Targets:       {active targets list}
  Files updated:
    - {relative/path/file.md} ({type})
  Delegated to:
    - x-generate-release-changelog (changelog)
    - x-generate-adr (adr)
    - x-update-system-architecture (system-architecture)
  Skipped:       {types skipped with reason}
  Warnings:      {any warnings emitted}
  Duration:      {time}s
```

## Error Handling

| Scenario | Action |
|----------|--------|
| No git repository | Exit `OPERATIONAL_ERROR: not a git repository` |
| Project YAML not found | Use auto-detect mode with WARN |
| Delegated skill not available | Log warning, skip that type, continue |
| Path traversal in `documentation.targets` | Exit `PATH_TRAVERSAL_REJECTED` |
| Symlink encountered | Skip, log WARN, never follow |
| Target file is read-only | Report error for that file, continue with others |
| `x-update-system-architecture` unavailable | Log WARN, continue without arch update |

## Performance Contract

- Total generation time: < 60s for projects with 20+ targets and 10+ skill files.
- `--dry-run` adds no I/O overhead (read-only diff analysis).

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-generate-release-changelog` | delegates-to | Changelog generation is fully delegated |
| `x-generate-adr` | delegates-to | ADR generation from architecture plans |
| `x-update-system-architecture` | delegates-to (conditional) | On architectural change detection; EPIC-0070 |
| `x-validate-docs` | followed-by (Phase 3) | generate runs first; validate confirms freshness |
| `x-implement-story` | called-by | Phase 3 (Documentation) — story-0071-0006 wires both generate + validate |
