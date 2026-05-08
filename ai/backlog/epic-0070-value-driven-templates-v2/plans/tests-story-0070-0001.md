# Test Plan — story-0070-0001

**Story:** Capability + Rule 30 + ADR-0023
**Scope:** STANDARD — governance artifacts, no Java production code

## Verification Approach

No new Java test class required (no production Java code added). Validation is structural:

1. `capabilities/governance/value-driven-templates.yaml` exists and fields match schema
2. `src/main/resources/targets/claude/rules/30-value-driven-templates.md` exists with required frontmatter + sections
3. `docs/adr/ADR-0023-value-driven-templates.md` exists with canonical ADR structure
4. `ai/epics/epic-0056-*/epic-0056.md` opens with SUPERSEDED block after H1

## AC Coverage

| AC Category | Scenario | Verification |
|-------------|----------|-------------|
| Happy | All artifacts created | `ls` + schema check |
| Degenerate | Capability without required fields | Schema validation (Epic0064SmokeIT passes) |
| Error | Rule number collision | Verified pre-creation — Rule 30 free |
| Boundary | D-R7 overlap < 30% → Rule separada | Documented in ADR-0023 |
| Performance | mvn verify ≤ 60s | Governance-only — no new test class |
| Security | Path traversal prevention | Capability ID format validated against schema pattern |

## Existing Test Suites (passive gate)
- `Epic0064SmokeIT` — capability schema validation (covers Degenerate scenario)
- `LifecycleIntegrityAuditTest` — rules lifecycle
- Frontmatter-3.0 schema validators — covers Rule MD
