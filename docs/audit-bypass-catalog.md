# Audit Bypass Catalog

> **Generated:** 2026-04-28 (story-0063-0009)
> **Purpose:** Catalogue of skills with file-based or telemetry-based evidence and the hardening roadmap (EPIC-0063 RULE-002 + RULE-003).

This document lists every skill that produces auditable evidence in `plans/epic-*/` or `ai/epics/*/`, the type of evidence it currently produces, observed/possible bypass patterns, and the planned hardening to close the gap.

**Evidence type enum:**
- `file` — only markdown/json artifact in plans/reports
- `telemetry` — only NDJSON event in events.ndjson
- `both` — produces file + telemetry, but neither is cross-validated
- `schema` — file is JSON-validated against schema (envelope-style)

---

## 1. x-review-codebase

- **Tipo de evidência hoje:** both (file + telemetry, but telemetry not validated as gate)
- **Padrão de bypass observado:** EPIC-0062 — review-story-*.md com 2 linhas "Verdict: GO" passa Camada 3
- **Plano de blindagem:**
  - story-0063-0002 (Content Audits): heurísticas sintáticas (≥50 lines, ≥3 sections, ≥2 file refs, decision marker)
  - story-0063-0003 (Telemetry Audit): cross-validate ≥1 evento `tool.call x-review-codebase` por story merged

## 2. x-review-pr

- **Tipo de evidência hoje:** both
- **Padrão de bypass observado:** Tech-Lead review com bullet list trivial e GO sem análise real
- **Plano de blindagem:**
  - story-0063-0002 (mesma heurística aplica a techlead-review-*.md)
  - story-0063-0003 (≥1 evento `tool.call x-review-pr`)

## 3. x-internal-verify-story

- **Tipo de evidência hoje:** schema (verify-envelope-*.json) + telemetry
- **Padrão de bypass observado/possível:** envelope com `passed=true, failures=[], acCheckResults=[]` (zero AC checks executados)
- **Plano de blindagem:**
  - story-0063-0002 (audit-verify-envelope.sh): valida `acCheckResults.length >= acCheckCount` quando `passed=true`
  - story-0063-0003 (telemetry): ≥1 evento `tool.call x-internal-verify-story`

## 4. x-audit-dependencies

- **Tipo de evidência hoje:** file (dependency-audit-*.md)
- **Padrão de bypass possível:** report stub com "0 vulnerabilidades" sem rodar `mvn dependency:analyze` real
- **Plano de blindagem:**
  - **Backlog:** Tier-2 — output validation contra real Maven plugin output (CycloneDX BOM cross-check)
  - story-0063-0003 (telemetry parcial): evento `tool.call x-audit-dependencies` confirma invocação

## 5. x-plan-architecture

- **Tipo de evidência hoje:** file (arch-story-*.md) + telemetry (when scope=STANDARD+)
- **Padrão de bypass possível:** plano arquitetural com seções vazias ou genéricas
- **Plano de blindagem:**
  - story-0063-0015 (Planning-Content Audits): heurísticas para 6 artefatos Phase 1 (arch + impl + test + tasks + security + compliance)
  - Telemetria já registrada via x-internal-build-story-plan dispatch

## 6. x-drive-tdd

- **Tipo de evidência hoje:** commit history (TDD tags `[TDD:RED]`, `[TDD:GREEN]`)
- **Padrão de bypass possível:** commits de test e implementação no mesmo commit (não TDD)
- **Plano de blindagem:**
  - **Backlog:** Tier-2 — commit-order audit (test commit precedes implementation per task)
  - Auditar timeline de telemetria: tool.call x-drive-tdd RED antes de GREEN

## 7. x-plan-task

- **Tipo de evidência hoje:** file (plan-task-TASK-*.md, schema v2)
- **Padrão de bypass possível:** plan-task ausente para tarefas v2 (silently skipped)
- **Plano de blindagem:**
  - **Backlog:** Tier-2 — pre-flight gate verifies plan-task-*.md exists for every TASK-id em tasks-story-*.md
  - story-0063-0015 (Planning-Content Audits): valida plan-task-*.md tem mínimo de seções

## 8. x-model-threats

- **Tipo de evidência hoje:** file (threat-model-story-*.md, soft check)
- **Padrão de bypass possível:** threat model omitido para stories tocando auth/network/persistence
- **Plano de blindagem:**
  - **Backlog:** Tier-2 — story classification: detect auth/network/persistence keywords in story.md, fail if threat model absent
  - Telemetria: enforce evento `tool.call x-model-threats` for stories matching keyword set

## 9. x-execute-tests

- **Tipo de evidência hoje:** file (test-run-*.txt, soft check) + JaCoCo report
- **Padrão de bypass possível:** test-run report sem coverage data
- **Plano de blindagem:**
  - story-0063-0007 (Local Coverage Gate): audit-coverage-local.sh valida JaCoCo CSV contra thresholds
  - **Backlog:** Tier-2 — JaCoCo report committable (não gitignore) para audit retroativo

## 10. x-detect-spec-drift

- **Tipo de evidência hoje:** none (skill standalone, no required artifact)
- **Padrão de bypass possível:** skill nunca invocada — drift acumula silenciosamente
- **Plano de blindagem:**
  - **Backlog:** Tier-3 — periodic gate (e.g., monthly cron CI job) que invoca x-detect-spec-drift e fails CI if drift detected
  - Telemetria: evento `tool.call x-detect-spec-drift` com timestamp da última execução

## 11. x-audit-code

- **Tipo de evidência hoje:** none (skill standalone, no required artifact)
- **Padrão de bypass possível:** skill nunca invocada antes de release
- **Plano de blindagem:**
  - **Backlog:** Tier-3 — release branch gate (`x-release` Step N requires x-audit-code invocation)
  - Telemetria: evento `tool.call x-audit-code` registrado durante release flow

---

## Hardening Tier Summary

| Tier | Scope | Enforcement |
| :--- | :--- | :--- |
| **Tier-1 (in EPIC-0063)** | x-review-codebase, x-review-pr, x-internal-verify-story | Camada 0 (preflight) + Camada 3 (CI audit) ALL artifacts validated |
| **Tier-2 (backlog)** | x-audit-dependencies, x-drive-tdd, x-plan-task, x-model-threats | Output/timeline validation against external sources |
| **Tier-3 (backlog)** | x-detect-spec-drift, x-audit-code | Periodic / lifecycle-bound enforcement |

## Bypass Pattern Taxonomy

Three observed patterns from EPIC-0062 post-mortem:

1. **Content Stub** — file exists but has trivial/placeholder content (e.g., 2-line review)
2. **Schema Stub** — JSON envelope passes structural check but encodes "passed=true, failures=[]"  with no actual gate logic executed
3. **Skill Skip** — skill never invoked; no file, no telemetry, lifecycle silently completes

Tier-1 closes patterns 1 and 2 via content + schema audits. Tier-2 closes pattern 3 via telemetry cross-validation.

## References

- EPIC-0063 SPEC §1.2 — Causa raiz (post-mortem EPIC-0062)
- Rule 24 — Execution Integrity (4-camada enforcement)
- Rule 26 — Audit Gate Lifecycle (5-camada com Camada 0 preventive)
- Rule 27 — Zero-Bypass Lifecycle Contract
