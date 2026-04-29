# Review Content Calibration Corpus

**Purpose:** Provide ground-truth examples for audit-review-content.sh heuristic validation and manual audit baseline calibration.

**Generated:** 2026-04-28 (TASK-0063-0002-003)

## Stub Reviews (Intentionally Invalid)

### stub-epic-0062-0001.md
- **Violations:** H1_LINES (2 lines < 50), H2_SECTIONS (0 sections < 3), H3_FILE_REFS (0 refs < 2)
- **Decision Marker:** present ✓ (GO)
- **Rationale:** Minimal stub review; exactly the kind pre-EPIC-0063 audits missed
- **Expected audit-review-content.sh result:** exit 1 (REVIEW_CONTENT_INSUFFICIENT)

### stub-epic-0062-0008.md
- **Violations:** H1_LINES (3 lines < 50), H2_SECTIONS (0 sections < 3), H3_FILE_REFS (0 refs < 2)
- **Decision Marker:** present ✓ (NO-GO)
- **Rationale:** Slightly longer stub, but still lacks required structure
- **Expected audit-review-content.sh result:** exit 1

### stub-epic-0062-0015.md
- **Violations:** H1_LINES (2 lines < 50), H2_SECTIONS (0 sections < 3), H3_FILE_REFS (0 refs < 2), H4_DECISION (missing)
- **Decision Marker:** absent ✗
- **Rationale:** Stub without decision marker; triggers H4 violation
- **Expected audit-review-content.sh result:** exit 1

### stub-epic-0062-0021.md
- **Violations:** H1_LINES (2 lines < 50), H2_SECTIONS (1 section < 3), H3_FILE_REFS (0 refs < 2)
- **Decision Marker:** absent ✗
- **Rationale:** One section but not H2/H3 (## not used), no decision
- **Expected audit-review-content.sh result:** exit 1

## Legitimate Reviews (Valid)

### review-epic-0062-0007.md
- **Heuristics:** 
  * H1: 67 lines ✓ (>= 50)
  * H2: 4 sections (## Summary, ## Technical Assessment, ## Dependency Analysis, ## Recommendations, ## Compliance, ## Decision) ✓ (>= 3)
  * H3: 3 file refs (src/main/java/dev/iadev/adapter/inbound/cli/*.java, src/main/java/dev/iadev/domain/parallelism/*.java) ✓ (>= 2)
  * H4: Decision marker (GO) ✓
- **Expected result:** exit 0 (AUDIT_OK)
- **Rationale:** Comprehensive architecture review with all heuristics met

### review-epic-0062-0012.md
- **Heuristics:**
  * H1: 82 lines ✓
  * H2: 5 sections (## Overview, ## Code Inspection, ## Test Coverage Assessment, ## Compliance Checklist, ## Recommendations, ## Final Verdict) ✓
  * H3: 4 file refs (src/main/java/dev/iadev/adapter/inbound/cli/GenerateCommand.java, src/main/java/dev/iadev/adapter/inbound/cli/ValidateCommand.java, src/main/java/dev/iadev/domain/..., tests) ✓
  * H4: Decision marker (GO-WITH-RESERVATIONS) ✓
- **Expected result:** exit 0
- **Rationale:** Specialist code review with detailed file-level analysis

### review-epic-0062-0019.md
- **Heuristics:**
  * H1: 89 lines ✓
  * H2: 7 sections (## Overview, ## Architecture Evaluation, ## Implementation Review, ## Code Quality, ## Test Coverage, ## Compliance, ## Risk Assessment, ## Recommendations, ## Acceptance) ✓
  * H3: 5+ file refs (java/src/main/resources/targets/claude/hooks/verify-*.sh, enforce-*.sh, src/main/java/...) ✓
  * H4: Decision marker (GO) ✓
- **Expected result:** exit 0
- **Rationale:** Tech-lead review covering architecture, risk, and compliance

## Usage

### Manual Audit Baseline
These examples are provided as reference for human review of audit-review-content.sh behavior. Run:

```bash
.claude/scripts/audit-review-content.sh --review-file governance/calibration/review-content/stubs/stub-epic-0062-0001.md
# Expected: exit 1, REVIEW_CONTENT_INSUFFICIENT

.claude/scripts/audit-review-content.sh --review-file governance/calibration/review-content/legitimate/review-epic-0062-0007.md
# Expected: exit 0, AUDIT_OK
```

### Regression Testing
When modifying audit-review-content.sh heuristics, run against this corpus to ensure no regressions:

```bash
bash src/test/shell/audit_review_content_test.sh
# All 8 tests should pass
```

## Maintenance

This corpus is static and immutable after TASK-0063-0002-003 merges. Future stubs discovered in EPIC-0062 reviews will be added to the baseline (`governance/baselines/review-content-baseline.txt`) rather than this calibration corpus.
