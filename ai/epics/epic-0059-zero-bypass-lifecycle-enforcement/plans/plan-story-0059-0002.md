---
generated-by: x-internal-story-build-plan@0792be069d39537b5f4c7c76d7e68372b585697f
generated-at: 2026-04-27T16:50:49Z
story-id: story-0059-0002
---

# Implementation Plan — story-0059-0002: Origin Markers in Artifacts + Anti-Backfill Audit

## Scope

- **Epic:** EPIC-0059 (Zero-Bypass Lifecycle Enforcement)
- **Story:** story-0059-0002
- **Scope Classification:** STANDARD (3 tasks, Bash + SKILL.md modifications, no Java)
- **Planning Mode:** HYBRID

## Task Summary

| Task ID | Description | Layer | Size | Dependencies |
| :--- | :--- | :--- | :--- | :--- |
| TASK-0059-0002-001 | Emit frontmatter in x-arch-plan and x-internal-story-build-plan | Adapter (SKILL.md) | M | — |
| TASK-0059-0002-002 | Emit frontmatter in x-test-plan and security/compliance skills | Adapter (SKILL.md) | S | TASK-0059-0002-001 |
| TASK-0059-0002-003 | Add SHA validation and anti-backfill to audit script | Adapter (CI script) | M | TASK-0059-0002-001, TASK-0059-0002-002 |

## Implementation Order

Inner layers first per Rule 04:

1. **TASK-0059-0002-001** — Modify SKILL.md files for x-arch-plan and x-internal-story-build-plan. These are the primary planning skills; establishing their frontmatter pattern first lets task 002 follow the same template.
2. **TASK-0059-0002-002** — Extend to x-test-plan and Phase 1E/1F of x-story-implement. Follows the same pattern as task 001.
3. **TASK-0059-0002-003** — Extend `audit-execution-integrity.sh` with `check_frontmatter_origin()` and `check_anti_backfill()`. Only implementable after the frontmatter format is finalized in tasks 001 and 002.

## Key Implementation Details

### TASK-0059-0002-001: Frontmatter Emission in x-arch-plan and x-internal-story-build-plan

**Source-of-truth SKILL.md locations:**
- `java/src/main/resources/targets/claude/skills/core/plan/x-arch-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/internal/plan/x-internal-story-build-plan/SKILL.md`

**Generated SKILL.md locations (mirror changes):**
- `.claude/skills/x-arch-plan/SKILL.md`
- `.claude/skills/x-internal-story-build-plan/SKILL.md`

**Instruction to add** (before "### Step 7 — Save the document" in x-arch-plan, and analogous save step in x-internal-story-build-plan):

```markdown
#### Origin Marker (EPIC-0059 — story-0059-0002)

Before writing the artifact file, prepend the following YAML frontmatter block:

```bash
GENERATED_SHA=$(git rev-parse HEAD 2>/dev/null || echo "unknown")
GENERATED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)
```

Insert at the very top of the output file (before any Markdown content):

```yaml
---
generated-by: x-arch-plan@${GENERATED_SHA}
generated-at: ${GENERATED_AT}
story-id: ${STORY_ID}
---
```

This frontmatter is mandatory for `audit-execution-integrity.sh` Phase-1 validation (EPIC-0059, Rule 24).
```

### TASK-0059-0002-002: Frontmatter Emission in x-test-plan and Phase 1E/1F

**Source-of-truth SKILL.md locations:**
- `java/src/main/resources/targets/claude/skills/core/test/x-test-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md` (Phase 1E and 1F sections)

Same frontmatter template as task 001, with skill name adjusted:
- x-test-plan: `generated-by: x-test-plan@${SHA}`
- Phase 1E (security): `generated-by: x-story-implement-security@${SHA}`
- Phase 1F (compliance): `generated-by: x-story-implement-compliance@${SHA}`

### TASK-0059-0002-003: Audit Script Extensions

**Target file:** `scripts/audit-execution-integrity.sh`

