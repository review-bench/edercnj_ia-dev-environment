# Implementation Plan — story-0076-0004

**Story:** Renomear skills públicas de suporte, review, teste, segurança, git, PR, Jira e operações  
**Epic:** EPIC-0076  
**Date:** 2026-05-03

## Scope

Rename support cluster public skills in source of truth using `git mv` (history preserved) and update `name:` frontmatter.

## Collision Resolution

Both `x-test-perf` and `x-test-performance` exist on disk and the SPEC v1.2 listed both mapping to `x-execute-performance-tests`. Inspection reveals they are distinct skills:

- `x-test-perf`: Simple general performance test (baseline/normal/peak/sustained scenarios)
- `x-test-performance`: Stack-aware EPIC-0072 skill (dispatches Newman/ghz/hyperfine/Artillery)

Resolution:
- `x-test-performance` → `x-execute-performance-tests` (canonical — the more comprehensive EPIC-0072 skill)
- `x-test-perf` → `x-run-perf-tests` (differentiated — still verb-first, shorter name reflects simpler scope)

## Renames Applied

### 6.2 Testes (12 skills)

| Old name | New name | Source path |
|---|---|---|
| x-test-plan | x-plan-tests | core/test |
| x-test-run | x-execute-tests | core/test |
| x-test-tdd | x-drive-tdd | core/test |
| x-test-e2e | x-execute-e2e-tests | conditional/test |
| x-test-contract | x-execute-contract-tests | conditional/test |
| x-test-contract-lint | x-lint-contract-tests | conditional/test |
| x-test-smoke-api | x-execute-api-smoke-tests | conditional/test |
| x-test-smoke-socket | x-execute-socket-smoke-tests | conditional/test |
| x-test-perf | x-run-perf-tests | conditional/test |
| x-test-performance | x-execute-performance-tests | conditional/test |
| x-test-mutation | x-execute-mutation-tests | conditional/test |
| x-test-regression-shell | x-execute-shell-regression-tests | conditional/test |

### 6.3 Review (5 renames, 11 unchanged)

| Old name | New name | Source path |
|---|---|---|
| x-review | x-review-codebase | core/review |
| x-review-perf | x-review-performance | core/review |
| x-review-db | x-review-database | conditional/review |
| x-review-obs | x-review-observability | conditional/review |
| x-code-audit | x-audit-code | core/review |

*Unchanged (already acceptable): x-review-pr, x-review-qa, x-review-api, x-review-devops, x-review-events, x-review-gateway, x-review-graphql, x-review-grpc, x-review-security, x-review-compliance, x-review-data-modeling*

### 6.4 Code, docs, templates (8 renames, 2 unchanged)

| Old name | New name | Source path |
|---|---|---|
| x-code-format | x-format-code | core/code |
| x-code-lint | x-lint-code | core/code |
| x-doc-generate | x-generate-docs | core/ops |
| x-doc-validate | x-validate-docs | core/ops |
| x-template-migrate | x-migrate-templates | core/plan |
| x-frontmatter-migrate | x-migrate-frontmatter | core/internal/plan |
| x-ci-generate | x-generate-ci | core/dev |
| x-mcp-recommend | x-recommend-mcp | core/dev |

*Unchanged (already verb-first): x-setup-env (core/dev), x-setup-stack (conditional/dev)*

### 6.5 Git, PR e worktree (13 renames)

| Old name | New name | Source path |
|---|---|---|
| x-git-branch | x-create-git-branch | core/git |
| x-git-cleanup-branches | x-cleanup-git-branches | core/git |
| x-git-commit | x-commit-changes | core/git |
| x-git-merge | x-merge-branches | core/git |
| x-git-push | x-push-branch | core/git |
| x-git-worktree | x-manage-worktrees | core/git |
| x-planning-commit | x-commit-planning | core/git |
| x-pr-create | x-create-pr | core/pr |
| x-pr-fix | x-fix-pr | core/pr |
| x-pr-fix-epic | x-fix-epic-pr | core/pr |
| x-pr-merge | x-merge-pr | core/pr |
| x-pr-merge-train | x-manage-pr-merge-train | core/pr |
| x-pr-watch-ci | x-watch-pr-ci | core/pr |

### 6.6 Operações, release e telemetria (9 renames, 1 unchanged)

| Old name | New name | Source path |
|---|---|---|
| x-ops-incident | x-handle-incident | core/ops |
| x-ops-troubleshoot | x-troubleshoot-operations | core/ops |
| x-perf-profile | x-profile-performance | core/ops |
| x-release-changelog | x-generate-release-changelog | core/ops |
| x-status-reconcile | x-reconcile-status | core/ops |
| x-telemetry-analyze | x-analyze-telemetry | core/ops |
| x-telemetry-trend | x-analyze-telemetry-trends | core/ops |
| x-obs-instrument | x-instrument-observability | conditional/ops |
| x-memory-search | x-search-memory | core/ops |

*Unchanged (already acceptable): x-release (core/ops)*

### 6.7 Segurança (16 renames)

| Old name | New name | Source path |
|---|---|---|
| x-dependency-audit | x-audit-dependencies | core/security |
| x-supply-chain-audit | x-audit-supply-chain | core/security |
| x-hardening-eval | x-evaluate-hardening | core/security |
| x-runtime-eval | x-evaluate-runtime | core/security |
| x-owasp-scan | x-scan-owasp | core/security |
| x-security-dashboard | x-generate-security-dashboard | core/security |
| x-security-pipeline | x-generate-security-pipeline | core/security |
| x-security-secrets | x-scan-secrets | conditional/security |
| x-security-sast | x-run-sast | conditional/security |
| x-security-dast | x-run-dast | conditional/security |
| x-security-container | x-scan-container-security | conditional/security |
| x-pentest-dynamic | x-run-dynamic-pentest | core/security |
| x-security-pentest | x-run-pentest | conditional/security |
| x-security-infra | x-assess-infrastructure-security | conditional/security |
| x-security-sonar | x-run-sonar-security | conditional/security |
| x-dep-policy-validate | x-validate-dependency-policy | conditional/security |

### 6.8 Jira (2 renames)

| Old name | New name | Source path |
|---|---|---|
| x-jira-create-epic | x-create-jira-epic | core/jira |
| x-jira-create-stories | x-create-jira-stories | core/jira |

## Summary

**Total renames: 65 skills**

## Notes

- Body references (Skill(...) calls) NOT updated here — handled atomically in story-0076-0006
- `x-review-pr`, `x-review-qa` and other already-acceptable names remain unchanged
- `x-release` remains unchanged (verb-noun form already acceptable per SPEC §6.6)
- git mv used for all renames (history preserved)
- Collision between x-test-perf and x-test-performance resolved: both get distinct names

## DoD Check

- [ ] 65/65 support cluster skills renamed via git mv
- [ ] name: frontmatter updated in all 65 SKILL.md files
- [ ] Git status shows rename (RM) for all SKILL.md files
