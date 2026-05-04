---
name: x-create-pr
description: "Task-level PR creation with formatted title, automatic labels, structured body, and target branch logic. Creates standardized PRs for individual tasks with Task ID traceability."
user-invocable: true
allowed-tools: Bash, Read, Grep, Glob, Skill
argument-hint: "TASK-XXXX-YYYY-NNN [--auto-approve-pr] [--draft] [--description \"short desc\"] [--target-branch <branch>] [--auto-merge <merge|squash|rebase|none>] [--epic-id <XXXX>]"
context-budget: medium
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Task PR Creation

## Purpose

Creates standardized Pull Requests for individual tasks in {{PROJECT_NAME}} with Task ID in the title, automatic labels (`task`, `story-XXXX-YYYY`, `epic-XXXX`), structured body with review checklist, and correct target branch. Ensures every task delivery is traceable and independently reviewable.

## Triggers

- `/x-create-pr TASK-0029-0001-001` -- create PR for the specified task
- `/x-create-pr TASK-0029-0001-001 --draft` -- create as draft PR
- `/x-create-pr TASK-0029-0001-001 --auto-approve-pr` -- target parent story branch instead of develop
- `/x-create-pr TASK-0029-0001-001 --description "add user validation"` -- override PR title description
- `/x-create-pr TASK-0029-0001-001 --target-branch epic/0049 --auto-merge merge --epic-id 0049` -- orchestrator-propagated PR targeting an epic branch with auto-merge enabled and epic label applied

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `task-id` | String | Yes | Task ID in format `TASK-XXXX-YYYY-NNN` |
| `--auto-approve-pr` | Flag | No | Target parent story branch instead of `develop` |
| `--draft` | Flag | No | Create PR as draft |
| `--description` | String | No | Short description for PR title (overrides branch-derived description) |
| `--target-branch` | String | No | Explicit base branch for the PR (overrides `--auto-approve-pr` and default `develop`). Typical orchestrator value: `epic/XXXX` |
| `--auto-merge` | Enum | No | Post-create auto-merge strategy. One of `merge`, `squash`, `rebase`, `none` (default `none`). Requires `--target-branch` |
| `--epic-id` | String(4) | No | Four-digit epic identifier (regex `^\d{4}$`). When set, adds label `epic-XXXX` and epic metadata to the PR body |
| `--no-story-evidence` | Flag | No | Omit `## Orchestrator Evidence` section. Use ONLY for chore/docs PRs without story context. Allowed by `audit-pr-evidence.sh`. |

## Workflow

```
Phase 0   VALIDATE        -> Parse task ID, validate branch naming, parse extension flags
Phase 1   PRE-CHECK       -> Run {{TEST_COMMAND}} to verify tests pass
Phase 2   TITLE           -> Format PR title (<=70 chars, Conventional Commits)
Phase 3   BODY            -> Generate structured PR body
Phase 3.5 EVIDENCE        -> Inject ## Orchestrator Evidence (unless --no-story-evidence)
Phase 4   LABELS          -> Create labels if missing, collect label list
Phase 5   CREATE          -> Create PR via gh pr create
Phase 6   AUTO-MERGE      -> If --auto-merge != none, delegate to x-merge-pr
```

### Phase 0 -- Validate Branch and Arguments

1. Parse task ID from arguments: extract `TASK-XXXX-YYYY-NNN` format
2. Validate task ID matches pattern: `TASK-\d{4}-\d{4}-\d{3}`
3. Verify current branch follows naming convention:

```bash
CURRENT_BRANCH=$(git branch --show-current)
# Must match: feat/task-XXXX-YYYY-NNN-*
PATTERN="^feat/task-[0-9]{4}-[0-9]{4}-[0-9]{3}-"
if [[ ! "$CURRENT_BRANCH" =~ $PATTERN ]]; then
  echo "ABORT: Current branch does not match task naming convention: feat/task-XXXX-YYYY-NNN-*"
  echo "Current branch: $CURRENT_BRANCH"
  exit 1
fi
```

4. Extract epic ID (`XXXX`), story ID (`XXXX-YYYY`), and task number (`NNN`) from the task ID
5. If `--auto-approve-pr` is set, verify parent branch exists:

```bash
STORY_ID="story-XXXX-YYYY"
PARENT_BRANCH=$(git branch -a | grep "feat/${STORY_ID}" | head -1 | tr -d ' ')
if [[ -z "$PARENT_BRANCH" ]]; then
  echo "ABORT: Parent branch not found for ${STORY_ID}"
  exit 1
fi
```

