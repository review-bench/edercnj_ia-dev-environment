# Task Breakdown — story-0077-0006

## TASK-0077-0006-001
- Branch: `feat/task-0077-0006-001-capability-template`
- Files: `ai/templates/_TEMPLATE-CAPABILITY.md`, `ai/examples/example-capability-auth.md`, `ai/examples/example-capability-payment.md`
- AC: 7 sections, no-relax mechanism visible, examples validate with smoke script

## TASK-0077-0006-002
- Branch: `feat/task-0077-0006-002-capability-entity`
- Files: `domain/capability/Capability.java`, `domain/capability/RNFOverride.java`, `domain/capability/RNFNoRelaxValidator.java`, `application/capability/CapabilityCreationUseCase.java`, test
- AC: TDD order, justification enforcement, mandatory hard-block

## TASK-0077-0006-003
- Branch: `feat/task-0077-0006-003-rnf-approval-flow`
- Files: `domain/capability/RNFOverrideApprovalPort.java`, `adapter/outbound/approval/RNFOverrideApprovalAdapter.java`, `application/capability/RNFOverrideApprovalUseCase.java`, IT
- AC: pending → approved → rejected lifecycle

## TASK-0077-0006-004
- Branch: `feat/task-0077-0006-004-capability-smoke`
- Files: `ci/smoke/capability-template-smoke.sh`, `ci/audit/rnf-override-audit.sh`
- AC: smoke passes on valid example, audit --self-check exit 0
