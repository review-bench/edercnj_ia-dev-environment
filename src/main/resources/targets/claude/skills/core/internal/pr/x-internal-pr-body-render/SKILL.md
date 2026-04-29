---
name: x-internal-pr-body-render
description: "Renders PR body Markdown from existing disk artifacts using one of two templates. --kind=implementation: reads review/verify/telemetry artifacts for a story and emits a structured implementation PR body. --kind=backlog: reads epic/map artifacts and emits a scaffolding PR body. Fail-open: absent artifacts produce human-readable placeholders, never audit-sentinel strings. Internal — invoked only by x-pr-create and x-feature-create."
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

# Skill: x-internal-pr-body-render

## Purpose

Renders a PR body Markdown file from existing on-disk artifacts (review, verify, telemetry,
commits) and writes it to `--out`. Consumed by `x-pr-create` (kind=implementation) and
`x-feature-create` (kind=backlog). Fail-open: any missing source produces a human-readable
placeholder — never an audit-sentinel string recognized by `audit-pr-evidence.sh`.

## Parameters

| Parameter | Required | Description |
| :--- | :--- | :--- |
| `--kind` | Yes | `implementation` or `backlog` |
| `--story-id` | When `--kind=implementation` | Pattern `story-XXXX-YYYY` |
| `--epic-id` | When `--kind=backlog` | Pattern `epic-XXXX` or `EPIC-XXXX` |
| `--out` | Yes | Absolute path for output Markdown file |

## Exit Codes

| Exit | Code | Condition |
| :--- | :--- | :--- |
| 0 | OK | Body rendered and written successfully |
| 1 | INVALID_KIND | `--kind` absent or not `implementation`/`backlog` |
| 2 | OPERATIONAL_ERROR | template missing, write permission denied, path traversal detected |
| 3 | INVALID_SCOPE | `--story-id` absent for `--kind=implementation`; `--epic-id` absent for `--kind=backlog` |

## Fail-Open Contract (RULE-004)

Every absent artifact produces a human-readable placeholder. Permitted placeholders:
- `(pending)` — review artifacts not yet produced
- `(not yet run)` — verify gate not yet executed
- `(no telemetry available)` — events.ndjson absent or empty
- `(none)` — no tasks or commits found
- `(no commits)` — git log returns empty

**Forbidden placeholder values** (would trigger `audit-pr-evidence.sh`): `story-XXXX-YYYY`,
`EPIC-XXXX`, any pattern matching `story-[0-9]{4}-[0-9]{4}` as a verdict/decision value.

## Triggers

This skill is internal — never typed by a user. Callers use Rule 13 INLINE-SKILL pattern:

```markdown
Skill(skill: "x-internal-pr-body-render", args: "--kind=implementation --story-id story-XXXX-YYYY --out /abs/path/body.md")
```

## Phase 0 — ARGS

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-pr-body-render Phase-0-Args`

Parse and validate all arguments:

1. Extract `--kind`, `--story-id`, `--epic-id`, `--out` from argv.
2. Validate `--kind`:
   ```bash
   case "$KIND" in
     implementation|backlog) ;;
     *) echo "INVALID_KIND: --kind is required (implementation|backlog)" >&2; exit 1 ;;
   esac
   ```
3. Validate scope argument:
   ```bash
   if [[ "$KIND" == "implementation" && -z "$STORY_ID" ]]; then
     echo "INVALID_SCOPE: --story-id required for --kind=implementation" >&2; exit 3
   fi
   if [[ "$KIND" == "backlog" && -z "$EPIC_ID" ]]; then
     echo "INVALID_SCOPE: --epic-id required for --kind=backlog" >&2; exit 3
   fi
   ```
4. Validate `--story-id` format (kind=implementation): `^story-[0-9]{4}-[0-9]{4}$`.
5. Canonicalize `--out` and verify it stays within `${REPO_ROOT}` (Rule 06 J6 — path traversal):
   ```bash
   REPO_ROOT=$(realpath "$CLAUDE_PROJECT_DIR")
   OUT_CANONICAL=$(realpath --canonicalize-missing "$OUT")
   if [[ "$OUT_CANONICAL" != "$REPO_ROOT"* ]]; then
     echo "OPERATIONAL_ERROR: --out path traversal detected: $OUT" >&2; exit 2
   fi
   ```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-pr-body-render Phase-0-Args ok`