6. Parse extension flags (`--target-branch`, `--auto-merge`, `--epic-id`) and run the validation matrix:

```bash
# Defaults (backward compatible: absent flags produce legacy behavior)
TARGET_BRANCH_OVERRIDE=""     # empty -> legacy target (parent or develop)
AUTO_MERGE_STRATEGY="none"    # none | merge | squash | rebase
EPIC_ID_OVERRIDE=""            # empty -> no epic-XXXX label injection

# Validate --epic-id regex (4 digits) -- exit 5 / INVALID_EPIC_ID
if [[ -n "$EPIC_ID_OVERRIDE" && ! "$EPIC_ID_OVERRIDE" =~ ^[0-9]{4}$ ]]; then
  echo "ABORT [INVALID_EPIC_ID]: Epic ID must be 4 digits"
  exit 5
fi

# Validate --auto-merge value set
case "$AUTO_MERGE_STRATEGY" in
  none|merge|squash|rebase) ;;
  *) echo "ABORT: --auto-merge must be one of merge|squash|rebase|none"; exit 1 ;;
esac

# Mutex: --auto-merge requires --target-branch -- exit 6 / AUTO_MERGE_REQUIRES_TARGET
if [[ "$AUTO_MERGE_STRATEGY" != "none" && -z "$TARGET_BRANCH_OVERRIDE" ]]; then
  echo "ABORT [AUTO_MERGE_REQUIRES_TARGET]: --auto-merge requires --target-branch"
  exit 6
fi

# Soft warning: auto-merge directly to develop is unusual
if [[ "$TARGET_BRANCH_OVERRIDE" == "develop" && "$AUTO_MERGE_STRATEGY" != "none" ]]; then
  echo "WARN: Auto-merge directly to develop is unusual; consider using an epic branch"
fi
```

### Phase 1 -- Pre-Check (Tests)

Run the project test command before creating the PR:

```bash
{{TEST_COMMAND}}
if [[ $? -ne 0 ]]; then
  echo "ABORT: Tests failed. Fix tests before creating PR."
  exit 1
fi
```

If tests fail, abort with message: `"Tests failed. Fix tests before creating PR."`

### Phase 2 -- Format PR Title

Format the PR title following Conventional Commits:

1. Determine the commit type from the task type or default to `feat`
2. Build title: `<type>(TASK-XXXX-YYYY-NNN): <description>`
3. If `--description` is provided, use it; otherwise derive from branch name:

```bash
# Extract description from branch name
# feat/task-0029-0001-001-add-validation -> add validation
DESC=$(echo "$CURRENT_BRANCH" | sed 's/feat\/task-[0-9]*-[0-9]*-[0-9]*-//' | tr '-' ' ')
```

4. Truncate title to 70 characters maximum:

```bash
TITLE="feat(TASK-XXXX-YYYY-NNN): ${DESC}"
if [[ ${#TITLE} -gt 70 ]]; then
  TITLE="${TITLE:0:67}..."
fi
```

### Phase 3 -- Generate PR Body (via render skill)

**MANDATORY TOOL CALL — Rule 24 §Camada-1 / Rule 13 Pattern 1 (INLINE-SKILL):**

Delegate body generation to `x-internal-render-pr-body` (story-0066-0003). The render skill produces a body Markdown that includes the `<!-- template-version: 1.0 -->` marker required by `audit-pr-template.sh` (story-0066-0007).

```bash
# Derive story-id from task-id: TASK-XXXX-YYYY-NNN -> story-XXXX-YYYY
STORY_ID_FOR_RENDER="story-${TASK_EPIC_ID}-${TASK_STORY_NUM}"

# Create temp file for render output (Rule 06 §Temp files — chmod 600 implicit via mktemp)
TMP_BODY_PATH=$(mktemp -t pr-body-XXXXXX.md)
chmod 600 "$TMP_BODY_PATH"
trap 'rm -f "$TMP_BODY_PATH"' EXIT

echo "INFO: Invoking x-internal-render-pr-body --kind=implementation --story-id=$STORY_ID_FOR_RENDER"
```

Invoke the `x-internal-render-pr-body` skill via the Skill tool:

    Skill(skill: "x-internal-render-pr-body", model: "haiku", args: "--kind=implementation --story-id=$STORY_ID_FOR_RENDER --out=$TMP_BODY_PATH")

