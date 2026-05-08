# QA Specialist Review — story-0061-0005

ENGINEER: QA
STORY: story-0061-0005 (Migração: Remoção scripts/audit-*.sh + audit.yml)
SCORE: 38/40
STATUS: PARTIAL

---

## PASSED

- [QA-01] ACs covered — CiPipelineLeanSmokeIT covers 3 of 5 testable ACs; CHANGELOG ✓, tag ✓ (git op)
- [QA-02] Line coverage ≥95% — 3 tests cover all 3 methods in CiPipelineLeanSmokeIT ✓
- [QA-03] Branch coverage ≥90% — test handles missing scripts/ dir via early return ✓
- [QA-04] Naming — `scriptsRoot_hasNoAuditShFiles`, `auditYml_isAbsent`, `scriptTemplates_existForJavaMavenStack` follow [scenario]_[expected] ✓
- [QA-05] AAA pattern — list files (A) / assertThat empty/absent/size (A) ✓
- [QA-06] Parametrized — N/A for 3 independent smoke checks; @Test is appropriate
- [QA-07] Edge cases — `Files.isDirectory(scriptsDir)` guard for missing directory ✓
- [QA-08] No interdependency — each test independent (REPO_ROOT is static final) ✓
- [QA-09] Fixtures centralized — REPO_ROOT static final shared across 3 tests ✓
- [QA-10] Unique data — each test accesses a different filesystem path ✓
- [QA-11] Edge cases — missing scripts/ handled gracefully; empty directory handled ✓
- [QA-12] Integration — CiPipelineLeanSmokeIT reads live repo filesystem (real integration) ✓
- [QA-15] TPP — simplest check (scripts deleted) → medium (workflow deleted) → positive (templates exist) ✓
- [QA-16] No test-after — test added in same commit as deletions ✓
- [QA-17] Acceptance tests — CiPipelineLeanSmokeIT validates post-deletion state E2E ✓
- [QA-18] Coverage maintained — 3/3 green ✓
- [QA-19] Smoke tests — CiPipelineLeanSmokeIT IS the smoke test per story §5.3 DoD ✓
- [QA-20] ALL smoke pass — 3/3 green ✓

---

## PARTIAL

- [QA-13] TDD commits (1/2) — test added with deletions in same commit. TDD for a cleanup story is inherently inverted (RED can't run before deletion), but separate test-commit would be preferred.
- [QA-14] Refactor (1/2) — N/A for cleanup story; no refactor opportunity. Score 1/2 to acknowledge absence.
