# Specialist Review — story-0064-0601

**Story:** story-0064-0601 — audit-capability-graph.sh (Phase 6)
**Date:** 2026-04-29
**Branch:** epic/0064
**Reviewers:** QA, Performance, DevOps

## Consolidated Score

| Specialist  | Score  | Status   |
|-------------|--------|----------|
| QA          | 23/38  | Rejected |
| Performance | 8/10   | Partial  |
| DevOps      | 14/18  | Rejected |
| **Total**   | **45/66** | **REJECTED (68%)** |

## Critical/High Findings

| ID | Specialist | Severity | Item | Finding |
|----|-----------|---------|------|---------|
| QA-02 | QA | HIGH | Line coverage | 93.2% < 95% threshold (application.composition 72%, domain.capability 74%) |
| QA-03 | QA | HIGH | Branch coverage | 86.5% < 90% threshold |
| DEVOPS-04 | DevOps | HIGH | .dockerignore missing | Build context unguarded; planning artifacts included in image build |
| QA-13 | QA | MEDIUM | TDD commits | Tests bundled with implementation in single commits |
| QA-14 | QA | MEDIUM | No refactoring commits | No `refactor:` commits visible post-green |
| QA-18 | QA | MEDIUM | Coverage below threshold | Same as QA-02/03 — package-level gap |
| PERF-07 | Perf | MEDIUM | find without timeout | audit-capability-graph.sh find cmd could hang on NFS/deep symlinks |
| PERF-05 | Perf | LOW | No caching in composer | Acceptable for build-time but worth noting for scale |
| DEVOPS-06 | DevOps | LOW | Image not digest-pinned | Tag mutable; reproducibility risk |

## Individual Report Paths

- QA: `plans/epic-0064/reviews/review-qa-story-0064-0601.md`
- Performance: `plans/epic-0064/reviews/review-perf-story-0064-0601.md`
- DevOps: `plans/epic-0064/reviews/review-devops-story-0064-0601.md`

## Overall Verdict

**REJECTED** — HIGH findings QA-02/QA-03/DEVOPS-04 must be resolved before merge.

### Required Fixes Before Merge

1. **Coverage gap** — raise `application.composition` and `domain.capability` to ≥95%/90%
2. **.dockerignore** — create with at minimum: `.git`, `java/target/`, `plans/`, `ai/`, `.claude/`
3. **Bash timeout** — add `timeout 30` to `find` in audit-capability-graph.sh
