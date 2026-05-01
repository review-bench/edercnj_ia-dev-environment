# Specialist Review — story-0071-0003

**Story:** `x-doc-generate` v2 (target-stack-aware + integração com `x-arch-system-update`)
**Epic:** EPIC-0071
**Reviewed at:** 2026-05-01
**PR:** #893

## QA Review

- AC coverage: 6 Gherkin scenarios — all 4 mandatory categories present ✓
- `--target-stack-aware` default mode documented with stack-detection table ✓
- `--legacy-v1` deprecation warning documented + 2-release window per Rule 19 ✓
- FLAG_CONFLICT on `--legacy-v1` + `--target-stack-aware` → exit non-zero + stderr message ✓
- Degenerate: project without documentation.targets → auto-detect defaults applied ✓
- `--dry-run` flag documented (read-only mode) ✓

## Security Review

- PATH_TRAVERSAL_REJECTED on `..` or absolute paths in `documentation.targets` ✓
- Symlink follow explicitly forbidden ✓
- Security AC scenario: `../../etc/passwd` in targets → rejected ✓
- No credential or CI token exposure in report output ✓

## Architecture Review

- Rule 13 Pattern 1 (INLINE-SKILL) used for `x-arch-system-update` ✓
- Rule 28 grammar markers: `[required]` on `x-release-changelog`, `[optional]` on `x-adr-generate`, `[conditional: flag.arch_change_detected]` on `x-arch-system-update` ✓
- EPIC-0070 absence handled: WARN + skip (not blocking) ✓
- `requires-capabilities: [governance.doc-as-dod]` frontmatter v3.0 ✓
- `model: sonnet` (Orchestrator tier, Rule 23) ✓
- v1 fallback code path documented under `## v1 Fallback` section ✓
- Integration table updated: `x-doc-validate` as `followed-by` ✓

## Verdict

**GO** — Stack-aware generation, arch detection, and graceful EPIC-0070 fallback all documented correctly.