## Phase 1 — GATHER

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-pr-body-render Phase-1-Gather`

Read 7 data sources (kind=implementation only; backlog uses a different set — see Phase 1b in §Backlog extension). Build intermediate JSON envelope. Apply fail-open fallbacks.

### Source 1 — Story markdown (Acceptance Criteria)

Locate `story-{storyId}.md` via PathResolver (searches `ai/epics/*/`, `plans/epic-XXXX/`).
Parse Gherkin `Cenário:` headings for acceptance criteria list.
Fallback: `acceptanceCriteria: []` (empty list).

### Source 2 — Specialist review verdict

Locate `review-story-{storyId}.md` under `ai/epics/epic-XXXX-*/plans/` (v4) or `plans/epic-XXXX/plans/` (v3).

Parse **YAML-when-available**: if `head -1 <file>` returns `---` (YAML frontmatter), use
`yq '.verdict' <file>` (EPIC-0067 structured verdict). Otherwise, fallback regex:
```bash
grep -oP '(?<=\*\*Verdict:\s)(GO|NO-GO)' "$REVIEW_FILE" | head -1
```
Fallback if file absent or regex fails: `specialistVerdict: "(pending)"`.

### Source 3 — Tech-lead review decision

Locate `techlead-review-story-{storyId}.md`. Same YAML-when-available logic.
Fallback regex: `grep -oP '(?<=\*\*Decision:\s)(GO|NO-GO)'`.
Fallback: `techleadDecision: "(pending)"`.

### Source 4 — Verify envelope

Locate `reports/verify-envelope-{storyId}.json`. Parse via jq:
```bash
jq -r '.testsResult, .coverageLine, .coverageBranch' "$ENVELOPE"
```
Fallback per field: `"(not yet run)"`.

### Source 5 — Story completion report (task IDs)

Locate `reports/story-completion-report-{storyId}.md`. Parse task table rows:
```bash
grep -oP 'TASK-[0-9]{4}-[0-9]{4}-[0-9]{3}' "$REPORT" | sort -u | tr '\n' ', '
```
Fallback: `taskIds: "(none)"`.

### Source 6 — Commits

Run `git log origin/develop..HEAD --oneline` in REPO_ROOT. Capture stdout as commit list.
Fallback: `commitsList: "(no commits)"`.

### Source 7 — Orchestrator metadata

Capture:
- `orchSha`: `git rev-parse HEAD`
- `invocationSkill`: `x-story-implement`
- `phase1Artifacts`: list files matching `ai/epics/*/plans/*-{storyId}.md`
- `phase3Artifacts`: list files matching `ai/epics/*/reports/*-{storyId}.*`

Escape all string values: replace `|` with `\|`, backticks with `\``, newlines with space.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-pr-body-render Phase-1-Gather ok`

## Phase 2 — TELEMETRY

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-pr-body-render Phase-2-Telemetry`

Invoke `scripts/telemetry-consolidate.sh` (story-0066-0002) for the story scope:

```bash
TEL_JSON=$(scripts/telemetry-consolidate.sh --story="$STORY_ID" --format=json 2>/tmp/tel-err.txt)
TEL_EXIT=$?
```

- `TEL_EXIT=0`: parse `$TEL_JSON` for `activeMs`, `elapsedMs`, `events`, `tasks[]`, `topTools[]`. Incorporate into envelope.
- `TEL_EXIT=1` (NO_TELEMETRY): set `storyActiveMin=(no telemetry available)`, `storyElapsedMin=(no telemetry available)`, `storyEventCount=(no telemetry available)`, `tasks=[]`, `topTools=[]`. Emit WARNING to stderr.
- `TEL_EXIT=2` (OPERATIONAL_ERROR): same as exit 1 (warning + placeholder). Do NOT abort — telemetry is non-blocking per RULE-002.

Convert activeMs/elapsedMs to minutes: `floor(ms / 60000)`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-pr-body-render Phase-2-Telemetry ok`

## Phase 3 — RENDER

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-pr-body-render Phase-3-Render`

Select template based on `--kind`:

```bash
case "$KIND" in
  implementation) TEMPLATE_NAME="_TEMPLATE-PR-IMPLEMENTATION.md" ;;
  backlog)        TEMPLATE_NAME="_TEMPLATE-PR-BACKLOG.md" ;;
esac
TEMPLATE_PATH="$CLAUDE_PROJECT_DIR/.claude/templates/$TEMPLATE_NAME"
```

If `$TEMPLATE_PATH` not found → emit `OPERATIONAL_ERROR: template missing: $TEMPLATE_PATH` and exit 2.

Invoke `x-internal-report-write` via Rule 13 Pattern 1 (INLINE-SKILL):

```
Skill(skill: "x-internal-report-write", args: "--template $TEMPLATE_PATH --data <json-envelope> --output $TEMP_OUT")
```

`$TEMP_OUT` is a temp file under `${REPO_ROOT}/.claude/tmp/pr-render-$STORY_ID.md`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-pr-body-render Phase-3-Render ok`

## Phase 4 — WRITE

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-pr-body-render Phase-4-Write`

Atomic write: move temp file to final `--out` path:

```bash
mv "$TEMP_OUT" "$OUT_CANONICAL" 2>/tmp/write-err.txt || {
  echo "OPERATIONAL_ERROR: write permission denied or disk full: $(cat /tmp/write-err.txt)" >&2
  exit 2
}
echo "$OUT_CANONICAL"
```

On success, emit the final path to stdout and exit 0.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-pr-body-render Phase-4-Write ok`

## Backlog Extension (kind=backlog — story-0066-0004)

Phase 1b (backlog variant) reads different sources: epic markdown, IMPLEMENTATION-MAP.md,
story list. This extension is added by story-0066-0004 to this same SKILL.md. Phase 0, 2,
3, and 4 are reused with minor template selection change (Phase 3).

## Error Handling

| Scenario | Action |
| :--- | :--- |
| `--kind` absent/invalid | Exit 1 `INVALID_KIND` |
| `--story-id` absent for implementation | Exit 3 `INVALID_SCOPE` |
| `--out` path traversal | Exit 2 `OPERATIONAL_ERROR: path traversal` |
| Template file missing | Exit 2 `OPERATIONAL_ERROR: template missing` |
| Write permission denied | Exit 2 `OPERATIONAL_ERROR: write permission denied` |
| `telemetry-consolidate.sh` exit 1/2 | WARNING + placeholder (continue) |
| Any artifact absent | Placeholder per RULE-004 (continue) |

## Integration Notes

| Skill | Relationship |
| :--- | :--- |
| `x-pr-create` | Primary caller for `--kind=implementation` |
| `x-feature-create` | Caller for `--kind=backlog` (story-0066-0006) |
| `x-internal-report-write` | Delegate for template rendering (Phase 3) |
| `scripts/telemetry-consolidate.sh` | Telemetry aggregator (Phase 2, story-0066-0002) |
| `_TEMPLATE-PR-IMPLEMENTATION.md` | Template consumed by Phase 3 (story-0066-0001) |
| `_TEMPLATE-PR-BACKLOG.md` | Template for backlog kind (story-0066-0001) |

**Haiku eligibility (Rule 23 §criterion a):** utility without design reasoning — reads files,
formats data, writes output. No architectural choices. Upgrade to sonnet/opus must be justified
in this section with concrete rationale.
