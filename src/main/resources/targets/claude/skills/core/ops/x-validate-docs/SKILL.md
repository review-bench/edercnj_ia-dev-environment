---
name: x-validate-docs
description: "Documentation freshness gate: validates 6 dimensions (readme, api-specs, grpc-proto, adr, skill-docs, system-architecture) against code changes in a PR. Stack-aware — only validates targets declared/auto-detected from ProjectConfig.documentation.targets. Returns exit non-zero on staleness; produces structured report."
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

# Skill: Documentation Freshness Validator

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

## Validation Workflow

```
1. PARSE       -> Parse args, load ProjectConfig documentation.targets
2. DETECT      -> Identify changed files (git diff for PR or HEAD)
3. CLASSIFY    -> Map changed files to documentation dimensions
4. VALIDATE    -> Run each active dimension validator
5. WINDOW      -> Apply freshness-window-hours grace period
6. REPORT      -> Emit structured report, exit with status
```

### Step 1 — Parse Arguments and Load Targets

Parse invocation arguments. Load `documentation.targets` from project YAML:

```bash
# Read documentation block from project YAML (iadev.yaml or profile.yaml)
cat iadev.yaml 2>/dev/null | grep -A 20 "^documentation:" | head -20
```

If `documentation.targets` is empty (auto-detect mode), apply stack-aware defaults:

| Condition | Auto-detected targets |
|-----------|----------------------|
| `interfaces[].spec=openapi` present in YAML | `readme`, `openapi`, `adr`, `skill-docs` |
| `interfaces[].broker` present (event-driven) | `readme`, `asyncapi`, `adr`, `skill-docs` |
| `interfaces[].spec=proto3` present | `readme`, `grpc-proto`, `adr`, `skill-docs` |
| CLI-only project (no interfaces) | `readme`, `adr`, `skill-docs` |
| `docs/architecture/system.md` exists | add `system-architecture` to all above |

Reject path traversal in targets: any target containing `..` or starting with `/` or not matching `[a-z0-9/_-]+` is rejected with `OPERATIONAL_ERROR`.

### Step 2 — Detect Changed Files

```bash
# PR diff (when --pr-number given)
gh pr diff {pr_number} --name-only 2>/dev/null

# Branch diff against base (fallback)
git diff --name-only origin/develop..HEAD 2>/dev/null || git diff --name-only HEAD~1..HEAD
```

If `--scope` is specified, filter the changed file list to paths under that prefix.

**Security:** Never follow symlinks. Normalize all paths with `realpath --relative-to=.` before processing.

### Step 3 — Classify Changes

Map changed files to documentation dimensions:

| Changed file pattern | Triggers dimension |
|---------------------|-------------------|
| `src/**/` Java/TS/PY/Go files | `readme`, `api-specs` (if REST controller detected), `skill-docs` |
| `*.java` matching `*Controller*\|*Resource*\|*Handler*\|*Endpoint*` | `api-specs` (openapi or asyncapi) |
| `**/*.proto` | `grpc-proto` |
| `docs/adr/**`, story markdown with `ADR Reference:` | `adr` |
| `.claude/skills/**` | `skill-docs` |
| `src/**/` new subdir in `application/\|domain/\|adapter/` | `system-architecture` |
| Only `docs/**` or `*.md` changed (no code) | mark all non-touched dimensions as `skipped (no code change)` |

If all changed files are documentation-only (no code), set overall result to `PASS` (degenerate case — PR is a docs-only PR).

### Step 4 — Validate Each Active Dimension

Run each dimension that is (a) in `documentation.targets` (or auto-detected) AND (b) triggered by the changed files.

#### Dimension: `readme`

```bash
# Check if README.md was touched in the diff
git diff --name-only {range} | grep -i "^README"
```

- **PASS**: README.md exists in diff AND contains at least one of the new symbols/features detected in code changes.
- **FAIL**: Code changes added a new class/skill/config but README.md was NOT touched.
- **SKIP**: Code changes did not affect user-facing features (only internal refactors, test changes).

Heuristic for "user-facing feature": new public class in `adapter/inbound/`, new `SKILL.md`, or new config key in YAML.

#### Dimension: `api-specs` (OpenAPI / AsyncAPI)

**OpenAPI sub-dimension** (active when `interfaces[].spec=openapi`):

```bash
# Detect new endpoints in diff
git diff {range} -- src/ | grep -E "^\+.*(@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@RequestMapping|app\.(get|post|put|delete))"
```

For each new endpoint detected, verify it appears in `openapi.yaml` / `openapi.json` / `docs/api/*.yaml` (by path+method):

```bash
# Check OpenAPI spec contains the endpoint path
grep -r "{endpoint_path}" openapi.yaml docs/api/ 2>/dev/null
```

- **PASS**: All new endpoints appear in OpenAPI spec.
- **FAIL**: At least one new endpoint is not documented in OpenAPI spec.
- **SKIP**: No new endpoints detected in diff.

**AsyncAPI sub-dimension** (active when `interfaces[].broker` present):