```bash
RENDER_EXIT=$?

if [[ $RENDER_EXIT -eq 0 ]]; then
  # Happy path: render skill produced body with template-version marker
  BODY=$(cat "$TMP_BODY_PATH")
else
  # Fallback path — see ## Recovery section
  echo "WARN [render-fallback]: PR body gerado via fallback inline (sem template-version marker)." >&2
  echo "     audit-pr-template.sh reportará PR_TEMPLATE_VIOLATION para este PR." >&2
  echo "     Investigar: x-internal-render-pr-body exit code = $RENDER_EXIT." >&2

  # Generate legacy inline body (see ## Recovery for full template)
  BODY="<inline body — see ## Recovery section>"
fi
```

If `--draft` is set, prepend to the body:

```markdown
> [DRAFT] This PR is not ready for review
```

#### Phase 3.5 -- Inject Orchestrator Evidence (EPIC-0059, story-0059-0007 + EPIC-0066, story-0066-0005 dedup)

Unless `--no-story-evidence` is set, ensure the body contains exactly one `## Orchestrator Evidence` section. The render skill (Phase 3 happy path) already includes the section via `_TEMPLATE-PR-IMPLEMENTATION.md`; the fallback inline body (Phase 3 ## Recovery path) does NOT — it must be injected manually here.

**Deduplication rule (story-0066-0005):**

```bash
# Skip if explicitly opted out (chore/docs PRs without a story context)
if [[ "${NO_STORY_EVIDENCE:-false}" != "true" ]]; then
  # Dedup: if render skill already produced ## Orchestrator Evidence, skip injection
  if echo "$BODY" | grep -q "^## Orchestrator Evidence"; then
    echo "INFO: ## Orchestrator Evidence already present in rendered template — skipping injection"
  else
  # Extract story ID from task ID: TASK-0059-0007-001 -> story-0059-0007
  STORY_ID_FOR_EVIDENCE="story-${TASK_EPIC_ID}-${TASK_STORY_NUM}"

  # Orchestrator commit SHA: last commit matching x-implement-story in log
  ORCH_SHA=$(git log --grep="x-implement-story" -1 --format="%H" 2>/dev/null)
  if [[ -z "$ORCH_SHA" ]]; then
    # Fallback: use current HEAD SHA
    ORCH_SHA=$(git rev-parse HEAD)
  fi

  # Phase 1 artifacts (6 planning artifacts that exist on disk)
  EPIC_PLANS_DIR="ai/epics/epic-${TASK_EPIC_ID}/plans"
  P1_ARTIFACTS=""
  for artifact in \
    "arch-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "plan-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "tests-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "tasks-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "security-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "compliance-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md"; do
    full_path="${EPIC_PLANS_DIR}/${artifact}"
    if [[ -f "$full_path" ]]; then
      P1_ARTIFACTS="${P1_ARTIFACTS:+${P1_ARTIFACTS}, }${full_path}"
    fi
  done
  [[ -z "$P1_ARTIFACTS" ]] && P1_ARTIFACTS="(none found)"

  # Phase 3 artifacts (4 verification/review artifacts that exist on disk)
  EPIC_REPORTS_DIR="ai/epics/epic-${TASK_EPIC_ID}/reports"
  P3_ARTIFACTS=""
  for artifact in \
    "verify-envelope-${TASK_EPIC_ID}-${TASK_STORY_NUM}.json" \
    "story-completion-report-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md"; do
    full_path="${EPIC_REPORTS_DIR}/${artifact}"
    if [[ -f "$full_path" ]]; then
      P3_ARTIFACTS="${P3_ARTIFACTS:+${P3_ARTIFACTS}, }${full_path}"
    fi
  done
  for artifact in \
    "review-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md" \
    "techlead-review-story-${TASK_EPIC_ID}-${TASK_STORY_NUM}.md"; do
    full_path="${EPIC_PLANS_DIR}/${artifact}"
    if [[ -f "$full_path" ]]; then
      P3_ARTIFACTS="${P3_ARTIFACTS:+${P3_ARTIFACTS}, }${full_path}"
    fi
  done
  [[ -z "$P3_ARTIFACTS" ]] && P3_ARTIFACTS="(none found)"

  ORCHESTRATOR_EVIDENCE_SECTION="
## Orchestrator Evidence

<!-- Filled automatically by x-create-pr. Do not edit manually. -->

| Campo | Valor |
| :--- | :--- |
| Story IDs | ${STORY_ID_FOR_EVIDENCE} |
| Orchestrator Commit SHA | ${ORCH_SHA} |
| Invocation Skill | x-implement-story |
| Phase 1 Artifacts | ${P1_ARTIFACTS} |
| Phase 3 Artifacts | ${P3_ARTIFACTS} |
"

    BODY="${BODY}${ORCHESTRATOR_EVIDENCE_SECTION}"
  fi
fi
```

**Invariant (story-0066-0005):** the final PR body always contains exactly one `## Orchestrator Evidence` occurrence, regardless of whether it came from the render skill (template) or the fallback inline body.

**`--no-story-evidence` flag:** when set, the `## Orchestrator Evidence` section is omitted and `audit-pr-evidence.sh` will accept the PR. Use ONLY for chore/docs PRs that genuinely have no story context (e.g., CHANGELOG-only commits, dependency bumps). When set, `audit-pr-evidence.sh` logs `"no evidence required (--no-story-evidence)"` and exits 0.

### Phase 4 -- Label Auto-Creation

Ensure labels exist in the repository, creating them if missing:

```bash
# Label definitions (base set derived from task ID)
LABELS=(
  "task|#0075ca|Individual task PR"
  "story-XXXX-YYYY|#e4e669|Parent story reference"
  "epic-XXXX|#d73a4a|Parent epic reference"
)

# Orchestrator-supplied --epic-id adds/overrides an explicit epic-XXXX label
if [[ -n "$EPIC_ID_OVERRIDE" ]]; then
  LABELS+=("epic-${EPIC_ID_OVERRIDE}|#d73a4a|Parent epic reference (orchestrator override)")
fi

for LABEL_DEF in "${LABELS[@]}"; do
  IFS='|' read -r NAME COLOR DESC <<< "$LABEL_DEF"
  # Check if label exists; create if not
  gh label list --search "$NAME" --json name -q '.[].name' | grep -qx "$NAME" || \
    gh label create "$NAME" --color "${COLOR#\#}" --description "$DESC"
done
```

### Phase 5 -- Create PR

Determine target branch and create the PR:

```bash
# Target branch logic (precedence: --target-branch > --auto-approve-pr > develop)
if [[ -n "$TARGET_BRANCH_OVERRIDE" ]]; then
  TARGET_BRANCH="$TARGET_BRANCH_OVERRIDE"
elif [[ "$AUTO_APPROVE" == "true" ]]; then
  TARGET_BRANCH="$PARENT_BRANCH"
else
  TARGET_BRANCH="develop"
fi

# Push branch if needed
git push -u origin "$CURRENT_BRANCH"

# Build gh pr create command
DRAFT_FLAG=""
if [[ "$DRAFT" == "true" ]]; then
  DRAFT_FLAG="--draft"
fi

# Assemble label list (epic-ID override appended when present)
LABEL_LIST="task,story-XXXX-YYYY,epic-XXXX"
if [[ -n "$EPIC_ID_OVERRIDE" ]]; then
  LABEL_LIST="${LABEL_LIST},epic-${EPIC_ID_OVERRIDE}"
fi

gh pr create \
  --base "$TARGET_BRANCH" \
  --title "$TITLE" \
  --body "$BODY" \
  --label "$LABEL_LIST" \
  $DRAFT_FLAG
```

After creation, report the result (extended response includes `targetBranch` and `autoMergeEnabled`):

```
PR #42 created: https://github.com/owner/repo/pull/42
  Title:        feat(TASK-0029-0001-001): add validation
  Target:       develop
  Labels:       task, story-0029-0001, epic-0029
  Draft:        false
  AutoMerge:    false
```

### Phase 6 -- Post-Create Auto-Merge (Optional)

When `--auto-merge` is set to a strategy other than `none`, delegate to `x-merge-pr` so the PR is queued for automatic merge once checks pass. Uses Pattern 1 (INLINE-SKILL) per Rule 13.

```
Skill(skill: "x-merge-pr", args: "--pr <PR_NUMBER> --strategy <AUTO_MERGE_STRATEGY> --auto")
```

Set `autoMergeEnabled=true` in the skill response when the delegated call succeeds; otherwise fall through with `autoMergeEnabled=false` and a WARN line so the orchestrator can decide whether to retry.

Backward compatibility: when `--auto-merge` is absent (default `none`), Phase 6 is a no-op and the response reports `autoMergeEnabled=false`.

## Recovery (Render Skill Fallback — EPIC-0066, story-0066-0005)

When `x-internal-render-pr-body` returns exit ≠ 0 in Phase 3, this skill falls back to the legacy inline body generator to ensure PR creation never aborts (RULE-004 fail-open). The fallback body lacks the `<!-- template-version: 1.0 -->` marker — `audit-pr-template.sh` (story-0066-0007) will block the merge with `PR_TEMPLATE_VIOLATION`. This is intentional: the preventive render path produces the marker; the detective audit gate enforces it.

The WARN message includes `RENDER_EXIT` (the render skill exit code) so the operator can diagnose the cause:

| Render exit | Cause | Action |
| :--- | :--- | :--- |
| 1 / 2 / 3 | Bug, template missing, story-id derivation failed | Investigate via `RENDER_EXIT`; rerun `mvn process-resources` if template missing |

Full fallback body template, dedup interaction, and RULE-004 contract detail in [`references/recovery.md`](references/recovery.md) per ADR-0007.

## Error Handling

| Scenario | Action |
|----------|--------|
| Invalid task ID format | ABORT with format explanation |
| Branch naming mismatch | ABORT: "Current branch does not match task naming convention: feat/task-XXXX-YYYY-NNN-*" |
| Tests fail | ABORT: "Tests failed. Fix tests before creating PR." |
| Parent branch not found (auto-approve mode) | ABORT: "Parent branch not found for story-XXXX-YYYY" |
| Label creation fails | WARN and continue (PR can be created without labels) |
| gh CLI not available | ABORT: "gh CLI is required. Install from https://cli.github.com" |
| PR already exists for branch | ABORT: "PR already exists for this branch. Use gh pr view to check." |
| `--epic-id` fails regex `^\d{4}$` | ABORT exit 5 `INVALID_EPIC_ID`: "Epic ID must be 4 digits" |
| `--auto-merge` != none without `--target-branch` | ABORT exit 6 `AUTO_MERGE_REQUIRES_TARGET`: "--auto-merge requires --target-branch" |
| `--target-branch develop` combined with `--auto-merge` != none | WARN and continue: "Auto-merge directly to develop is unusual; consider using an epic branch" |
| `x-merge-pr` delegation fails in Phase 6 | WARN and continue: PR is created, `autoMergeEnabled=false`, orchestrator retries |
| `git log --grep` returns empty SHA | Fall back to `git rev-parse HEAD` for `Orchestrator Commit SHA` field |
| No Phase 1/3 artifacts found on disk | Use `(none found)` as value; PR is still created (audit validates at CI time) |

## Integration Notes

| Skill | Relationship | Context |
|-------|-------------|---------|
| `x-commit-changes` | predecessor | Commits are created before PR |
| `x-push-branch` | alternative | x-push-branch handles general git workflow; x-create-pr is task-specific |
| `x-implement-story` | called-by | Phase 5 (PR creation) delegates to this skill for task-level PRs |
| `x-execute-tests` | called-by | Phase 1 pre-check runs the test command |
| `x-merge-pr` | called (Phase 6) | Invoked when `--auto-merge` != `none` to enable GitHub auto-merge with the chosen strategy |
| `x-implement-epic` | caller | Propagates `--target-branch epic/XXXX --auto-merge merge --epic-id XXXX` OO-style (RULE-009) |

## Examples

### Basic Task PR

```
/x-create-pr TASK-0029-0001-001
```

Creates PR:
- Title: `feat(TASK-0029-0001-001): add validation`
- Target: `develop`
- Labels: `task`, `story-0029-0001`, `epic-0029`

### Draft PR

```
/x-create-pr TASK-0029-0001-001 --draft
```

Creates draft PR with `[DRAFT] This PR is not ready for review` banner.

### Auto-Approve Mode

```
/x-create-pr TASK-0029-0001-001 --auto-approve-pr
```

Creates PR targeting parent story branch (`feat/story-0029-0001-*`) instead of `develop`.

### Custom Description

```
/x-create-pr TASK-0029-0001-001 --description "implement user input validation"
```

Creates PR with title: `feat(TASK-0029-0001-001): implement user input validation`

### Target an Epic Branch (orchestrator propagation)

```
/x-create-pr TASK-0049-0016-001 --target-branch epic/0049
```

Creates PR with `--base epic/0049`. Legacy label set is preserved; no auto-merge is enabled.

### Full Orchestrator Chain (target + auto-merge + epic-id)

```
/x-create-pr TASK-0049-0016-001 --target-branch epic/0049 --auto-merge merge --epic-id 0049
```

Creates PR with `--base epic/0049`, label `epic-0049` injected, and delegates Phase 6 to `x-merge-pr --pr <N> --strategy merge --auto`. Response includes `autoMergeEnabled=true`.

### Error -- auto-merge without target-branch

```
/x-create-pr TASK-0049-0016-001 --auto-merge merge
```

Exits with code `6` (`AUTO_MERGE_REQUIRES_TARGET`): "--auto-merge requires --target-branch".

### Error -- invalid epic-id

```
/x-create-pr TASK-0049-0016-001 --target-branch epic/0049 --epic-id 49
```

Exits with code `5` (`INVALID_EPIC_ID`): "Epic ID must be 4 digits".
