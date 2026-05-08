---
name: x-create-feature
model: sonnet
description: "Creates a complete feature (Epic + Stories + Map) from a spec file in an isolated worktree."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, AskUserQuestion, Skill
argument-hint: "[SPEC-FILE-PATH] --epic-id <NNNN> [--no-jira] [--dry-run]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for technical terms (cache, timeout, handler, endpoint) and code identifiers.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-create-feature (Feature Creation Chain Orchestrator)

## Purpose

Orchestrate the full creation of a feature from a system specification: an **Epic**, individual **Story** files, and an **Implementation Map**. The entire operation runs inside an isolated worktree (`feature-XXXX-<slug>`), producing a `docs/<epic-id>-<slug>` branch with a single consolidated commit, auto-merged into `epic/XXXX` via PR.

**EPIC-0065 hard-cut (Rule 19 §Hard-cut autorizado):** This skill supersedes `x-epic-decompose` (removed). Internal sub-skills `x-internal-create-epic`, `x-internal-map-epic`, and `x-internal-create-story` are now the implementation of the generation phases (not user-invocable).

## When to Use

- Create a complete feature backlog from a spec: epic + stories + implementation map
- After reviewing the spec PR produced by `x-ideate-feature`
- User wants to turn a specification into immediately implementable work items

## Prerequisites

Read before starting:

- `.claude/templates/_TEMPLATE-EPIC.md` (RA9 v2: 9 sections)
- `.claude/templates/_TEMPLATE-STORY.md` (RA9 v2: 9 sections)
- `.claude/templates/_TEMPLATE-IMPLEMENTATION-MAP.md`
- `.claude/skills/planning-standards-kp/SKILL.md` — **Mandatory** RA9 9-section model (source of truth), granularity per level (Epic/Story/Task), Packages Hexagonal catalog format, Decision Rationale micro-template
- `references/decomposition-guide.md` (bundled with this skill)

If any template is missing, stop and tell the user.

## Workflow Overview

```
P1.5 WORKTREE      -> x-internal-precheck-worktree + x-manage-worktrees create feature-XXXX-<slug>
P2.  BRANCH        -> x-internal-ensure-epic-branch --epic-id XXXX (RULE-001)
1.   ANALYSIS      -> Read spec, identify rules, stories, dependencies, phases (inline)
1.5  JIRA          -> Determine Jira integration mode (conditional, inline)
2.   EPIC          -> x-internal-create-epic instructions (Phase 2 below)
3.   STORIES       -> x-internal-create-story instructions (Phase 3 below)
4.   MAP           -> x-internal-map-epic instructions (Phase 4 below)
4.5  JIRA LINKS    -> Create Jira dependency links (conditional, inline)
P4.  COMMIT        -> x-commit-planning consolidated commit on docs/<epic-id>-<slug>
P5.  PUSH          -> git push docs/<epic-id>-<slug> to origin
P6.  PR            -> x-create-pr --target-branch epic/XXXX --auto-merge merge --label docs
P7.  CI-WATCH      -> x-watch-pr-ci MANDATORY (Rule 45)
5.   REPORT        -> Save all files, validate quality, report summary
```

### Note on consolidated commit

`x-create-feature` uses a **single consolidated commit** in Phase P4 covering the full batch (epic + N stories + implementation map). Sub-skill phases write artifacts but do NOT commit individually — the orchestrator owns the commit on branch `docs/<epic-id>-<slug>`, not on `epic/XXXX` directly.

---

## Phase P1.5 — Worktree Setup (RULE-001 + EPIC-0065 D-R5)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P1_5-Worktree`

**P1.5.1 — Idempotent re-entrancy check:**

    Skill(skill: "x-internal-precheck-worktree", model: "haiku", args: "--identifier feature-<epic-id>-<slug> --allow-dirty")

