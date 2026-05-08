# Implementation Plan — story-0077-0006

## Task Sequence

### TASK-0077-0006-001: `_TEMPLATE-CAPABILITY.md` + examples
- Create `ai/templates/_TEMPLATE-CAPABILITY.md` with 7 sections
- Section 2 includes no-relax override table with examples
- Create `ai/examples/example-capability-auth.md` (Authentication capability)
- Create `ai/examples/example-capability-payment.md` (Payment capability)

### TASK-0077-0006-002: Capability Entity + RNF Override (Domain)
- TDD: write `RNFNoRelaxValidatorTest` first (RED)
- Implement `RNFOverride` record with justification constraint
- Implement `RNFNoRelaxValidator` (GREEN)
- Implement `Capability` final class
- Implement `CapabilityCreationUseCase`
- Refactor (REFACTOR)

### TASK-0077-0006-003: Approval Flow Integration
- TDD: write `RNFOverrideApprovalIT` first (RED)
- Implement `RNFOverrideApprovalPort` (domain outbound port)
- Implement `RNFOverrideApprovalAdapter` (stub, in-memory)
- Implement `RNFOverrideApprovalUseCase`
- GREEN + REFACTOR

### TASK-0077-0006-004: Smoke + Audit scripts
- Create `ci/smoke/capability-template-smoke.sh`
  - Validates 7 sections in example-capability-auth.md
  - Validates no-relax mechanism presence
- Create `ci/audit/rnf-override-audit.sh`
  - Lists all RNF overrides with status from yaml files
  - --self-check flag (Rule 26)
