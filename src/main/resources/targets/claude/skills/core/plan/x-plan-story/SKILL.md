---
name: x-plan-story
description: "Multi-agent story planning: 7 specialized agents produce task breakdown and plans."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "[STORY-ID] [--force] [--skip-dor] [--dry-run] [--no-commit]"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

## Triggers

```
/x-plan-story STORY-ID             — plan story with 7 parallel agents
/x-plan-story STORY-ID --force     — regenerate even if artifacts are fresh
/x-plan-story STORY-ID --skip-dor  — skip Phase 5 DoR validation
/x-plan-story STORY-ID --dry-run   — write artifacts but skip subagent/commit steps
/x-plan-story STORY-ID --no-commit — skip commit step (for orchestrators batch-committing)
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `STORY-ID` | positional | (required) | `story-XXXX-YYYY` format |
| `--force` | boolean | `false` | Regenerate all artifacts even if fresh |
| `--skip-dor` | boolean | `false` | Skip Phase 5 DoR validation |
| `--dry-run` | boolean | `false` | Artifacts written but Steps P1/P2/P4/P5 become no-ops |
| `--no-commit` | boolean | `false` | Skip commit step; used by orchestrators batching commits at parent level |

**CRITICAL:** 6 phases (0-5) all mandatory (unless `--skip-dor` skips Phase 5). Never stop before Phase 5. Print `>>> Phase N/5 completed. Proceeding to Phase N+1...` after each phase.

## Output Contract

**Schema dispatch:**

| `planningSchemaVersion` | Phases run |
|--------------------------|------------|
| `"1.0"` (or absent) | Phases 0-5 (legacy flow: task breakdown + planning report + DoR) |
| `"2.0"` | Phases 0-5 + 4a-4c (v2: task-TASK-NNN.md + plan-task-TASK-NNN.md per task + task-implementation-map-STORY-*.md) |

**Artifacts produced:**

| Artifact | Path |
|----------|------|
| Task breakdown | `ai/epics/epic-XXXX/plans/tasks-story-XXXX-YYYY.md` |
| Planning report | `ai/epics/epic-XXXX/plans/planning-report-story-XXXX-YYYY.md` |
| DoR checklist | `ai/epics/epic-XXXX/plans/dor-story-XXXX-YYYY.md` |
| Task files (v2) | `ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md` per task |
| Task plans (v2) | `ai/epics/epic-XXXX/plans/plan-task-TASK-XXXX-YYYY-NNN.md` per task |
| Task map (v2) | `ai/epics/epic-XXXX/plans/task-implementation-map-STORY-XXXX-YYYY.md` |

**Phase execution with telemetry:**

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-P1-Worktree-Detect`

**Step P1 (Worktree Detect):** `Skill(skill: "x-manage-worktrees", args: "detect-context")` — advisory, fail-open. Skip when `--no-commit`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-P1-Worktree-Detect ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-P2-Epic-Branch-Ensure`

**Step P2 (Epic Branch Ensure):** `Skill(skill: "x-internal-ensure-epic-branch", args: "--epic-id <XXXX>")` — idempotent. Abort on failure with `EPIC_BRANCH_ENSURE_FAILED`. Skip when `--no-commit` or `--dry-run`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-P2-Epic-Branch-Ensure ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-1-Context-Gathering`

**Phase 1 (Context Gathering):** Read story, epic, implementation map, and existing plan artifacts inline. Also read `.claude/skills/planning-standards-kp/SKILL.md` (RA9 contract: 9 sections, Packages format, Decision Rationale micro-template). See staleness check and context-combination matrix in references.

> **RA9 guidance (EPIC-0056 / planning-standards-kp):** When producing the consolidated plan, the Architect subagent MUST fill:
> - **Section 2 (Packages Hexagonal):** Feature-level packages in each hexagonal layer from the story analysis.
> - **Section 8 (Decision Rationale):** At least 1 feature-level decision using the 4-line micro-template.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-1-Context-Gathering ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-2-Parallel-Planning`

**Phase 2 (Parallel Planning):** Dispatch 7 subagents in a **single message** (Rule 13 Pattern 2 — SUBAGENT-GENERAL) for true parallelism.

<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story Architect`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story QA`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story Security`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story PentestEngineer`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story TechLead`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story PO`
<!-- TELEMETRY: subagent.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-start x-plan-story PerformanceEngineer`