**New function `check_frontmatter_origin()`:**
```bash
check_frontmatter_origin() {
    local artifact_path="$1"
    # 1. Check frontmatter block present
    if ! head -1 "${artifact_path}" | grep -q '^---'; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — missing generated-by frontmatter" >&2
        return 1
    fi
    # 2. Extract generated-by field
    local generated_by
    generated_by=$(awk '/^---/{f++} f==1 && /^generated-by:/{print $2}' "${artifact_path}" | head -1)
    if [[ -z "${generated_by}" ]]; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — generated-by field absent" >&2
        return 1
    fi
    # 3. Validate format: <skill>@<40-hex>
    if ! echo "${generated_by}" | grep -qE '^[a-z-]+@[0-9a-f]{40}$'; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — generated-by format invalid: ${generated_by}" >&2
        return 1
    fi
    # 4. Validate SHA exists in git history
    local sha="${generated_by##*@}"
    if ! git cat-file -t "${sha}" 2>/dev/null | grep -q 'commit'; then
        echo "EIE_EVIDENCE_MISSING: ${artifact_path} — SHA not found in git history: ${sha}" >&2
        return 1
    fi
    return 0
}
```

**New function `check_anti_backfill()`:**
```bash
check_anti_backfill() {
    local story_id="$1"
    local artifact_path="$2"
    local story_merge_ref="${3:-HEAD}"
    # Get timestamp when artifact was first committed
    local artifact_first_commit_ts
    artifact_first_commit_ts=$(git log --diff-filter=A --pretty=format:%ct -- "${artifact_path}" 2>/dev/null | tail -1)
    [[ -z "${artifact_first_commit_ts}" ]] && return 0  # file not in git yet (untracked) — skip
    # Get merge commit timestamp for the story PR
    local merge_ts
    merge_ts=$(git log --first-parent --merges \
        --pretty=format:'%ct %s' 2>/dev/null \
        | grep -i "story-${story_id#story-}" \
        | awk '{print $1}' | head -1)
    [[ -z "${merge_ts}" ]] && return 0  # cannot determine merge time — skip (fail-open)
    if [[ "${artifact_first_commit_ts}" -gt "${merge_ts}" ]]; then
        echo "EIE_BACKFILL_DETECTED: ${artifact_path} — artifact added after story merge (artifact_ts=${artifact_first_commit_ts} > merge_ts=${merge_ts})" >&2
        return 1
    fi
    return 0
}
```

**Integration into `check_phase1_evidence()`:**
After confirming file presence, call `check_frontmatter_origin` and `check_anti_backfill` for each artifact. A failure from either propagates as `story_failed=1`.

**Exit code additions:**
The existing exit code 1 (`EIE_EVIDENCE_MISSING`) covers both frontmatter validation failures and SHA not-found cases. `EIE_BACKFILL_DETECTED` also exits 1 (per Section 5.2 of story-0059-0002.md).

## TDD Approach

### Smoke Tests (Bash BATS framework or plain shell)

Since the project has no dedicated BATS setup, smoke tests are implemented as a standalone shell script:

**`src/test/bash/audit-anti-backfill-smoke.sh`**:

1. Create temp directory with a fake git repo
2. Test: artifact without frontmatter → exit 1 `EIE_EVIDENCE_MISSING`
3. Test: artifact with valid frontmatter + real SHA → exit 0
4. Test: artifact with SHA of 40 zeros (nonexistent) → exit 1
5. Test: artifact committed after a fake merge commit → exit 1 `EIE_BACKFILL_DETECTED`
6. Test: `<!-- audit-exempt: backfill https://issue/123 -->` → exit 0
7. Test: `<!-- audit-exempt: backfill -->` (empty link) → exit 3

## File Footprint

### write:
- `java/src/main/resources/targets/claude/skills/core/plan/x-arch-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/internal/plan/x-internal-story-build-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/test/x-test-plan/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/dev/x-story-implement/SKILL.md`
- `java/src/main/resources/targets/claude/skills/core/plan/x-task-plan/SKILL.md`
- `scripts/audit-execution-integrity.sh`
- `.claude/skills/x-arch-plan/SKILL.md`
- `.claude/skills/x-internal-story-build-plan/SKILL.md`
- `.claude/skills/x-test-plan/SKILL.md`
- `.claude/skills/x-story-implement/SKILL.md`
- `.claude/skills/x-task-plan/SKILL.md`
- `src/test/bash/audit-anti-backfill-smoke.sh`

### read:
- `plans/epic-0059/story-0059-0002.md`
- `audits/execution-integrity-baseline.txt`