- Exit 0 (CLEAN): proceed to worktree creation.
- Exit 15 (WORKTREE_AMBIGUOUS): log warning and proceed with caution.
- On non-zero for other reasons: abort with `WORKTREE_FAILED`.

**P1.5.2 — Create feature worktree:**

Derive slug from the spec file title or filename (kebab-case, max 40 chars, ASCII-only).

    Skill(skill: "x-manage-worktrees", model: "haiku", args: "create --identifier feature-<epic-id>-<slug> --branch docs/<epic-id>-<slug> --base epic/<epic-id>")

- Worktree path: `.claude/worktrees/feature-<epic-id>-<slug>/`
- Branch: `docs/<epic-id>-<slug>` (base: `epic/XXXX`, per D-R6 — docs PRs auto-merge into epic branch)
- All subsequent file writes happen inside this worktree.
- Worktree is creator-owned (Rule 14 §Invariants) — removed only after successful PR merge.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P1_5-Worktree ok`

---

## Phase P2 — Ensure `epic/<ID>` Branch (RULE-001)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P2-Epic-Branch`

Ensure the canonical `epic/<ID>` branch exists locally AND on origin before writing any artifact:

    Skill(skill: "x-internal-ensure-epic-branch", model: "haiku", args: "--epic-id <XXXX>")

Abort with `EPIC_BRANCH_ENSURE_FAILED` on any non-zero exit.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P2-Epic-Branch ok`

---

## Phase 1 — Analysis

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-1-Analysis`

1. Read the spec file (path from argument or the spec PR produced by `x-ideate-feature`)
2. Read `references/decomposition-guide.md`
3. Analyze the spec:
   - Identify cross-cutting rules (spanning multiple journeys)
   - Identify stories by layer (foundation → core → extensions → compositions → cross-cutting)
   - Map dependencies between stories
   - Compute phases from the dependency DAG
   - Identify the critical path

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-1-Analysis ok`

---

## Phase 1.5 — Jira Integration Decision

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-1_5-Jira`

Before generating artifacts, determine Jira integration mode. Logic unchanged from predecessor:

- If `--no-jira`: `jiraContext = { enabled: false }`, skip.
- If `--jira <PROJECT_KEY>`: discover `cloudId` via `mcp__atlassian__getAccessibleAtlassianResources`, set `jiraContext`.
- If no flag: prompt via `AskUserQuestion` (3 options: all Jira / epic only / markdown only).

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-1_5-Jira ok`

---

## Phase 2 — Generate the Epic (x-internal-create-epic)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-2-Epic`

Follow the instructions in `.claude/skills/x-internal-create-epic/SKILL.md`:

- Determine epic number (scan `ai/epics/` for existing `epic-XXXX-*` folders; use `--epic-id` override when supplied)
- Create directory `ai/epics/epic-XXXX-<slug>/` inside the worktree
- Extract rules → RULE-001..N table
- Build story index with titles and dependencies
- Define DoR/DoD (must include TDD Compliance + Double-Loop TDD)
- Generate `ai/epics/epic-XXXX-<slug>/epic-XXXX.md` following `_TEMPLATE-EPIC.md`

**Jira integration:** same logic as predecessor (create Epic issue if `jiraContext.enabled`).

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-2-Epic ok`

---

## Phase 3 — Generate the Stories (x-internal-create-story)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-3-Stories`

Follow the instructions in `.claude/skills/x-internal-create-story/SKILL.md`:

For each story in the Epic index:
- Dependencies, applicable rules, user story description, technical context
- Entrega de Valor (Section 3.5) — measurable business value
- Data contracts (precise field names, types, formats)
- Mermaid sequence diagrams (real component names)
- Gherkin acceptance criteria (all 4 mandatory categories, TPP order)
- Sub-tasks tagged `[Dev]`, `[Test]`, `[Doc]` — at least one `[Test] Smoke/E2E`

Generate files as `ai/epics/epic-XXXX-<slug>/story-XXXX-YYYY.md` following `_TEMPLATE-STORY.md`.

