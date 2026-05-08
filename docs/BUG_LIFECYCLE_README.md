# Bug Lifecycle Management

Complete bug reporting, tracking, and resolution workflow for the ia-dev-environment project.

## Overview

Bug Lifecycle Management provides a structured approach to creating, refining, investigating, and resolving software bugs. Every bug follows a standardized template with 9 RA9 sections, enabling consistent documentation and cross-team collaboration.

## Quick Start

### Create a Bug Report

```bash
/x-create-bug "Login endpoint returns 500 on invalid credentials"
```

Returns:

```json
{
  "bugId": "bug-0001",
  "slug": "login-endpoint-returns-500-on-invalid-credentials",
  "bugFile": "docs/bugs/bug-0001-login-endpoint-returns-500-on-invalid-credentials.md",
  "severity": "HIGH",
  "scope": "STANDARD",
  "status": "Pendente",
  "created": "2026-05-07T14:23:15Z"
}
```

### Refine a Bug Report

```bash
/x-refine-bug docs/bugs/bug-0001-login-endpoint-returns-500-on-invalid-credentials.md
```

### Bug Status Workflow

```
Pendente (initial)
  ↓ [refinement approved]
Refinada (ready for investigation)
  ↓ [investigation starts]
Em Investigação (root-cause analysis in progress)
  ↓ [fix assigned]
Em Correção (fix implementation in progress)
  ↓ [tests pass]
Concluída (resolved and deployed)

Optional transitions:
→ Bloqueada (blocked by external dependency)
→ Descartada (duplicate, wontfix, or superseded)
→ Falha (fix validation failed)
```

## Bug Template Structure (9 RA9 Sections)

Every bug report contains:

### 1. Visão (Vision)
- User story format: "As a [persona], I experience [observed], when [trigger], so that [desired]"
- Impact statement describing business or user consequences

### 2. Persona & Cenário
- Target user or system role experiencing the bug
- Complete trigger scenario with context

### 3. Entrega de Valor (Value Delivery)
- Primary value being restored
- Success metrics
- Business impact assessment

