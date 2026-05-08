# Execution Plan — EPIC-0074 (Dependency Policy & SCA Final Gate)

**Generated:** 2026-05-01
**flowVersion:** 4
**Mode:** sequential (default)

## Resolved TBDs

| TBD | Palpite | Resolvido |
|-----|---------|-----------|
| Rule number (D-R3) | Rule 32 | **Rule 32** (next free after 31) |
| ADR number (D-R4) | ADR-0023 | **ADR-0027** (next free after ADR-0026) |
| Rule 27 surface (D-R7) | surface 13 | **surface 13** (next after 12) |

## Execution Plan

### Phase 0 — Governance + Schema + KP (sequential, 1 story)
- **story-0074-0001:** Schema YAML `dependencies.policy` + `DependencyPolicyConfig.java` + capabilities (6 YAML files) + Rule 32 + ADR-0027 + KP dependency-policy-playbook.md

### Phase 1 — Skill + Template (sequential, 2 stories)
- **story-0074-0002:** Skill `x-dep-policy-validate` + `_TEMPLATE-DEP-POLICY-REPORT.md` + `x-dependency-audit --policy` modification
- **story-0074-0003:** `_TEMPLATE-DEP-POLICY-DECLARATION.md` + DocsAssembler integration

### Phase 2 — CI + Phase 3 Modification (sequential, 2 stories)
- **story-0074-0004:** CI script `audit-dep-policy.sh` + catalog entry
- **story-0074-0005:** `x-story-implement` Phase 3 MANDATORY conditional + Rule 27 surface 13

### Phase 3 — Smoke + Release (sequential, 1 story)
- **story-0074-0006:** `Epic0074DepPolicySmokeIT` (6 scenarios) + CHANGELOG + CLAUDE.md

## Critical Path
```
0074-0001 → 0074-0002 → 0074-0005 → 0074-0006
```

## Key Artifacts Produced

| Story | Artifact |
|-------|---------|
| 0074-0001 | `capabilities/governance/dependency-policy.yaml` + 5 sub-families |
| 0074-0001 | `.claude/rules/32-dependency-policy-gate.md` (source: `src/main/resources/targets/claude/rules/`) |
| 0074-0001 | `docs/adr/ADR-0027-dependency-policy-gate.md` |
| 0074-0001 | `src/main/java/.../domain/model/DependencyPolicyConfig.java` |
| 0074-0001 | `src/main/resources/targets/claude/knowledge/security/dependency-policy-playbook.md` |
| 0074-0002 | `src/main/resources/targets/claude/skills/core/security/x-dep-policy-validate/SKILL.md` |
| 0074-0002 | `src/main/resources/shared/templates/_TEMPLATE-DEP-POLICY-REPORT.md` |
| 0074-0003 | `src/main/resources/shared/templates/_TEMPLATE-DEP-POLICY-DECLARATION.md` |
| 0074-0004 | `src/main/resources/targets/claude/scripts/audit-dep-policy.sh` |
| 0074-0005 | Modified `x-story-implement/SKILL.md` Phase 3 + Rule 27 surface 13 |
| 0074-0006 | `Epic0074DepPolicySmokeIT.java` + CHANGELOG + CLAUDE.md update |