**Jira integration:** same cascade logic as predecessor.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-3-Stories ok`

---

## Phase 4 — Generate the Implementation Map (x-internal-map-epic)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-4-Map`

Follow the instructions in `.claude/skills/x-internal-map-epic/SKILL.md`:

- Dependency matrix (all stories, validated for consistency)
- Phase diagram (ASCII box-drawing)
- Critical path analysis
- Mermaid dependency graph (color-coded by phase)
- Phase summary and detail tables
- Strategic observations

Generate `ai/epics/epic-XXXX-<slug>/IMPLEMENTATION-MAP.md` following `_TEMPLATE-IMPLEMENTATION-MAP.md`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-4-Map ok`

---

## Phase 4.5 — Jira Dependency Linking (conditional)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-4_5-Jira-Links`

If `jiraContext.enabled == true` and `jiraContext.cascadeToStories == true`: create Jira dependency links between stories via `mcp__atlassian__createIssueLink`. Best-effort — failures are warned, not fatal.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-4_5-Jira-Links ok`

---

## Phase P4 — Consolidated Planning Commit (RULE-007)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P4-Commit`

If `--dry-run`: log `"dry-run, skipping commit"` and skip.

Collect the full path set (epic + all stories + map) and commit via `x-commit-planning`:

    Skill(skill: "x-commit-planning", model: "haiku",
          args: "--scope chore --epic-id <XXXX> --paths ai/epics/epic-<XXXX>-<slug>/epic-<XXXX>.md,ai/epics/epic-<XXXX>-<slug>/story-*.md,ai/epics/epic-<XXXX>-<slug>/IMPLEMENTATION-MAP.md --subject \"full decomposition (epic + <N> stories + map)\" --branch docs/<epic-id>-<slug>")

On `COMMIT_FAILED` (exit 4): abort. On `noOp=true`: silent no-op.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P4-Commit ok`

---

## Phase P5 — Push docs Branch

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P5-Push`

If `--dry-run`: skip.

Push the docs branch to origin:

```bash
git push -u origin docs/<epic-id>-<slug>
```

On push failure: log WARNING and continue. Local commit preserved.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P5-Push ok`

---

## Phase P5.5 — BACKLOG-RENDER (EPIC-0066, story-0066-0006)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P55-Render`

**MANDATORY TOOL CALL — Rule 24 §Camada-1 / Rule 13 Pattern 1 (INLINE-SKILL):**

Render the backlog PR body from the committed epic.md + IMPLEMENTATION-MAP.md + stories. This produces a body Markdown that includes the `<!-- template-version: 1.0 -->` marker required by `audit-pr-template.sh` (story-0066-0007).

```bash
TMP_BODY_PATH=$(mktemp -t feature-pr-body-XXXXXX.md)
chmod 600 "$TMP_BODY_PATH"
trap 'rm -f "$TMP_BODY_PATH"' EXIT
echo "INFO: Invoking x-internal-render-pr-body --kind=backlog --epic-id=$EPIC_ID"
```

Invoke the `x-internal-render-pr-body` skill via the Skill tool:

    Skill(skill: "x-internal-render-pr-body", model: "haiku", args: "--kind=backlog --epic-id=epic-<EPIC_ID> --out=$TMP_BODY_PATH")

```bash
RENDER_EXIT=$?
if [[ $RENDER_EXIT -eq 0 ]]; then
  PR_BODY_FILE="$TMP_BODY_PATH"   # rendered body with template-version marker
else
  echo "WARN [render-fallback]: x-internal-render-pr-body falhou (exit $RENDER_EXIT). Usando body manual." >&2
  echo "     audit-pr-template.sh reportará PR_TEMPLATE_VIOLATION para este PR." >&2
  PR_BODY_FILE=""   # empty → fallback path in Phase P6
fi
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P55-Render ok`

---