```bash
# Detect new event publishers/consumers in diff
git diff {range} -- src/ | grep -E "^\+.*(publish|emit|subscribe|consume|@KafkaListener|@RabbitListener)"
```

Verify new event types appear in `asyncapi.yaml` / `docs/events/*.yaml`.

#### Dimension: `grpc-proto`

Active when `interfaces[].spec=proto3` in project YAML.

```bash
# Check if proto files were modified when gRPC service classes changed
git diff --name-only {range} | grep "\.proto$"
git diff {range} -- src/ | grep -E "^\+.*(extends.*Grpc|@GrpcService|rpc [A-Z])"
```

- **PASS**: New gRPC service methods detected AND corresponding `.proto` file touched.
- **FAIL**: New gRPC service methods detected but NO `.proto` file in diff.
- **SKIP**: No gRPC service changes detected.

#### Dimension: `adr`

Always active.

```bash
# Check if story file references an ADR and whether that ADR exists
find docs/adr/ -name "ADR-*.md" | wc -l
```

For each story touched in the PR (via `ai/epics/*/story-*.md`), verify:
- If story contains `ADR Reference: ADR-XXXX`, that ADR file exists in `docs/adr/`.
- If architectural decision was made (arch plan present), an ADR covers it.

- **PASS**: All ADR references resolve to existing files.
- **FAIL**: ADR reference resolves to non-existent file.
- **SKIP**: No stories or arch plans touched in diff.

#### Dimension: `skill-docs`

Always active when `.claude/skills/` exists.

```bash
# Check new SKILL.md files have required frontmatter
find .claude/skills/ -name "SKILL.md" -newer HEAD~1 2>/dev/null
git diff --name-only {range} | grep "SKILL\.md"
```

For each new or modified `SKILL.md`, validate:
- Frontmatter contains: `name`, `description`, `visibility`, `user-invocable`, `requires-capabilities`.
- Body contains `## Triggers` section.
- Body contains at least one usage example.

- **PASS**: All SKILL.md files in diff have valid frontmatter + Triggers section.
- **FAIL**: Missing required frontmatter field or missing Triggers section.
- **SKIP**: No SKILL.md files in diff.

#### Dimension: `system-architecture`

Active when `docs/architecture/system.md` exists.

```bash
# Check for new architectural components in diff
git diff {range} -- src/ | grep -E "^\+.*class [A-Z]" | head -20
find src/ -newer docs/architecture/system.md -name "*.java" 2>/dev/null | grep -E "(application|domain|adapter)" | head -10
```

Detect new subdirectories in architectural layers:
```bash
git diff {range} --name-only | grep -E "^src/.*(application/|domain/|adapter/inbound/|adapter/outbound/)" | awk -F'/' '{print $1"/"$2"/"$3"/"$4}' | sort -u
```

- **PASS**: `docs/architecture/system.md` was touched in diff OR no new architectural component was added.
- **FAIL**: New component detected (new subdir in application/domain/adapter/ with ≥1 Java/TS/PY/Go file) but `system.md` was NOT touched.
- **SKIP**: No architectural changes detected.

When fail detected, optionally invoke `x-update-system-architecture --validate-only` (if EPIC-0070 available):
```
Skill(skill: "x-update-system-architecture", model: "sonnet", args: "--validate-only")
```
[optional]

If `x-update-system-architecture` is unavailable: log `WARN: x-update-system-architecture unavailable — system-architecture dimension evaluated heuristically`.

### Step 5 — Apply Freshness Window

If `freshness-window-hours > 0` (from ProjectConfig or `--freshness-window-hours` flag):

```bash
# Check when code was last committed
git log -1 --format="%ci" -- {changed_code_files}
```

If the code was committed within the freshness window AND doc was NOT updated:
- Downgrade FAIL → WARNING: `"freshness window open — update documentation before merge"`
- Exit 0 with warnings surfaced in report.

If freshness window has expired AND doc was NOT updated:
- Restore FAIL status → exit 1.

### Step 6 — Emit Report

Write the structured report using `_TEMPLATE-DOC-VALIDATE-REPORT.md`. If `--report-path` is given, write to that path. Otherwise print to stdout.

```
Documentation Validation Report
  Story: {story-id or N/A}
  PR:    #{pr-number or N/A}
  Date:  {ISO-8601}

  Overall: PASS | FAIL | WARNING

  Dimensions:
    readme              : OK | FAILED | SKIPPED | WARNING
    api-specs           : OK | FAILED | SKIPPED | N/A (not in stack)
    grpc-proto          : OK | FAILED | SKIPPED | N/A (not in stack)
    adr                 : OK | FAILED | SKIPPED
    skill-docs          : OK | FAILED | SKIPPED
    system-architecture : OK | FAILED | SKIPPED | N/A (system.md absent)

  Failures:
    - {dimension}: {reason} (file: {relative-path})

  Warnings:
    - {dimension}: {message}

  Duration: {seconds}s
```

**Security:** Never include absolute filesystem paths in report output. Use paths relative to repository root. Never emit environment variables, tokens, or CI runner metadata.

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
