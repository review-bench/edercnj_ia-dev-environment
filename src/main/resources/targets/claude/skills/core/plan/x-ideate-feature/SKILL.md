---
name: x-ideate-feature
description: "Transform free-form prose or a text file into a structured RA9 spec (5 mandatory sections) and open a PR on docs/feature-<slug> targeting develop for human review before invoking x-create-feature."
model: opus
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Skill, Agent
argument-hint: "<prose-paragraph or file.txt|file.md>"
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: x-ideate-feature

## Purpose

Transform free-form ideation prose (or a text file) into a structured feature specification document (RA9 v2 minimal, 5 sections) and open a pull request for human review. The operator reviews and optionally edits the spec, then manually invokes `x-create-feature` to drive the full epic + story decomposition.

**RULE-005 (no auto-chain):** This skill does NOT call `x-create-feature` automatically. It terminates after opening the PR with an explicit instruction to the operator.

## Triggers

- `/x-ideate-feature "Implement CSV export with filters..."` — inline prose
- `/x-ideate-feature path/to/idea.txt` — text file as input
- User wants to convert an idea into a structured spec for engineer review

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `input` | String | Yes | Free-form prose (min 100 chars) or path to a `.txt`/`.md` file |

## Exit Codes

| Code | Name | Condition |
|------|------|-----------|
| 0 | SUCCESS | Spec created and PR opened |
| 1 | INPUT_TOO_SHORT | Prose < 100 characters |
| 2 | OPERATIONAL_ERROR | `gh` auth failure, push failure, or `git` error |
| 3 | WORKTREE_FAILED | `x-manage-worktrees` non-zero |

---

## Phase 0 — Input validation and worktree setup

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-ideate-feature Phase-0-Setup`

**0.1 Read input:**

- If argument is a file path (`.txt` or `.md`): read file content.
- Otherwise treat the argument as inline prose.
- Validate length ≥ 100 characters; if shorter emit `OPERATIONAL_ERROR: input prose too short — provide at least 100 characters` and exit 1.

**0.2 Derive slug:**

Derive a kebab-case slug from the first 5 words of the prose (ASCII-only, lowercase, max 40 chars). Example: "Implement CSV export with date filters" → `implement-csv-export-with-date`.

**0.3 Create worktree + branch:**

Invoke `x-manage-worktrees` to create an isolated working tree:

    Skill(skill: "x-manage-worktrees", model: "haiku", args: "create --identifier feature-ideation-<slug> --branch docs/feature-<slug> --base develop")

- Worktree path: `.claude/worktrees/feature-ideation-<slug>/`
- Branch: `docs/feature-<slug>` (base: `develop`)
- On non-zero exit → `WORKTREE_FAILED`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-ideate-feature Phase-0-Setup ok`

---

## Phase 1 — Prose analysis (deep-reasoning)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-ideate-feature Phase-1-Analyze`

Dispatch a deep-reasoning subagent to extract structure from the prose:

    Agent(
      subagent_type: "general-purpose",
      model: "opus",
      description: "Analyze ideation prose and extract structured elements",
      prompt: "Analyze the following ideation prose and extract:
    1. DOMAIN: The business domain and system role (1-2 sentences)
    2. SCOPE_IN: What is explicitly included (3-5 bullet points)
    3. SCOPE_OUT: What is explicitly excluded or out of scope (2-3 bullet points)
    4. RULES: Cross-cutting business rules mentioned or implied (RULE-001..N format)
    5. STORIES: Preliminary story titles (5-10 user stories in 'As a X, I need Y so that Z' format)
    6. DOR: Initial Definition of Ready criteria (3-5 items)
    7. DOD: Initial Definition of Done criteria (3-5 items)
    8. RISKS: Top 3 risks with mitigation

    Input prose:
    <PROSE>

    Return a structured JSON object with keys: domain, scopeIn, scopeOut, rules, stories, dor, dod, risks."
    )

Capture the returned structured analysis as `{domain, scopeIn, scopeOut, rules, stories, dor, dod, risks}`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-ideate-feature Phase-1-Analyze ok`

---

## Phase 2 — Spec generation (RA9 v2 minimal)

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-ideate-feature Phase-2-Spec`

Using the analysis from Phase 1, write the spec file inside the worktree.

**Output path (inside worktree):** `docs/specs/SPEC-<slug>-v1.md`

**RA9 v2 minimal structure (5 mandatory sections, ≥100 chars each):**

