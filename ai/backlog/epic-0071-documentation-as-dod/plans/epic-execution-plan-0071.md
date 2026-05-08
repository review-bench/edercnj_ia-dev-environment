# Epic Execution Plan — EPIC-0071 (Documentation as DoD)

**Generated:** 2026-04-30
**flowVersion:** 4
**Mode:** sequential (with parallel waves per phase)
**Stories:** 8

## Phase 0 — Governance + Domain (sequential)
- story-0071-0001: Capability + Rule 31 + ADR + YAML schema

## Phase 1 — Skills + Templates (parallel, 3 stories)
- story-0071-0002: Skill `/x-doc-validate` (target stack-aware)
- story-0071-0003: `x-doc-generate` v2
- story-0071-0004: `x-release-changelog` v2 + TEMPLATE-CHANGELOG-ENTRY

## Phase 2 — Infrastructure + Integration (sequential)
- story-0071-0005: CI script `audit-doc-freshness.sh`
- story-0071-0006: Phase 3 de x-story-implement MODIFIED (MANDATORY)
- story-0071-0007: Dogfood: primeiro changelog híbrido

## Phase 3 — Smoke Test + Release (sequential)
- story-0071-0008: E2E smoke + CHANGELOG MAJOR

## Critical Path
0001 → 0002 → 0006 → 0008 (4 hops)
