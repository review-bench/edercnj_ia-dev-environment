# Migration Guide: `x-feature-create` → `x-create-feature`

**Introduced by:** EPIC-0076 (Verb-First Skill Naming Refactor)  
**Coordinated with:** EPIC-0077 (story-0077-0003)  
**Type:** Rename only — behavior unchanged

---

## Summary

EPIC-0076 renamed `x-feature-create` to `x-create-feature` as part of the verb-first naming convention. All arguments and behavior are identical; only the invocation name changed.

---

## Before / After Comparison

### Invocation

| | Before | After |
| :--- | :--- | :--- |
| Chat command | `/x-feature-create spec.md --epic-id 0049` | `/x-create-feature spec.md --epic-id 0049` |
| Skill invocation | `Skill(skill: "x-feature-create", ...)` | `Skill(skill: "x-create-feature", ...)` |

### Arguments (unchanged)

```bash
# Before (EPIC-0065 era)
/x-feature-create [SPEC-FILE-PATH] --epic-id <NNNN> [--no-jira] [--dry-run]

# After (EPIC-0076+)
/x-create-feature [SPEC-FILE-PATH] --epic-id <NNNN> [--no-jira] [--dry-run]
```

All flags (`--epic-id`, `--no-jira`, `--dry-run`) are identical.

---

## What to Update

1. **Chat invocations**: Replace `/x-feature-create` with `/x-create-feature` in any runbooks, scripts, or documentation
2. **SKILL.md references**: Any SKILL.md that delegates to `x-feature-create` must be updated to `x-create-feature`
3. **CI scripts**: Search for `x-feature-create` and replace with `x-create-feature`

```bash
# Find and report residual references
grep -r "x-feature-create" .claude/skills/ src/main/resources/targets/claude/skills/ --include="*.md"
```

Expected: 0 results (the audit script `src/test/bash/audit-skill-references.sh` enforces this).

---

## Coordination Note (EPIC-0077)

EPIC-0077 (Product-First Lifecycle) required a distinct name for its planned "create Feature from Capability" skill. The verb-first rename of EPIC-0076 (using `x-create-feature` for the spec-based creation) clarified the naming space. EPIC-0077 will use a different name (e.g., `x-create-product-feature`) for its Feature-from-Capability concept, ensuring no naming collision.

See: `ai/epics/epic-0077-product-first-lifecycle/plans/coordination-record-0077-0003.md`