## Phase P6 — PR Creation (docs → epic/XXXX)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P6-PR`

If `--dry-run`: skip.

Create the docs PR using the body rendered in Phase P5.5. Auto-merge is authorized for `docs/<epic-id>-*` → `epic/XXXX` per Rule 21 §Anti-Patterns exception. The flow uses `gh pr create --body-file` (not the `x-create-pr` skill) because `x-create-pr` is task-scoped and renders `--kind=implementation`; backlog PRs need `--kind=backlog` (story-0066-0006).

```bash
PR_TITLE="chore(epic-${EPIC_ID}): full decomposition (epic + ${STORY_COUNT} stories + map)"

if [[ -n "$PR_BODY_FILE" ]]; then
  # Happy path — use rendered body with template-version marker
  PR_URL=$(gh pr create \
    --base "epic/${EPIC_ID}" \
    --head "docs/${EPIC_ID}-${SLUG}" \
    --title "$PR_TITLE" \
    --body-file "$PR_BODY_FILE" \
    --label docs --label "epic-${EPIC_ID}")
else
  # Fallback path — see ## Recovery (manual body without template-version marker)
  PR_URL=$(gh pr create \
    --base "epic/${EPIC_ID}" \
    --head "docs/${EPIC_ID}-${SLUG}" \
    --title "$PR_TITLE" \
    --body "$(cat <<EOF
chore(epic-${EPIC_ID}): full decomposition (epic + ${STORY_COUNT} stories + map)

(rendered via fallback path — body lacks template-version marker; audit will block merge)
EOF
)" \
    --label docs --label "epic-${EPIC_ID}")
fi

# Enable auto-merge (Rule 21 §Anti-Patterns docs/* exception)
PR_NUMBER=$(echo "$PR_URL" | grep -oE '[0-9]+$')
gh pr merge "$PR_NUMBER" --merge --auto

# Capture {prUrl, prNumber} for Phase P7
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P6-PR ok`

---

## Phase P7 — CI-Watch (Rule 45 — MANDATORY)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-P7-CIWatch`

**MANDATORY TOOL CALL — NON-NEGOTIABLE (Rule 45):** Invoke `x-watch-pr-ci` to wait for CI + merge status:

    Skill(skill: "x-watch-pr-ci", model: "haiku", args: "--pr-number <prNumber>")

Persist `.claude/state/pr-watch-{prNumber}.json` (Rule 45 evidence).

Dispatch on exit code:
- `SUCCESS` (0): CI green + PR merged → proceed to Phase 5 (report)
- `CI_PENDING_PROCEED` (10): CI green, no Copilot review → proceed with WARNING
- `CI_FAILED` (20): block; log `CI_FAILED` and abort
- `TIMEOUT` (30): surface to operator; abort
- `PR_ALREADY_MERGED` (40): idempotent → proceed
- `NO_CI_CONFIGURED` (50): skip CI gate; proceed
- `PR_CLOSED` (60): abort
- `PR_NOT_FOUND` (70): abort

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-P7-CIWatch ok`

---

## Phase 5 — Save and Report

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-create-feature Phase-5-Report`

All files saved inside `ai/epics/epic-XXXX-<slug>/` (v4 layout — D-R1). Quality validation:

- [ ] Every story has Entrega de Valor (measurable, not technical)
- [ ] Every story has at least one `[Test] Smoke/E2E` sub-task
- [ ] Every story DoD includes automated test requirement
- [ ] Dependencies are symmetric
- [ ] No circular dependencies
- [ ] Phase computation is correct

Report summary:
- Total rules extracted, stories generated, phases computed, critical path, max parallelism
- PR: `#<prNumber>` — `<prUrl>` — Status: MERGED (or AWAITING CI)
- Jira summary (if applicable)

