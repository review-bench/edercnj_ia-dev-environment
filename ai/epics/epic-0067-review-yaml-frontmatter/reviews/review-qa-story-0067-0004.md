ENGINEER: QA
STORY: story-0067-0004
SCORE: 30/36
STATUS: Partial
---
PASSED:
- [QA-1] Test naming convention follows [subject]_[scenario]_[expected] pattern (2/2)
- [QA-2] @DisplayName used consistently on all test classes and methods (2/2)
- [QA-3] No weak assertions — all verify specific exit codes + stderr content (2/2)
- [QA-4] Nested class organization (@Nested per scenario) — file 324 lines, structured (2/2)
- [QA-5] @BeforeEach isolation — each test gets fresh TempDir + git-init + schema copy (2/2)
- [QA-6] Happy path specialist review tested (exit 0) (2/2)
- [QA-7] Happy path tech-lead review tested (exit 0) (2/2)
- [QA-8] Missing frontmatter violation tested (exit 1 + REVIEW_FRONTMATTER_VIOLATION) (2/2)
- [QA-9] Invalid decision enum tested (exit 1 + REVIEW_FRONTMATTER_VIOLATION + "decision") (2/2)
- [QA-10] Missing required field tested (exit 1 + severity-counts message) (2/2)
- [QA-11] Baseline grandfather tested (exit 0 without frontmatter) (2/2)
- [QA-12] Self-check tested (exit 0 + SELF_CHECK_OK, or exit 2 + OPERATIONAL_ERROR) (2/2)
- [QA-13] Path resolution v3+v4 tested in --all mode (2/2)
- [QA-14] No duplicate fixtures — validSpecialistFrontmatter/validTechLeadFrontmatter helpers shared (2/2)
- [QA-15] Cross-file consistency — AuditReviewFrontmatterTest and Epic0067ReviewFrontmatterSmokeTest follow same ProcessResult record and run() pattern (2/2)

PARTIAL:
- [QA-16] INVALID_EXEMPTION exit-3 path not tested (0/2) — audit-exempt marker with missing reason produces exit 3; neither test class exercises this branch — file:AuditReviewFrontmatterTest.java — Improvement: add @Test void auditScript_auditExemptMissingReason_exitsThree() writing a review file with <!-- audit-exempt: --> (no reason text) and asserting exitCode == 3 + stderr contains "INVALID_EXEMPTION" [MEDIUM]
- [QA-17] audit-exempt happy path not tested (1/2) — the path where a valid <!-- audit-exempt: legacy review --> marker skips validation is unexercised — file:AuditReviewFrontmatterTest.java — Improvement: add test with valid audit-exempt marker asserting exit 0 [LOW]
- [QA-18] OPERATIONAL_ERROR for unknown flag not tested (1/2) — run(List.of("--bad-flag")) should exit 2; not covered — file:AuditReviewFrontmatterTest.java — Improvement: add negative-arg test [LOW]
