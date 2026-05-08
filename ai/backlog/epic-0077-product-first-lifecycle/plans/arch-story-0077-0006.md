# Architecture Plan — story-0077-0006

## New Packages

```
domain/capability/
  Capability.java           — aggregate root: capabilityId, productId, inheritedRNFs, overrides
  RNFOverride.java          — value object: rnfCategory, originalValue, overrideValue, noRelaxed, justification, approvalStatus, approver
  RNFNoRelaxValidator.java  — validates no override without justification when noRelaxed=false
  RNFOverrideApprovalPort.java — outbound port: requestApproval(override), updateApprovalStatus(rnfId, status)

application/capability/
  CapabilityCreationUseCase.java    — creates Capability, validates no-relax via domain validator
  RNFOverrideApprovalUseCase.java   — manages approval lifecycle: request → pending → approved/rejected

adapter/outbound/approval/
  RNFOverrideApprovalAdapter.java   — stub implementation (in-memory / event log)
```

## Dependency Direction

```
adapter.outbound.approval → domain.capability.RNFOverrideApprovalPort (interface)
application.capability    → domain.capability.*
domain.capability         → java.util.* only
```

## Key Design Decisions

1. `RNFOverride` is a value object (record) — immutable; factory method validates justification requirement
2. `Capability` is a final class with defensive copy of RNF lists
3. `RNFNoRelaxValidator` is a pure domain validator — no I/O, no ports
4. `RNFOverrideApprovalPort` is an outbound port — decouples domain from approval mechanism
5. Approval flow stub: in-memory list in adapter (sufficient for story scope)

## File Footprint

```
write:
  - ai/templates/_TEMPLATE-CAPABILITY.md
  - ai/examples/example-capability-auth.md
  - ai/examples/example-capability-payment.md
  - src/main/java/dev/iadev/domain/capability/Capability.java
  - src/main/java/dev/iadev/domain/capability/RNFOverride.java
  - src/main/java/dev/iadev/domain/capability/RNFNoRelaxValidator.java
  - src/main/java/dev/iadev/domain/capability/RNFOverrideApprovalPort.java
  - src/main/java/dev/iadev/application/capability/CapabilityCreationUseCase.java
  - src/main/java/dev/iadev/application/capability/RNFOverrideApprovalUseCase.java
  - src/main/java/dev/iadev/adapter/outbound/approval/RNFOverrideApprovalAdapter.java
  - src/test/java/dev/iadev/domain/capability/RNFNoRelaxValidatorTest.java
  - src/test/java/dev/iadev/application/capability/RNFOverrideApprovalIT.java
  - ci/smoke/capability-template-smoke.sh
  - ci/audit/rnf-override-audit.sh
read:
  - ai/templates/_TEMPLATE-PRODUCT.md
  - src/main/java/dev/iadev/domain/product/RNFCategory.java
```