Print:
```
====================================================
 x-create-feature — Feature ready
====================================================
 Epic:    ai/epics/epic-XXXX-<slug>/epic-XXXX.md
 Stories: N files under ai/epics/epic-XXXX-<slug>/
 Map:     ai/epics/epic-XXXX-<slug>/IMPLEMENTATION-MAP.md
 Branch:  docs/<epic-id>-<slug>
 PR:      #<prNumber> — <prUrl> — MERGED
----------------------------------------------------
 Next step: /x-implement-epic XXXX
====================================================
```

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-create-feature Phase-5-Report ok`

---

## Recovery (Render Skill Fallback — EPIC-0066, story-0066-0006)

When `x-internal-render-pr-body` returns exit ≠ 0 in Phase P5.5, this skill falls back to a minimal manual body in Phase P6 to ensure PR creation never aborts (RULE-004 fail-open). The fallback body lacks the `<!-- template-version: 1.0 -->` marker — `audit-pr-template.sh` (story-0066-0007) will block the merge with `PR_TEMPLATE_VIOLATION`. This is intentional: the preventive render path produces the marker; the detective audit gate enforces it.

The WARN message includes `RENDER_EXIT` (the render skill exit code) so the operator can diagnose the cause: typically `OPERATIONAL_ERROR` (exit 2) when `_TEMPLATE-PR-BACKLOG.md` is missing from `.claude/templates/` (rerun `mvn process-resources`).

## Error Handling

| Scenario | Action |
|----------|--------|
| Spec file not found or empty | Abort: `Spec file not found or is empty.` |
| Template file missing | Stop and tell the user which template is missing |
| `x-internal-precheck-worktree` exit 15 | Log WARNING, proceed with caution |
| `x-manage-worktrees` fails | Abort with `WORKTREE_FAILED` |
| `x-internal-ensure-epic-branch` fails | Abort with `EPIC_BRANCH_ENSURE_FAILED` |
| `x-commit-planning` exit 4 | Abort with `COMMIT_FAILED` |
| `x-commit-planning` `noOp=true` | Silent no-op, continue to P5 |
| `x-create-pr` fails | Abort with `PR_CREATE_FAILED` |
| `x-watch-pr-ci` exit 20 (`CI_FAILED`) | Abort; preserve worktree for diagnosis |
| `--dry-run` set | Phases P4, P5, P6, P7 are no-ops |
| Circular dependency detected | Abort with cycle description |
| Story fails quality validation | Fix before saving — do not skip validation |

## Integration Notes

| Skill | Relationship | Notes |
|-------|-------------|-------|
| `x-internal-precheck-worktree` | delegates (P1.5.1) | Idempotent re-entrancy check |
| `x-manage-worktrees` | delegates (P1.5.2) | Creates `feature-XXXX-<slug>` worktree |
| `x-internal-ensure-epic-branch` | delegates (P2) | Ensures `epic/XXXX` on local + origin |
| `x-internal-create-epic` | follows instructions (Ph 2) | Generates Epic file |
| `x-internal-create-story` | follows instructions (Ph 3) | Generates Story files |
| `x-internal-map-epic` | follows instructions (Ph 4) | Generates Implementation Map |
| `x-commit-planning` | delegates (P4) | Single consolidated commit on docs branch |
| `x-create-pr` | delegates (P6) | Opens PR with auto-merge (Rule 21 docs/ exception) |
| `x-watch-pr-ci` | delegates (P7) | MANDATORY CI-watch (Rule 45) |
| `x-ideate-feature` | preceded by | Optional — produces spec PR reviewed by operator |
| `x-implement-epic` | followed by | Implements the generated epic |

## Backward Compatibility (EPIC-0065)

`x-epic-decompose` was removed via hard-cut (Rule 19 §Hard-cut autorizado — RULE-004). No alias. Operators update invocations to `/x-create-feature`. Internal sub-skills (`x-internal-create-epic`, `x-internal-map-epic`, `x-internal-create-story`) are not user-invocable (Rule 22 — visibility: internal).