```markdown
# SPEC-<slug>-v1 — <Feature Title>

> **Status:** Draft  
> **Author:** (operator)  
> **Date:** <YYYY-MM-DD>  
> **Branch:** docs/feature-<slug>

---

## Sistema

<Domain description — 1-3 paragraphs covering what the system does, its role, and user stakeholders>

---

## Escopo

### Incluído
<bullet list of 3-5 explicitly included capabilities>

### Excluído
<bullet list of 2-3 explicitly excluded items>

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-001 | <cross-cutting rule description> | <which stories/layers it affects> |
...

---

## Histórias

Lista preliminar (sem decomposição detalhada — será refinada em x-create-feature):

| # | Título | Stakeholder |
|---|--------|-------------|
| 1 | <story title> | <role> |
...

---

## DoR / DoD

### Definition of Ready
- [ ] <prerequisite>
...

### Definition of Done
- [ ] <completion criterion>
...

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| <risk> | Alto/Médio/Baixo | <mitigation> |
```

Write the file to the worktree via Bash:

```bash
mkdir -p .claude/worktrees/feature-ideation-<slug>/docs/specs
# Write SPEC-<slug>-v1.md content to the worktree
```

**Validation:** After writing, verify each of the 5 sections (Sistema, Escopo, Regras, Histórias, DoR/DoD) has ≥ 100 characters. If any section is too short, expand with domain-relevant placeholder content.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-ideate-feature Phase-2-Spec ok`

---

## Phase 3 — Commit + PR

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-ideate-feature Phase-3-PR`

**3.1 Commit the spec:**

From inside the worktree, commit the spec file:

    Skill(skill: "x-commit-changes", model: "haiku", args: "--worktree .claude/worktrees/feature-ideation-<slug> --type docs --subject \"add SPEC-<slug>-v1 ideation spec\" --body \"Generated by x-ideate-feature from prose ideation\"")

**3.2 Push and create PR:**

    Skill(skill: "x-create-pr", model: "haiku", args: "--head docs/feature-<slug> --target-branch develop --label docs --no-auto-merge --title \"docs: SPEC-<slug>-v1 ideation spec (review before x-create-feature)\" --body \"Spec generated by x-ideate-feature. Review and edit before invoking x-create-feature.\"")

Capture `{prUrl, prNumber}`.

**3.3 No CI-watch (RULE-003):** Ideation PRs do not require CI validation — they are planning artifacts, not code changes. Skip `x-watch-pr-ci`.

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-ideate-feature Phase-3-PR ok`

---

## Phase 4 — Report

<!-- TELEMETRY: phase.start -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-ideate-feature Phase-4-Report`

Print the following structured report to stdout:

```
====================================================
 x-ideate-feature — Spec ready
====================================================
 Slug:    <slug>
 Spec:    docs/specs/SPEC-<slug>-v1.md
 Branch:  docs/feature-<slug>
 PR:      #<prNumber> — <prUrl>
 Status:  Awaiting human review
----------------------------------------------------
 Next step:
   1. Review and edit the spec at <prUrl>
   2. Once satisfied, invoke:
      /x-create-feature docs/specs/SPEC-<slug>-v1.md --epic-id <NNNN>
====================================================
```

**Exact mandatory phrase (RULE-005):** "Spec pronta. Para criar a feature inteira, invoque `/x-create-feature <PR-spec-path> --epic-id <NNNN>`."

<!-- TELEMETRY: phase.end -->
Bash command: `$CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-ideate-feature Phase-4-Report ok`

---

## Error Handling

| Scenario | Action |
|----------|--------|
| Input prose < 100 chars | Exit 1 (`INPUT_TOO_SHORT`) with message |
| File path provided but file not found | Exit 2 (`OPERATIONAL_ERROR`) |
| Worktree creation fails | Exit 3 (`WORKTREE_FAILED`) — no cleanup needed |
| Commit fails | Preserve worktree for diagnosis; exit 2 |
| PR creation fails | Exit 2; print worktree path for manual push |
| RA9 section < 100 chars | Expand inline before commit |
| `gh` not authenticated | Exit 2 with `gh auth login` instruction |

## Integration Notes

| Skill | Relationship | Notes |
|-------|-------------|-------|
| `x-manage-worktrees` | delegates (P0) | Creates isolated `feature-ideation-<slug>` worktree |
| `x-commit-changes` | delegates (P3) | Commits spec with `docs:` Conventional Commits prefix |
| `x-create-pr` | delegates (P3) | Opens PR to `develop` with label `docs`, no auto-merge |
| `x-create-feature` | NOT called | Operator manually invokes after reviewing the spec PR |

## Backward Compatibility

This is a new skill (EPIC-0065, Rule 19 hard-cut). No predecessor names existed. No aliases required.
