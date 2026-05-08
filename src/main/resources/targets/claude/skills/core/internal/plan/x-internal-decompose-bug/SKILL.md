---
visibility: internal
user-invocable: false
requires-capabilities: [governance.bug-lifecycle]
---

> 🔒 **INTERNAL SKILL**
> Invocada apenas por x-create-bug e x-refine-bug.
> NÃO é destinada a invocação direta pelo usuário.

# Skill: x-internal-decompose-bug

## Purpose

Decompose a `bug.md` file into 2-4 implementation story files deterministically based on `(severity, scope)`. Implements the decomposition rules table defined in story-0080-0002.

## Triggers (internal only)

```markdown
Skill(skill: "x-internal-decompose-bug", model: "haiku", args: "<bug-id>")
```

Example: `Skill(skill: "x-internal-decompose-bug", args: "bug-000001")`

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `bug-id` | Yes | Bug ID matching pattern `^bug-[0-9]{6}$` |

## Decomposition Rules

| `severity` | `scope` | Stories produced |
| :--------- | :------ | :--------------- |
| any | single-file | story-01-regression-test, story-02-fix |
| any | single-module | story-01-regression-test, story-02-fix |
| HIGH+ | cross-module | story-01-regression-test, story-02-fix, story-03-doc-update |
| CRITICAL | any | story-01-regression-test, story-02-fix, story-03-doc-update, story-04-rollback-plan |

## Output Contract

On success, emits JSON to stdout:

```json
{
  "bugId": "bug-000001",
  "storiesCreated": [
    "ai/bugs/bug-000001/story-01-regression-test.md",
    "ai/bugs/bug-000001/story-02-fix.md"
  ],
  "idempotent": false,
  "wallClockMs": 123
}
```

When idempotent (files already exist):
```json
{
  "bugId": "bug-000001",
  "storiesCreated": [],
  "idempotent": true,
  "wallClockMs": 5
}
```

## Workflow

### Step 1 — Argument parsing and path resolution

```bash
BUG_ID="${1}"

# Validate bug-id format (security: prevent path traversal)
if [[ ! "$BUG_ID" =~ ^bug-[0-9]{6}$ ]]; then
  echo "ABORT [ARGS_INVALID]: bug-id must match pattern bug-NNNNNN" >&2
  exit 2
fi

BUG_DIR="ai/bugs/${BUG_ID}"
BUG_FILE="${BUG_DIR}/bug.md"

# Verify bug directory exists
if [[ ! -d "$BUG_DIR" ]]; then
  echo "ABORT [BUG_NOT_FOUND]: Bug directory not found: $BUG_DIR" >&2
  exit 3
fi

# Verify bug.md exists
if [[ ! -f "$BUG_FILE" ]]; then
  echo "ABORT [BUG_FILE_NOT_FOUND]: Bug file not found: $BUG_FILE" >&2
  exit 4
fi
```

### Step 2 — Idempotency check

```bash
FIRST_STORY="${BUG_DIR}/story-01-regression-test.md"
if [[ -f "$FIRST_STORY" ]]; then
  jq -n \
    --arg bugId "$BUG_ID" \
    --argjson storiesCreated '[]' \
    --argjson idempotent true \
    --argjson wallClockMs 5 \
    '{bugId:$bugId, storiesCreated:$storiesCreated, idempotent:$idempotent, wallClockMs:$wallClockMs}'
  echo "decomposition already present — skipping" >&2
  exit 0
fi
```

### Step 3 — Read bug metadata

```bash
# Extract severity and scope from bug.md
SEVERITY=$(grep -m1 '^\*\*Severity:\*\*' "$BUG_FILE" | sed 's/\*\*Severity:\*\* //')
SCOPE=$(grep -m1 '^\*\*Scope:\*\*' "$BUG_FILE" | sed 's/\*\*Scope:\*\* //')

if [[ -z "$SEVERITY" ]]; then
  echo "ABORT [PARSE_ERROR]: Could not extract severity from $BUG_FILE" >&2
  exit 5
fi

if [[ -z "$SCOPE" ]]; then
  echo "ABORT [PARSE_ERROR]: Could not extract scope from $BUG_FILE" >&2
  exit 5
fi
```

### Step 4 — Determine stories to create

```bash
STORIES_TO_CREATE=("story-01-regression-test" "story-02-fix")

# Cross-module + HIGH or CRITICAL adds doc-update
if [[ "$SCOPE" == "cross-module" ]] && \
   [[ "$SEVERITY" == "HIGH" || "$SEVERITY" == "CRITICAL" ]]; then
  STORIES_TO_CREATE+=("story-03-doc-update")
fi

# CRITICAL always gets rollback-plan
if [[ "$SEVERITY" == "CRITICAL" ]]; then
  # Ensure doc-update is present
  if [[ ! " ${STORIES_TO_CREATE[@]} " =~ "story-03-doc-update" ]]; then
    STORIES_TO_CREATE+=("story-03-doc-update")
  fi
  STORIES_TO_CREATE+=("story-04-rollback-plan")
fi
```

