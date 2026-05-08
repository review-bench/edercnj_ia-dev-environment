---
visibility: internal
user-invocable: false
requires-capabilities: [governance.bug-lifecycle]
description: "Generate bug implementation map with Kahn sort and Mermaid graph"
---

> 🔒 **INTERNAL SKILL**
> Invoked only by x-create-bug (after decomposition) and x-refine-bug.
> NOT user-invocable.

# Skill: x-internal-map-bug

## Purpose

Aggregate bug stories into `ai/bugs/bug-XXXXXX/IMPLEMENTATION-MAP.md` with dependency matrix, ASCII phase diagram, critical path (Kahn topological sort), Mermaid graph, and generation metadata.

## Triggers (internal only)

```markdown
Skill(skill: "x-internal-map-bug", model: "haiku", args: "<bug-id>")
```

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `bug-id` | Yes | Bug ID matching `^bug-[0-9]{6}$` |

## Output Contract

JSON response:

```json
{
  "mapPath": "ai/bugs/bug-000001/IMPLEMENTATION-MAP.md",
  "storiesAggregated": 3,
  "elapsedMs": 145
}
```

## Workflow

### Step 1 — Validate and resolve paths

```bash
BUG_ID="${1}"
if [[ ! "$BUG_ID" =~ ^bug-[0-9]{6}$ ]]; then
  echo "ABORT [ARGS_INVALID]: bug-id must match ^bug-[0-9]{6}$" >&2
  exit 2
fi

BUG_DIR="ai/bugs/${BUG_ID}"
BUG_FILE="${BUG_DIR}/bug.md"
MAP_FILE="${BUG_DIR}/IMPLEMENTATION-MAP.md"

[[ -d "$BUG_DIR" ]] || { echo "ABORT [BUG_NOT_FOUND]: $BUG_DIR" >&2; exit 3; }
[[ -f "$BUG_FILE" ]] || { echo "ABORT [BUG_FILE_NOT_FOUND]: $BUG_FILE" >&2; exit 4; }
```

### Step 2 — Collect and parse story files

```bash
START_MS=$(($(date +%s%N) / 1000000))
mapfile -t STORY_FILES < <(find "$BUG_DIR" -name "story-[0-9][0-9]-*.md" | sort)

declare -A BLOCKED_BY  # story_name → predecessor_name
declare -a STORY_ORDER

for sf in "${STORY_FILES[@]}"; do
  sname=$(basename "$sf" .md)
  STORY_ORDER+=("$sname")
  
  # Extract Blocked By from Section 7
  blocker=$(grep -m1 '\*\*Blocked By:\*\*' "$sf" \
    | sed 's/.*Blocked By:\*\* //' | tr -d '[:space:]')
  
  if [[ "$blocker" == "—" || -z "$blocker" ]]; then
    BLOCKED_BY["$sname"]=""
  else
    BLOCKED_BY["$sname"]="$blocker"
  fi
done
```

### Step 3 — Validate dependency consistency (AC-2)

```bash
for sname in "${!BLOCKED_BY[@]}"; do
  blocker="${BLOCKED_BY[$sname]}"
  if [[ -n "$blocker" ]]; then
    # Check that the declared predecessor exists
    if [[ ! -f "${BUG_DIR}/${blocker}.md" ]]; then
      echo "ABORT [MAP_INCONSISTENT_DEPS]: $sname declares Blocked-By $blocker but $blocker.md not found" >&2
      exit 1
    fi
  fi
done
```

### Step 4 — Compute topological phases (Kahn's algorithm)

