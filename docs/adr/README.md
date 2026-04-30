# Architecture Decision Records

> Architecture Decision Records for **ia-dev-environment**.

| ID | Title | Status | Date |
|----|-------|--------|------|
| ADR-0001 | [Intentional Architectural Deviations for CLI Tool](ADR-0001-intentional-architectural-deviations-for-cli-tool.md) | Accepted | 2026-03-20 |
| ADR-0002 | [Skill Delegation Protocol (Rule 13)](ADR-0002-skill-delegation-protocol.md) | Accepted | 2026-04-10 |
| ADR-0003 | [Skill Taxonomy and Naming Refactor](ADR-0003-skill-taxonomy-and-naming.md) | Proposed | 2026-04-10 |
| ADR-0004 | [Worktree-First Branch Creation Policy](ADR-0004-worktree-first-branch-creation-policy.md) | Accepted | 2026-04-13 |
| ADR-0005 | [Telemetry Architecture for Skill Execution Visibility (EPIC-0040)](ADR-0005-telemetry-architecture.md) | Accepted | 2026-04-17 |
| ADR-0006 | [File-Conflict-Aware Parallelism Analysis (EPIC-0041)](ADR-0006-file-conflict-aware-parallelism.md) | Accepted | 2026-04-19 |
| ADR-0007 | [ConsoleProgressReporter stdout/stderr Contract](ADR-0007-console-progress-reporter-stdout-contract.md) | Accepted | 2026-04-20 |
| ADR-0008 | [Hybrid Hex-Core + Flat-CLI Package Layout](ADR-0008-hybrid-hex-core-and-flat-cli-layout.md) | Accepted | 2026-04-21 |
| ADR-0009 | [Wide Records Bound to External Schemas (Rule 03 Exception)](ADR-0009-wide-records-bound-to-external-schemas.md) | Accepted | 2026-04-21 |
| ADR-0010 | [Interactive Gates Convention (Rule 20)](ADR-0010-interactive-gates-convention.md) | Accepted | 2026-04-22 |
| ADR-0011 | [Shared Snippets — Inclusion Strategy for `_shared/`](ADR-0011-shared-snippets-inclusion-strategy.md) | Accepted | 2026-04-23 |
| ADR-0012 | [Skill Body Slim-by-Default (Flipped Orientation)](ADR-0012-skill-body-slim-by-default.md) | Accepted | 2026-04-24 |
| ADR-0013 | [Knowledge Packs live under `.claude/knowledge/`](ADR-0013-knowledge-packs-dedicated-directory.md) | Accepted | 2026-04-25 |
| ADR-0014 | [Task Hierarchy & Phase Gate Enforcement (EPIC-0055)](ADR-0014-task-hierarchy-and-phase-gates.md) | Accepted | 2026-04-26 |
| ADR-0015 | [Audit Gate Lifecycle Convention (EPIC-0058)](ADR-0015-audit-gate-lifecycle.md) | Accepted | 2026-04-26 |
| ADR-0016 | [Capability-Driven Composition (EPIC-0064)](ADR-0016-capability-driven-composition.md) | Accepted | 2026-04-28 |
| ADR-0017 | [Local-First Lifecycle Convention (EPIC-0061)](ADR-0017-local-first-lifecycle.md) | Accepted | 2026-04-28 |
| ADR-0018 | [Zero-Bypass Amnesty for EPIC-0054 through EPIC-0057](ADR-0018-zero-bypass-amnesty.md) | Accepted | 2026-04-27 |
| ADR-0019 | [Preflight Gates WARN→FAIL Rollout Strategy](ADR-0019-preflight-warn-to-fail-rollout.md) | Accepted | 2026-04-28 |
| ADR-0020 | [Hexagonal Architecture Migration (EPIC-0015)](ADR-0020-hexagonal-architecture-migration.md) | Accepted | 2026-04-04 |
| ADR-0021 | [`CLAUDE.md` Contract (Root File via Dedicated Assembler) (EPIC-0048)](ADR-0021-claude-md-contract.md) | Accepted | 2026-04-22 |
| ADR-0022 | [Refinement Gate Convention (EPIC-0069)](ADR-0022-refinement-gate.md) | Accepted | 2026-04-30 |
| ADR-0048 | [Java-Only Scope for the ia-dev-env Generator (EPIC-0048)](ADR-0048-java-only-scope.md) | Accepted | 2026-04-22 |

> **Note (2026-04-29):** ADRs 0018–0021 are renumbered duplicates from a prior numbering collision (originally 0015-zero-bypass, 0016-preflight, 001-hexagonal, 0048-B). The canonical ADRs at 0015, 0016, and 0048 retain their original numbers.

## Creating a New ADR

Copy `_TEMPLATE-ADR.md` and follow the naming convention:
`ADR-NNNN-title-in-kebab-case.md`
