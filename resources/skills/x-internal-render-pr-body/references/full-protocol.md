# x-internal-render-pr-body — Full Protocol

Detailed reference for `x-internal-render-pr-body`. The SKILL.md body holds the minimum viable contract per ADR-0012; this document is the canonical procedural reference.

## Fail-Open Contract (RULE-004)

Every absent artifact produces a human-readable placeholder. Permitted placeholders:
- `(pending)` — review artifacts not yet produced
- `(not yet run)` — verify gate not yet executed
- `(no telemetry available)` — events.ndjson absent or empty
- `(none)` — no tasks or commits found
- `(no commits)` — git log returns empty

**Forbidden placeholder values** (would trigger `audit-pr-evidence.sh`): `story-XXXX-YYYY`, `EPIC-XXXX`, any pattern matching `story-[0-9]{4}-[0-9]{4}` as a verdict/decision value.

## Phase 0 — ARGS

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-render-pr-body Phase-0-Args`

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
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-render-pr-body Phase-0-Args ok`

## Phase 1 — GATHER

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-render-pr-body Phase-1-Gather`

Read 7 data sources (kind=implementation only; backlog uses a different set — see Phase 1.5 below). Build intermediate JSON envelope. Apply fail-open fallbacks.

### Source 1 — Story markdown (Acceptance Criteria)

Locate `story-{storyId}.md` via PathResolver (searches `ai/epics/*/`, `plans/epic-XXXX/`).
Parse Gherkin `Cenário:` headings for acceptance criteria list.
Fallback: `acceptanceCriteria: []` (empty list).

### Source 2 — Specialist review verdict

Locate `review-story-{storyId}.md` under `ai/epics/epic-XXXX-*/plans/` (v4) or `plans/epic-XXXX/plans/` (v3).

Parse **YAML-when-available**: if `head -1 <file>` returns `---` (YAML frontmatter), use `yq '.verdict' <file>` (EPIC-0067 structured verdict). Otherwise, fallback regex:
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
- `invocationSkill`: `x-implement-story`
- `phase1Artifacts`: list files matching `ai/epics/*/plans/*-{storyId}.md`
- `phase3Artifacts`: list files matching `ai/epics/*/reports/*-{storyId}.*`

Escape all string values: replace `|` with `\|`, backticks with `\``, newlines with space.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-render-pr-body Phase-1-Gather ok`

## Phase 1.5 — BACKLOG-GATHER (conditional on --kind=backlog)

<!-- phase-no-gate: Phase 1.5 is a conditional sub-phase inside Phase 1 — no independent gate -->

Executed only when `--kind=backlog`. Reads backlog-specific sources to build the backlog JSON envelope (distinct from the implementation envelope built in Phase 1).

### Source 1 — Epic markdown

Locate `epic-XXXX.md` via PathResolver (searches `ai/epics/epic-XXXX-*/`, `plans/epic-XXXX/`).
Parse:
- `summary`: first paragraph under `## 1. Contexto & Escopo` or `## 1.1 Problema`
- `stories`: table rows in `## 9.2 Stories` — extract Phase, Story ID, Layer, Bloqueada por
- `successMetrics`: bullet items under `## 5.2 Métricas Quantitativas` or `## Métricas de Sucesso`
- `outOfScopeList`: bullet items under section containing "Fora do escopo"

**Escape all string values before JSON injection**: replace `|` with `\|`, backticks with `` \` ``, newlines with space.

Fallbacks: `summary: "(no summary)"`, `stories: []`, `successMetrics: ["(no metrics declared)"]`, `outOfScopeList: "(not documented)"`.

### Source 2 — IMPLEMENTATION-MAP.md

Locate `IMPLEMENTATION-MAP.md` in the epic directory. Parse:
- `criticalPath`: regex `\*\*Critical path.*?:\*\*\s*(.+)` or `Caminho crítico.*?:\s*(.+)`
- `maxParallelism`: regex `\*\*Maximum parallelism.*?:\*\*\s*(\d+)` or `Paralelismo máximo.*?:\s*(\d+)`
- `maxParallelPhase`: regex `\(Phase\s+([\w ]+)\)` after maxParallelism
- `totalPhases`: count of `## Phase N` headers

If IMPLEMENTATION-MAP.md absent:
```bash
echo "WARN: IMPLEMENTATION-MAP.md not found for ${EPIC_ID}" >&2
criticalPath="(map missing)"
maxParallelism=0
totalPhases=0
```

### Source 3 — Spec markdown

Search for spec file:
```bash
SPEC=$(find "${REPO_ROOT}" -name "spec-*.md" -path "*/epic-XXXX-*/*" 2>/dev/null | head -1)
```
If found: `specPath=$(realpath --relative-to="${REPO_ROOT}" "$SPEC")`, `specSha=$(git rev-parse "HEAD:${specPath}" 2>/dev/null || echo "(unknown)")`.
If not found: `specPath="(spec not found)"`, `specSha="(unknown)"`.

### Source 4 — Coordinations with in-progress epics

```bash
for state_file in "${REPO_ROOT}"/ai/epics/*/execution-state.json; do
  status=$(jq -r '.status // empty' "$state_file" 2>/dev/null)
  if [[ "$status" == "Em Andamento" ]]; then
    epic_id=$(jq -r '.epicId // empty' "$state_file" 2>/dev/null)
    coordinations+="{\"epicId\":\"$epic_id\",\"status\":\"in_progress\"}"
  fi
done
```
Fallback if none: `coordinations: [{"epicId":"(none)","status":"none","hotFiles":"","risk":"none"}]`.

## Phase 2 — TELEMETRY

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-render-pr-body Phase-2-Telemetry`

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
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-render-pr-body Phase-2-Telemetry ok`

## Phase 3 — RENDER

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-render-pr-body Phase-3-Render`

Select template based on `--kind`:

```bash
case "$KIND" in
  implementation) TEMPLATE_NAME="_TEMPLATE-PR-IMPLEMENTATION.md" ;;
  backlog)        TEMPLATE_NAME="_TEMPLATE-PR-BACKLOG.md" ;;
esac
TEMPLATE_PATH="$CLAUDE_PROJECT_DIR/.claude/templates/$TEMPLATE_NAME"
```

If `$TEMPLATE_PATH` not found → emit `OPERATIONAL_ERROR: template missing: $TEMPLATE_PATH` and exit 2.

Invoke `x-internal-write-report` via Rule 13 Pattern 1 (INLINE-SKILL):

```
Skill(skill: "x-internal-write-report", args: "--template $TEMPLATE_PATH --data <json-envelope> --output $TEMP_OUT")
```

`$TEMP_OUT` is a temp file under `${REPO_ROOT}/.claude/tmp/pr-render-$STORY_ID.md`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-render-pr-body Phase-3-Render ok`

## Phase 4 — WRITE

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-internal-render-pr-body Phase-4-Write`

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
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-internal-render-pr-body Phase-4-Write ok`

## Haiku Eligibility (Rule 23 §criterion a)

Utility without design reasoning — reads files, formats data, writes output. No architectural choices. Upgrade to sonnet/opus must be justified with concrete rationale.