```bash
declare -A IN_DEGREE
declare -A SUCCESSORS

for sname in "${STORY_ORDER[@]}"; do
  IN_DEGREE["$sname"]=0
  SUCCESSORS["$sname"]=""
done

for sname in "${STORY_ORDER[@]}"; do
  blocker="${BLOCKED_BY[$sname]}"
  if [[ -n "$blocker" ]]; then
    IN_DEGREE["$sname"]=$((${IN_DEGREE[$sname]} + 1))
    SUCCESSORS["$blocker"]="${SUCCESSORS[$blocker]} $sname"
  fi
done

declare -A PHASE
CURRENT_PHASE=0
QUEUE=()

for sname in "${STORY_ORDER[@]}"; do
  [[ "${IN_DEGREE[$sname]}" -eq 0 ]] && QUEUE+=("$sname")
done

while [[ ${#QUEUE[@]} -gt 0 ]]; do
  NEXT_QUEUE=()
  for sname in "${QUEUE[@]}"; do
    PHASE["$sname"]=$CURRENT_PHASE
    for successor in ${SUCCESSORS[$sname]}; do
      IN_DEGREE["$successor"]=$((${IN_DEGREE[$successor]} - 1))
      [[ "${IN_DEGREE[$successor]}" -eq 0 ]] && NEXT_QUEUE+=("$successor")
    done
  done
  QUEUE=("${NEXT_QUEUE[@]}")
  CURRENT_PHASE=$((CURRENT_PHASE + 1))
done
```

### Step 5 — Compute critical path

```bash
# Critical path = longest chain (BFS from roots)
declare -A DIST
for sname in "${STORY_ORDER[@]}"; do
  DIST["$sname"]=1
done

for sname in "${STORY_ORDER[@]}"; do
  blocker="${BLOCKED_BY[$sname]}"
  if [[ -n "$blocker" ]]; then
    new_dist=$((${DIST[$blocker]} + 1))
    if [[ $new_dist -gt ${DIST[$sname]} ]]; then
      DIST["$sname"]=$new_dist
    fi
  fi
done

# Find node with max distance (end of critical path)
MAX_DIST=0
CRITICAL_END=""
for sname in "${STORY_ORDER[@]}"; do
  if [[ "${DIST[$sname]}" -gt $MAX_DIST ]]; then
    MAX_DIST="${DIST[$sname]}"
    CRITICAL_END="$sname"
  fi
done

# Trace back to root
CRITICAL_PATH=("$CRITICAL_END")
CURRENT="$CRITICAL_END"
while [[ -n "${BLOCKED_BY[$CURRENT]}" ]]; do
  CURRENT="${BLOCKED_BY[$CURRENT]}"
  CRITICAL_PATH=("$CURRENT" "${CRITICAL_PATH[@]}")
done
```

### Step 6 — Generate map content and write atomically

Build each of the 6 sections and write to a temp file, then rename to the final path (atomic write):

```bash
TMP_MAP=$(mktemp)
chmod 600 "$TMP_MAP"
trap 'rm -f "$TMP_MAP"' EXIT

# Generate sections into $TMP_MAP
# ... (section building logic) ...

# Atomic rename
mv "$TMP_MAP" "$MAP_FILE"
trap - EXIT
```

### Step 7 — Emit response envelope

```bash
END_MS=$(($(date +%s%N) / 1000000))
ELAPSED=$((END_MS - START_MS))

jq -n \
  --arg mapPath "$MAP_FILE" \
  --argjson storiesAggregated "${#STORY_FILES[@]}" \
  --argjson elapsedMs "$ELAPSED" \
  '{mapPath:$mapPath, storiesAggregated:$storiesAggregated, elapsedMs:$elapsedMs}'
```

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | IMPLEMENTATION-MAP.md created |
| 1 | MAP_INCONSISTENT_DEPS | Story Blocked-By references nonexistent predecessor |
| 2 | ARGS_INVALID | Bug ID fails `^bug-[0-9]{6}$` |
| 3 | BUG_NOT_FOUND | Bug directory absent |
| 4 | BUG_FILE_NOT_FOUND | bug.md absent in directory |

## Security Notes

- Bug ID validated with `^bug-[0-9]{6}$` before any file I/O (path traversal prevention)
- Story file contents are read as plain text only — no template eval, no shell expansion
- Script payloads in story files appear verbatim in fenced blocks (AC-4 compliance)
- Atomic write via temp file + rename prevents partial map on failure

## Performance Contract

Target: ≤ 3 seconds P95 for up to 5 stories. All operations are local file I/O + string processing.

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-create-bug` | caller | Invoked after decomposition in Step 8 |
| `x-refine-bug` | caller | Re-invoked to refresh map during refinement |
| `x-internal-decompose-bug` | sibling | Must run before this skill |

## References

- story-0080-0003, task-0080-0003-002
- Template: `src/main/resources/targets/claude/templates/_TEMPLATE-BUG-IMPLEMENTATION-MAP.md`
- Kahn topological sort for phase computation