Dispatch all 7 in ONE assistant message:

    Agent(subagent_type: "architect", model: "opus", description: "Architect planning for story {STORY_ID}", prompt: "Read context files. Analyze story {STORY_ID}. Produce TASK_PROPOSAL entries (architecture, layers, dependencies). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "qa-engineer", model: "sonnet", description: "QA planning for story {STORY_ID}", prompt: "Read context files. Produce TASK_PROPOSAL entries (tests, coverage, acceptance criteria). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "security-engineer", model: "sonnet", description: "Security planning for story {STORY_ID}", prompt: "Read knowledge/security/application-security.md, knowledge/security/security-principles.md, and context files. Produce TASK_PROPOSAL entries (security, OWASP, threat model). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "pentest-engineer", model: "sonnet", description: "Pentest planning for story {STORY_ID}", prompt: "Read capabilities/quality/pentest/pentest-always-on.yaml and context files. Produce TASK_PROPOSAL entries (pentest scenarios, CVSS-rated vulnerabilities, exploitation paths). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "tech-lead", model: "sonnet", description: "Tech Lead planning for story {STORY_ID}", prompt: "Read context files. Produce TASK_PROPOSAL entries (code quality, SOLID, complexity). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "product-owner", model: "sonnet", description: "PO planning for story {STORY_ID}", prompt: "Read knowledge/compliance.md and context files. Produce TASK_PROPOSAL entries (business value, acceptance, DoD, compliance). Follow TASK_PROPOSAL format in references/full-protocol.md.")
    Agent(subagent_type: "performance-engineer", model: "sonnet", description: "Performance planning for story {STORY_ID}", prompt: "Read context files. Produce TASK_PROPOSAL entries (latency SLAs, throughput targets, load testing scenarios). Follow TASK_PROPOSAL format in references/full-protocol.md.")

<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story Architect ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story QA ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story Security ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story PentestEngineer ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story TechLead ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story PO ok`
<!-- TELEMETRY: subagent.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh subagent-end x-plan-story PerformanceEngineer ok`

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-2-Parallel-Planning ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-3-Consolidation`

**Phase 3 (Consolidation):** Merge all TASK_PROPOSAL entries using deterministic rules (majority-vote, duplicate elimination, dependency ordering). See consolidation algorithm in references.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-3-Consolidation ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-4-Artifact-Generation`

**Phase 4 (Artifact Generation):** Write `tasks-story-*.md` and `planning-report-*.md`. v2 only: Phases 4a-4c emit per-task artifacts and task map (see references).

**Phase 4b (v2 only — batch task-plan dispatch):** For each TASK-XXXX-YYYY-NNN, invoke `x-plan-task` in parallel (batch ≤ 4) with `--no-commit` so the caller aggregates into a single Step P4 commit:

    Skill(skill: "x-plan-task",
          args: "--task-file ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md --no-commit")

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-4-Artifact-Generation ok`

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-5-DoR-Validation`

**Phase 5 (DoR Validation):** Run 12 checks; v2 adds per-task READY checks. Skipped with `--skip-dor`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-5-DoR-Validation ok`

### Step P4 — Batch Planning Commit (EPIC-0049 / RULE-007)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-P4-Batch-Commit`

If `--dry-run` is set, log `"dry-run, skipping commit"` and skip this step. If `--no-commit` is set, skip as well — the parent orchestrator (e.g., `x-orchestrate-epic`) aggregates commits at the wave level.

Otherwise, issue ONE consolidated commit covering every planning artifact produced by this story (task breakdown, planning report, DoR checklist, plan-story, all task files, all plan-task files, task map, and the updated `execution-state.json`):

    Skill(skill: "x-commit-planning",
          args: "--scope docs --epic-id <XXXX> --paths ai/epics/epic-<XXXX>/plans/ ai/epics/epic-<XXXX>/execution-state.json --subject \"docs(story-<XXXX>-<YYYY>): add planning artifacts\"")

Idempotency: re-executing with identical inputs returns `commitSha=null` (silent no-op). On `COMMIT_FAILED` (exit 4), abort with the same code.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-P4-Batch-Commit ok`

### Step P5 — Push Epic Branch to Origin (optional, EPIC-0049)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-story Phase-P5-Push`

If `--dry-run` or `--no-commit` is set, log `"dry-run, skipping push"` and skip this step.

Otherwise, delegate the push to `x-push-branch` so the canonical `epic/<XXXX>` branch is synchronized with origin:

    Skill(skill: "x-push-branch", args: "--branch epic/<XXXX>")

On push failure (remote rejection, no connectivity), log a WARNING and continue — the local commit is preserved; the operator can re-run Step P5 or `git push` manually. Do NOT abort.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-story Phase-P5-Push ok`

## Error Envelope

| Code | Condition |
|------|-----------|
| `STORY_NOT_FOUND` | Story file absent at `ai/epics/epic-XXXX/story-XXXX-YYYY.md` |
| `EPIC_BRANCH_ENSURE_FAILED` | Step P2 `x-internal-ensure-epic-branch` non-zero |
| `CONSOLIDATION_FAILED` | No TASK_PROPOSAL entries returned by any subagent |
| `WRITE_FAILED` | Unable to write output artifact to `ai/epics/epic-XXXX/plans/` |
| `DOR_NOT_MET` | DoR validation returns < 12/12 checks passed |

## Full Protocol

> Complete per-phase detail (Phase 0 input resolution, staleness check, context-combination table, TASK_PROPOSAL format, consolidation algorithm with deterministic merge rules, Phase 3 conflict resolution, Phase 4 artifact templates, v2 Phases 4a–4c task-file-first execution, x-plan-task delegation per task, Phase 5 12-check DoR validation, commit conventions) and planning-guide reference in [`references/full-protocol.md`](references/full-protocol.md). Existing [`references/planning-guide.md`](references/planning-guide.md) preserved per story-0054-0003 audit.
