# Specialist Review — story-0071-0006

**Story:** Phase 3 of `x-story-implement` MODIFIED — MANDATORY `x-doc-generate` + `x-doc-validate`  
**Epic:** EPIC-0071 (Documentation as DoD)  
**Reviewed at:** 2026-05-01  
**Reviewer:** QA + Security Specialist

## Checklist

- [x] New step 3.0 added at start of Phase 3 — before verify gate (correct ordering)
- [x] `x-doc-generate` declared as MANDATORY TOOL CALL with `[required]` marker (Rule 28 grammar)
- [x] `x-doc-validate` declared as MANDATORY TOOL CALL with `[required]` marker (Rule 28 grammar)
- [x] `--skip-doc` added to Recovery table only — not to happy-path parameters table
- [x] `DOC_VALIDATION_FAILED` added to Error Envelope
- [x] Retry policy documented: 1 retry after 30s for transient failures
- [x] Rule 24 §Mandatory Evidence Artifacts updated with `x-doc-validate` entry
- [x] `audit-bypass-flags.sh` templates extended to detect `--skip-doc` outside Recovery (7 templates updated)
- [x] `verify-story-completion.sh` Stop hook extended to check `doc-validate-report-STORY-ID.md`
- [x] Phase 3 `--expected-artifacts` updated to include `doc-validate-report-STORY-ID.md`
- [x] Sub-task trackers updated from 6 to 8 (docGenerate + docValidate added)
- [x] Modification is cirúrgica — no Phase 4 created; existing 4-phase structure preserved

## Score: 97/100

Excellent implementation. The "## Recovery" section comment inside Phase 3 is slightly awkward (see `## Recovery` line between doc-generate and doc-validate paragraphs) — minor formatting issue, non-blocking.

## Verdict

**GO** — Phase 3 correctly enforces documentation gate as MANDATORY. All Rule 24 evidence chains updated.
