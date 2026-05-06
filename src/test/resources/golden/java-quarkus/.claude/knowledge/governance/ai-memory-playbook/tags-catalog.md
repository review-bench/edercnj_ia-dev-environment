---
name: ai-memory-tags-catalog
description: Canonical tag taxonomy for ai/memory/ epic summaries
requires-capabilities: [governance.ai-memory]
---

# Tags Catalog — AI Memory Summaries

Canonical tag taxonomy for `ai/memory/` epic summaries.
Used by `x-memory-search --by-tag` and `x-internal-epic-summary` validation.

## Process to Add a New Tag

1. Open a PR adding the entry below with `id`, `description`, and `introduced-by`.
2. PR requires human tech-lead review (not auto-mergeable).
3. Update `x-memory-search --help` to include the new tag in the documented list.

Format: `id` must be kebab-case. `description`: 1 sentence max.

## Catalog

```yaml
schemaVersion: "1.0"
tags:
  - id: governance
    description: Epic governs process, lifecycle, or policy rules.
    introduced-by: EPIC-0075

  - id: refinement
    description: Epic introduces or changes the story/epic refinement gate.
    introduced-by: EPIC-0075

  - id: dor-gate
    description: Epic adds or modifies a Definition-of-Ready enforcement mechanism.
    introduced-by: EPIC-0075

  - id: templates
    description: Epic adds or changes planning/documentation templates.
    introduced-by: EPIC-0075

  - id: templates-v2
    description: Epic migrates templates to value-driven v2 format (EPIC-0070+).
    introduced-by: EPIC-0075

  - id: documentation
    description: Epic improves or enforces documentation freshness.
    introduced-by: EPIC-0075

  - id: testing
    description: Epic adds or changes automated test strategy or tooling.
    introduced-by: EPIC-0075

  - id: performance
    description: Epic adds performance testing, budgets, or SLAs.
    introduced-by: EPIC-0075

  - id: mutation
    description: Epic adds mutation testing gates or tooling.
    introduced-by: EPIC-0075

  - id: contract
    description: Epic adds API or event contract testing.
    introduced-by: EPIC-0075

  - id: security
    description: Epic adds security scanning, policy, or hardening.
    introduced-by: EPIC-0075

  - id: dependency
    description: Epic adds dependency management policy or SCA gates.
    introduced-by: EPIC-0075

  - id: release
    description: Epic changes the release process, versioning, or changelog.
    introduced-by: EPIC-0075

  - id: ci-cd
    description: Epic modifies CI/CD workflows or pipeline structure.
    introduced-by: EPIC-0075

  - id: telemetry
    description: Epic adds or extends execution telemetry capture.
    introduced-by: EPIC-0075

  - id: observability
    description: Epic adds observability tooling (metrics, traces, logs) to generated projects.
    introduced-by: EPIC-0075

  - id: architecture
    description: Epic introduces an architectural pattern or constraint.
    introduced-by: EPIC-0075

  - id: refactor
    description: Epic is primarily a refactor with no new user-visible features.
    introduced-by: EPIC-0075

  - id: branding
    description: Epic changes naming, branding, or project identity conventions.
    introduced-by: EPIC-0075

  - id: naming
    description: Epic standardizes naming conventions (skills, files, or packages).
    introduced-by: EPIC-0075

  - id: memory
    description: Epic relates to the AI memory layer (EPIC-0075+).
    introduced-by: EPIC-0075

  - id: retrieval
    description: Epic adds search or retrieval capabilities over project knowledge.
    introduced-by: EPIC-0075

  - id: phase-gate
    description: Epic adds or enforces lifecycle phase gates.
    introduced-by: EPIC-0075

  - id: parallelism
    description: Epic addresses parallel execution, file-conflict analysis, or worktrees.
    introduced-by: EPIC-0075

  - id: worktree
    description: Epic involves git worktree lifecycle changes.
    introduced-by: EPIC-0075

  - id: audit
    description: Epic adds audit scripts, enforcement layers, or governance baselines.
    introduced-by: EPIC-0075

  - id: rule-lifecycle
    description: Epic introduces, supersedes, or archives a rule.
    introduced-by: EPIC-0075

  - id: capability
    description: Epic adds or changes capability-driven composition (Rule 28).
    introduced-by: EPIC-0075

  - id: compose
    description: Epic changes how .claude/ artifacts are assembled or composed.
    introduced-by: EPIC-0075

  - id: assembler
    description: Epic changes an Assembler class in the application layer.
    introduced-by: EPIC-0075

  - id: local-first
    description: Epic relates to local-first lifecycle or preventive hooks (EPIC-0061+).
    introduced-by: EPIC-0075
```