### 4. Critérios de Aceite (Acceptance Criteria)
- AC-1: Fix verified (bug doesn't reproduce)
- AC-2: Regression guard (no existing tests broken)

### 5. Reproduction Recipe
- Environment details (version, OS, Java)
- Step-by-step reproduction
- Observed vs. expected behavior
- Artifacts (logs, stack traces, screenshots)

### 6. Root-Cause Hypothesis
- Hypothesis statement
- Affected components with file/class references
- Classification (Logic, Data, Integration, Concurrency, Performance, Security)

### 7. Regression Test Slot
- Test class and method reference
- Failing command (before fix)
- Passing command (after fix)

### 8. Dependências (Dependencies)
- Blocked by: prerequisite bugs/stories
- Blocks: dependent features/bugs

### 9. Histórico de Decisão (Decision History)
- Significant decisions made during investigation and fix

## Severity Levels

| Level | Definition | Response |
|-------|-----------|----------|
| LOW | Cosmetic or minor convenience issue | Review in next sprint |
| MEDIUM | Feature not fully functional | Investigate within sprint |
| HIGH | Multiple users affected, workaround exists | Prioritize within 24h |
| CRITICAL | System unavailable, no workaround | Drop everything, fix now |

## Scope Classification

| Scope | Complexity | Effort |
|-------|-----------|--------|
| SIMPLE | Single component, < 25 lines change | < 1 hour |
| STANDARD | Multiple components, 25-100 lines | 1-4 hours |
| COMPLEX | Major architectural, > 100 lines | > 4 hours |

## Files and Artifacts

| Artifact | Location | Purpose |
|----------|----------|---------|
| Template | `src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md` | Standard bug report structure |
| Capability | `config/capabilities/bug-lifecycle.yaml` | Feature declaration and metadata |
| Skill | `.claude/skills/dev/x-create-bug/SKILL.md` | CLI command for bug creation |
| Bugs | `docs/bugs/bug-NNNN-slug.md` | Actual bug reports |

## Example Workflow

### Step 1: Create Bug

```bash
/x-create-bug "Session token leaks to browser console on login" --severity CRITICAL --scope COMPLEX
```

Generated file: `docs/bugs/bug-0001-session-token-leaks-to-browser-console-on-login.md`

### Step 2: Fill Reproduction Recipe

Edit the bug file and complete Section 5 with:
- Environment: Version, OS, Java version
- Steps: Exact reproduction sequence
- Observed: What actually happens
- Expected: What should happen

### Step 3: Refine Bug

```bash
/x-refine-bug docs/bugs/bug-0001-session-token-leaks-to-browser-console-on-login.md
```

Provides:
- Spell-check and clarity improvements
- Validation of all required sections
- Root-cause hypothesis review

### Step 4: Investigate

Team investigates using the provided reproduction recipe:
- Execute steps in Section 5
- Analyze logs and stack traces
- Form hypothesis in Section 6
- Identify affected components

### Step 5: Create Regression Test

Add a failing unit/integration test that reproduces the bug:

```java
@Test
void testSessionTokenNotVisibleInConsole() {
    // Test that session token does not leak to browser
    // This test should FAIL before the fix
    // This test should PASS after the fix
}
```

Reference the test in Section 7 (Regression Test Slot).

### Step 6: Implement Fix

Write code to resolve the root cause while keeping regression test in place.

### Step 7: Verify

- Run regression test: should PASS
- Run full test suite: no regressions
- Update Section 6 with confirmed root cause
- Mark status as `Em Correção` → `Concluída`

## Telemetry and KPIs

### Event Tracking

- `phase.start`: Bug refinement begins
- `phase.end`: Bug refinement completes
- `tool.call`: Skill invocation (x-create-bug)
- `session.start`: Bug lifecycle session opens
- `session.end`: Bug lifecycle session closes

### Key Performance Indicators

- **P75 Resolution Time:** ≤ 8 minutes (Pendente → Concluída)
- **Regression Rate:** ≤ 2% (bugs marked Concluída that recur)
- **Refinement Accuracy:** ≥ 95% (refined bugs match investigation findings)

## Capability Declaration

The Bug Lifecycle Management capability is declared in:

```yaml
---
requires-capabilities: [governance.bug-lifecycle]
capability-version: "1.0"
---
```

This ensures:
- Proper audit trail and compliance
- Capability-driven composition per Rule 28
- Dependency tracking across epics/stories

## Validation and Constraints

### Description Validation
- Minimum: 8 characters
- Maximum: 200 characters

### Slug Generation
- NFKD normalization
- Lowercase conversion
- Special character stripping
- Hyphen collapsing
- Maximum 40 characters

### Status Transitions
- Linear progression (Pendente → Refinada → Em Investigação → Em Correção → Concluída)
- Optional lateral moves (→ Bloqueada, → Descartada, → Falha)
- Status changes require explicit update (not automatic)

### Regression Testing
- Test must be present before bug is marked Concluída
- Test must fail before the fix (RED phase)
- Test must pass after the fix (GREEN phase)

## Integration with Project Workflows

### Within x-implement-story
- Bug lifecycle can be invoked for test bugs
- Capability enables acceptance criteria validation

### Within CI/CD
- Bug files are committed to version control
- Regression tests become permanent test suite members
- Status transitions trigger notifications

### Documentation
- Bug reports serve as incident records
- Decision history facilitates knowledge transfer
- Root-cause analysis prevents similar bugs

## Security Considerations

- **PII Handling:** Do not include PII (email, passwords, phone numbers) in bug descriptions
- **Secrets:** Do not include credentials, tokens, or API keys in reproduction steps
- **SQL Injection:** Reproduction recipes must use parameterized queries, never concatenate SQL
- **Command Injection:** Shell commands in reproduction steps must properly quote arguments

## Testing Coverage

Three levels of testing ensure reliability:

### Unit Tests (CreateBugCliTest)
- Description validation (min/max length)
- Slug generation edge cases
- ID auto-increment
- Template instantiation

### Acceptance Tests (BugCreationAcceptanceTest)
- 4 formal ACs (valid creation, no regressions, performance < 500ms, all 9 sections)
- 6 scenarios (special chars, sequential IDs, frontmatter, filename pattern, status, full workflow)

### Smoke Tests (BugCreationSmokeIT)
- End-to-end integration test
- Simulates full bug lifecycle workflow
- Validates file I/O and persistence

## Troubleshooting

### Bug File Not Created

**Problem:** `/x-create-bug` returns error, no file created

**Solution:**
1. Verify `docs/bugs/` directory exists: `mkdir -p docs/bugs`
2. Check description length: must be 8–200 characters
3. Ensure template exists: `ls src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md`
4. Verify file permissions: `chmod 755 docs/bugs`

### Slug Too Short or Missing

**Problem:** Slug is empty or doesn't represent the bug

**Solution:**
- Description with only special characters (e.g., "#@!#@!") generates empty slug
- Provide description with alphanumeric content
- Special characters are stripped; use hyphens instead

### Status Not Updating

**Problem:** Status remains "Pendente" when should be "Refinada"

**Solution:**
1. Manual update: edit `**Status:** Pendente` in the file
2. Use `/x-refine-bug` to transition to Refinada
3. Verify file is committed to git

## Related Documentation

- [Bug Lifecycle Capability](../config/capabilities/bug-lifecycle.yaml)
- [Bug Template](../src/main/resources/targets/claude/templates/_TEMPLATE-BUG.md)
- [EPIC-0080 — Bug Lifecycle Management](../ai/epics/epic-0080-bug-lifecycle-management/)
- [Create Bug Skill](../.claude/skills/dev/x-create-bug/SKILL.md)
- [Refinement Gate](../CLAUDE.md#refinement-gate)

## Support

For issues or questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review test examples in `CreateBugCliTest` and `BugCreationAcceptanceTest`
3. File a bug using this capability: `/x-create-bug "Issue with bug lifecycle..."`

---

**Last Updated:** 2026-05-07  
**Version:** 1.0  
**Maintained By:** EPIC-0080 (story-0080-0001, task-0080-0001-006)
