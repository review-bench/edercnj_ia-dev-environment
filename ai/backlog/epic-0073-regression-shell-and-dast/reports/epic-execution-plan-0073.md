# Epic Execution Plan — EPIC-0073 (Regression Shell + DAST)

**Generated:** 2026-05-01
**Flow Version:** 4
**Mode:** sequential (with parallel waves in Phases 1 and 2)
**Epic Branch:** epic/0073
**Target Branch:** develop

---

## Execution Phases

### Phase 0 — Governance + Schema (sequential)

| Story | Title | Status |
|-------|-------|--------|
| story-0073-0001 | Schema YAML `quality.{regression,dast}` + sub-records Java + capabilities + ADR | Pendente |

**Critical path start.** All Phase 1 stories unblocked after this.

---

### Phase 1 — Skills + Templates + Infra (3 parallel stories)

| Story | Title | Status | Blocked By |
|-------|-------|--------|-----------|
| story-0073-0002 | Skill `/x-test-regression-shell` (self + service) + template + KP | Pendente | 0001 |
| story-0073-0003 | Skill `/x-pentest-dynamic` (smoke + full) + template + KP DAST | Pendente | 0001 |
| story-0073-0004 | `scenarios.yaml.template` + `ScriptsAssembler` | Pendente | 0001 |

**File footprint: isolated** — no hotspot conflicts within Phase 1.

---

### Phase 2 — CI Workflows + Audit (2 parallel stories)

| Story | Title | Status | Blocked By |
|-------|-------|--------|-----------|
| story-0073-0005 | CI `dast-smoke.yml` (PR) + `audit-regression-shell.sh` | Pendente | 0002, 0004 |
| story-0073-0006 | CI `dast-full.yml` (nightly cron) | Pendente | 0003 |

**File footprint: isolated** — `dast-smoke.yml` ≠ `dast-full.yml`.

---

### Phase 3 — Integration + Smoke + Release (sequential)

| Story | Title | Status | Blocked By |
|-------|-------|--------|-----------|
| story-0073-0007 | Phase 3 MODIFIED + `Epic0073RegressionDastSmokeIT` + CHANGELOG | Pendente | 0005, 0006 |

---

## Critical Path

```
0073-0001 → 0073-0002 → 0073-0005 → 0073-0007
```

4 stories on critical path across 4 execution phases.

---

## Story Count

- Total: 7 stories
- Sequential dependencies: story-0073-0001, story-0073-0007
- Parallel waves: Phase 1 (3 stories), Phase 2 (2 stories)

---

## Known Coordination Risks

| Risk | Stories | Mitigation |
|------|---------|-----------|
| EPIC-0072 Phase 3 conflict (x-story-implement.SKILL.md) | 0007 | extend vs greenfield decided in story-0073-0001 |
| scenarios.yaml schema drift (0002 ↔ 0004) | 0002, 0004 | schema defined in 0002, materialized in 0004 |
| `ScriptsAssembler.java` multi-writer | 0001, 0004, 0005 | sequential PRs; merge order: 0001 → 0004 → 0005 |
