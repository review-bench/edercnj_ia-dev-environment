# Implementation Plan — story-0077-0027

**Story:** Scripts audit Camada 2: product-upstream + c4-completeness + rnf-gates + pentest-coverage
**Status:** Concluída
**Planned at:** 2026-05-05T19:30:00Z

## 1. Scope

Implements four new Camada 2 CI audit scripts for Product-First governance gates:
- `audit-c4-completeness.sh` — verifies C4 diagrams exist at all required levels (Context, Container, Component, Code) for each Feature
- `audit-pentest-coverage.sh` — verifies pentest evidence exists for all SECURITY-category RNFs
- `audit-product-upstream.sh` — verifies traceability chain: Story → Feature → Capability → Product
- `audit-rnf-gates.sh` — verifies all RNF inheritance approvals in story markdown files
- `audit-refinement-gate.sh` — updated with RNF inheritance violation detection (exit 34)

## 2. File Footprint

**write:**
- `src/main/resources/targets/claude/scripts/audit-c4-completeness.sh`
- `src/main/resources/targets/claude/scripts/audit-pentest-coverage.sh`
- `src/main/resources/targets/claude/scripts/audit-product-upstream.sh`
- `src/main/resources/targets/claude/scripts/audit-rnf-gates.sh`
- `src/main/resources/targets/claude/scripts/audit-refinement-gate.sh`
- `src/test/bash/audit_c4_completeness_test.sh`
- `src/test/bash/audit_pentest_coverage_test.sh`
- `src/test/bash/audit_product_upstream_test.sh`
- `src/test/bash/audit_refinement_gate_test.sh`
- `src/test/bash/audit_rnf_gates_test.sh`
- Golden files (9 profiles × audit-refinement-gate.sh)

## 3. Architecture

All scripts follow Rule 26 §Camada 2 conventions:
- Naming: `audit-*.sh` prefix
- Exit codes: 0=OK, 1=violation, 2=operational error, 3=baseline corrupt
- `--self-check` flag implemented
- Named violation constants on stderr

## 4. AC Coverage

| AC | Result |
|---|---|
| audit-c4-completeness.sh detects missing C4 levels | PASS |
| audit-pentest-coverage.sh detects missing pentest evidence | PASS |
| audit-product-upstream.sh detects broken traceability chain | PASS |
| audit-rnf-gates.sh detects unapproved RNF relaxations | PASS |
| audit-refinement-gate.sh updated with RNF_INHERITANCE_VIOLATION | PASS |
| All scripts pass --self-check | PASS |
| Golden files updated (9 profiles) | PASS |
