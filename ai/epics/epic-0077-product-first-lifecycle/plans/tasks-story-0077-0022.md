# Task Breakdown — story-0077-0022

**Story:** x-internal-rnf-validate (no-relax markers + justification gate)
**Date:** 2026-05-05

## Tasks

| Task ID | Description | Layer | Effort |
| :--- | :--- | :--- | :--- |
| TASK-0077-0022-001 | ValidateRNFNoRelaxUseCase + IT | application | S |
| TASK-0077-0022-002 | XInternalRnfValidateCommand + unit tests | adapter.inbound.cli | S |
| TASK-0077-0022-003 | SKILL.md (source-of-truth + generated copy) | skill | S |

## TASK-0077-0022-001

**Branch:** `feat/task-0077-0022-001-validate-rnf-no-relax-use-case`

Write:
- `src/main/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCase.java`
- `src/test/java/dev/iadev/application/capability/ValidateRNFNoRelaxUseCaseIT.java`

Dependencies: none (domain classes pre-exist from stories 0006/0007)

## TASK-0077-0022-002

**Branch:** `feat/task-0077-0022-002-x-internal-rnf-validate-command`

Write:
- `src/main/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommand.java`
- `src/test/java/dev/iadev/adapter/inbound/cli/XInternalRnfValidateCommandTest.java`

Dependencies: TASK-0077-0022-001

## TASK-0077-0022-003

**Branch:** `feat/task-0077-0022-003-x-internal-rnf-validate-skill-md`

Write:
- `src/main/resources/targets/claude/skills/core/internal/ops/x-internal-rnf-validate/SKILL.md`
- `.claude/skills/x-internal-rnf-validate/SKILL.md`

Dependencies: TASK-0077-0022-002 (for accurate parameter docs)

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
