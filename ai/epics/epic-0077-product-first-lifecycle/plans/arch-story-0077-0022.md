# Architecture Plan — story-0077-0022

**Story:** x-internal-rnf-validate (no-relax markers + justification gate)
**Date:** 2026-05-05

## Layer Assignment

| Component | Layer | Package |
| :--- | :--- | :--- |
| `ValidateRNFNoRelaxUseCase` | application | `dev.iadev.application.capability` |
| `XInternalRnfValidateCommand` | adapter.inbound.cli | `dev.iadev.adapter.inbound.cli` |
| `x-internal-rnf-validate/SKILL.md` | skill source-of-truth | `src/main/resources/targets/claude/skills/core/internal/ops/` |

## Domain Reuse

The following domain classes from stories 0006/0007 are reused without modification:

| Class | Role |
| :--- | :--- |
| `RNFNoRelaxValidator` | Validates overrides: hard-blocks SECURITY/COMPLIANCE; requires justification for others |
| `RNFOverride` | Value object (record) representing one RNF override with noRelaxed flag |
| `RNFRootValidationResult` | Result type (passed, errors list) |
| `RNFCategory` | Enum of RNF categories |

## Dependency Direction

```
XInternalRnfValidateCommand → ValidateRNFNoRelaxUseCase → RNFNoRelaxValidator (domain)
```

No new ports needed — `RNFNoRelaxValidator` is a pure domain computation.

## CLI Interface Design

Override spec format (repeatable `--override` flag):
- `CATEGORY:norelax` — RNF carried forward unchanged
- `CATEGORY:norelax:originalValue` — same, with context  
- `CATEGORY:relaxed:originalValue:newValue:justification` — relaxed RNF with mandatory justification

Split by `:` preserving all parts via `split(":", -1)`.

## SKILL.md Placement

Source-of-truth: `src/main/resources/targets/claude/skills/core/internal/ops/x-internal-rnf-validate/SKILL.md`
Generated output: `.claude/skills/x-internal-rnf-validate/SKILL.md`

## File Footprint

```yaml
write:
  - src/main/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCase.java
  - src/main/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommand.java
  - src/main/resources/targets/claude/skills/core/internal/ops/x-internal-rnf-validate/SKILL.md
  - .claude/skills/x-internal-rnf-validate/SKILL.md
  - src/test/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCaseIT.java
  - src/test/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommandTest.java
read:
  - src/main/java/dev/iadev/domain/capability/RNFNoRelaxValidator.java
  - src/main/java/dev/iadev/domain/capability/RNFOverride.java
  - src/main/java/dev/iadev/domain/product/RNFRootValidationResult.java
  - src/main/java/dev/iadev/domain/product/RNFCategory.java
regen: []
```