### Step 5 — Instantiate template for each story

```bash
TEMPLATE="src/main/resources/targets/claude/templates/_TEMPLATE-BUG-STORY.md"
CREATED_STORIES=()
START_MS=$(($(date +%s%N) / 1000000))

for STORY_NAME in "${STORIES_TO_CREATE[@]}"; do
  STORY_FILE="${BUG_DIR}/${STORY_NAME}.md"
  STORY_SEQ=$(echo "$STORY_NAME" | sed 's/story-\([0-9]*\).*/\1/')
  
  case "$STORY_NAME" in
    *regression-test*)
      KIND="[Test]"
      TITLE="Regression test for bug-${BUG_ID}"
      OBJECTIVE="author a failing test that reproduces the bug"
      VALUE="enforce regression-first TDD by having a RED test before any fix"
      BLOCKED_BY="— (root story)"
      ;;
    *fix*)
      KIND="[Dev]"
      TITLE="Fix implementation for bug-${BUG_ID}"
      OBJECTIVE="implement the fix that resolves the root cause"
      VALUE="turn the regression test GREEN and close the bug"
      BLOCKED_BY="story-01-regression-test"
      ;;
    *doc-update*)
      KIND="[Doc]"
      TITLE="Documentation update for bug-${BUG_ID}"
      OBJECTIVE="update documentation to reflect the root-cause and mitigation"
      VALUE="prevent future occurrences by documenting the fixed behavior"
      BLOCKED_BY="story-02-fix"
      ;;
    *rollback-plan*)
      KIND="[Ops]"
      TITLE="Rollback plan for critical bug-${BUG_ID}"
      OBJECTIVE="define and validate a rollback procedure in case the fix fails in production"
      VALUE="reduce MTTR for critical bugs by having a ready rollback"
      BLOCKED_BY="story-02-fix"
      ;;
  esac
  
  # Instantiate template
  sed \
    -e "s|{{BUG_STORY_KIND}}|${KIND}|g" \
    -e "s|{{BUG_STORY_TITLE}}|${TITLE}|g" \
    -e "s|{{BUG_ID}}|${BUG_ID#bug-}|g" \
    -e "s|{{STORY_SEQUENCE}}|${STORY_SEQ}|g" \
    -e "s|{{PERSONA}}|Developer|g" \
    -e "s|{{BUG_STORY_OBJECTIVE}}|${OBJECTIVE}|g" \
    -e "s|{{BUG_STORY_VALUE}}|${VALUE}|g" \
    -e "s|{{BLOCKED_BY}}|${BLOCKED_BY}|g" \
    -e "s|{{BLOCKS_OR_NONE}}|—|g" \
    "$TEMPLATE" > "$STORY_FILE"
  
  CREATED_STORIES+=("$STORY_FILE")
done

END_MS=$(($(date +%s%N) / 1000000))
WALL_CLOCK=$((END_MS - START_MS))
```

### Step 6 — Emit response envelope

```bash
STORIES_JSON=$(printf '%s\n' "${CREATED_STORIES[@]}" | jq -R . | jq -s .)

jq -n \
  --arg bugId "$BUG_ID" \
  --argjson storiesCreated "$STORIES_JSON" \
  --argjson idempotent false \
  --argjson wallClockMs "$WALL_CLOCK" \
  '{bugId:$bugId, storiesCreated:$storiesCreated, idempotent:$idempotent, wallClockMs:$wallClockMs}'
```

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | Stories created (or idempotent skip) |
| 2 | ARGS_INVALID | Bug ID fails pattern `^bug-[0-9]{6}$` |
| 3 | BUG_NOT_FOUND | `ai/bugs/<bug-id>/` directory absent |
| 4 | BUG_FILE_NOT_FOUND | `bug.md` absent inside bug directory |
| 5 | PARSE_ERROR | Cannot extract severity or scope from bug.md |

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid bug-id (path traversal attempt) | ABORT exit 2 before any file I/O |
| Bug directory missing | ABORT exit 3 |
| bug.md missing | ABORT exit 4 |
| severity/scope unparseable | ABORT exit 5 |
| Template not found | ABORT with message |
| Story file write failure | ABORT with message |
| jq not available | Exit 127 with "jq is required" |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-create-bug` | caller | Invoked after scaffold commit |
| `x-refine-bug` | caller | Can invoke to re-validate decomposition |
| `_TEMPLATE-BUG-STORY.md` | dependency | Template must exist |
| `_TEMPLATE-BUG.md` | sibling | Parent template used by x-create-bug |

## Performance Contract

Target: ≤ 5 seconds P95 for typical bug.md (≤ 200 lines). All operations are local file I/O + string processing. No network I/O.

## References

- story-0080-0002, task-0080-0002-002
- Template: `src/main/resources/targets/claude/templates/_TEMPLATE-BUG-STORY.md`
- Decomposition rules table: story-0080-0002 §5.2
