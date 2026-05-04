# ADR-0015 — Zero-Bypass Amnesty for EPIC-0054 through EPIC-0057

**Status:** Accepted
**Date:** 2026-04-27
**Supersedes:** —
**Superseded by:** —
**Related:** Rule 24 (Execution Integrity), Rule 26 (Audit Gate Lifecycle), Rule 27 (Zero-Bypass Lifecycle Enforcement), EPIC-0059 (Zero-Bypass Lifecycle Enforcement)

## Context

EPIC-0059 (Zero-Bypass Lifecycle Enforcement) introduced a suite of strict enforcement gates:

- `audit-execution-integrity.sh` — Phase 1 planning artifacts (x-internal-build-story-plan wave) mandatory for every merged story
- `audit-pr-evidence.sh` — Orchestrator evidence artifacts required per merged PR
- Telemetry enforcement via `events.ndjson` phase markers
- Baseline immutability after EPIC-0059 cutoff (story-0059-0011)

The epics **EPIC-0054**, **EPIC-0055**, **EPIC-0056**, and **EPIC-0057** were merged into `develop` before any of these enforcement gates existed. Retroactively applying the new gates to these historical epics would:

1. Break CI for all future PRs that reference historical story IDs via `git log` scanning.
2. Create false positives: these stories were compliant with the rules in effect at the time of their merge.
3. Violate the spirit of backward compatibility (Rule 19): enforcement gates must not penalize work that was legal when done.

### Inventory of Grandfathered Epics

| Epic | Story count | Reason for amnesty |
| :--- | :--- | :--- |
| EPIC-0054 | 4 stories (0054-0001 through 0054-0004) | Merged pre-EPIC-0059; no Phase-1 artifact gate existed |
| EPIC-0055 | 12 stories (0055-0001 through 0055-0012) | Merged pre-EPIC-0059; Task Hierarchy enforcement was EPIC-0055 itself |
| EPIC-0056 | 8 stories (0056-0001 through 0056-0008) | Merged pre-EPIC-0059; no PR evidence baseline existed |
| EPIC-0057 | 8 stories (0057-0001 through 0057-0008) | Merged pre-EPIC-0059; CI-Watch integrity was being established |

**Total grandfathered stories: 32**

## Decision

Grant **formal amnesty** to all 32 stories from EPIC-0054 through EPIC-0057, with the following implementation:

### 1. Entries in `audits/execution-integrity-baseline.txt`

All 32 stories are added to `audits/execution-integrity-baseline.txt` with the amnesty comment format:

```
story-0054-0001  # amnesty EPIC-0059: merged before Phase-1 artifact gate
```

This was completed by **story-0059-0001** (PR #689). The entries were authorized during the active amnesty window (while EPIC-0059 was in progress).

### 2. New file `audits/rule-26-baseline.txt`

A dedicated baseline file for the Rule 26 (Audit Gate Lifecycle) enforcement scope is created, covering the same 32 stories. Created by **story-0059-0011** (this story).

### 3. Cutoff date

The amnesty window closes at the **merge of story-0059-0011** into `epic/0059`. The commit SHA of the story-0059-0011 amnesty commit is recorded in `audits/baseline-cutoff.sha`. After this point, no new entries may be added to the baseline files without a `<!-- baseline-correction: <reason> -->` marker reviewed via CODEOWNERS.

### 4. Immutability enforcement

`scripts/audit-baseline-immutability.sh` (created by story-0059-0011) enforces that no new story IDs are added to the baseline files after the cutoff SHA:

- Exit 0: immutable — no violations
- Exit 1: `BASELINE_IMMUTABILITY_VIOLATION` — new entries after cutoff
- Exemption: `<!-- baseline-correction: <reason> -->` marker on the preceding line requires CODEOWNERS approval

The CI workflow (`.github/workflows/ci-release.yml`) runs this script on every PR.

### 5. Format

Baseline entries follow the format:

```
story-XXXX-YYYY  # amnesty EPIC-0059: <reason> (<date>)
```

Where `<reason>` is one of:
- `merged before Phase-1 artifact gate`
- `merged before Rule-26 audit gate enforcement`

## Consequences

### Positive

- EPIC-0054 through EPIC-0057 stories no longer trigger CI failures when referenced by historical audit scans.
- The amnesty is **explicit and documented** — no silent exemption, no implicit grandfather.
- The cutoff is **mechanically enforced** — `audit-baseline-immutability.sh` prevents future additions without review.
- Developers can audit the amnesty: `grep "amnesty EPIC-0059" audits/execution-integrity-baseline.txt` lists all exemptions with reasons.

### Negative

- The baseline files now carry 32+ amnesty entries, increasing file size.
- The cutoff SHA must be maintained; if the `baseline-cutoff.sha` file is deleted, the script falls back to detecting the creation commit of `rule-26-baseline.txt` (adequate fallback, but less explicit).

### Neutral

- Stories merged within EPIC-0059 itself (stories 0059-0001 through 0059-0012) are NOT grandfathered — they must comply with all enforcement gates. Only stories from the four historical epics receive amnesty.
- The enforcement is forward-only: new epics created after EPIC-0059 merges must produce all required artifacts or add an explicit per-story `<!-- audit-exempt: <reason> -->` marker in the story markdown.

## Related Artifacts

| Artifact | Location | Notes |
| :--- | :--- | :--- |
| `audits/execution-integrity-baseline.txt` | Repo root | EPIC-0054–0057 entries added by story-0059-0001 |
| `audits/rule-26-baseline.txt` | Repo root | New file, created by story-0059-0011 |
| `audits/baseline-cutoff.sha` | Repo root | Records cutoff commit SHA |
| `scripts/audit-baseline-immutability.sh` | Repo root | Immutability enforcement script |
| Rule 24 | `.claude/rules/24-execution-integrity.md` | Defines baseline format |
| Rule 26 | `.claude/rules/26-audit-gate-lifecycle.md` | Defines audit taxonomy |
| Rule 27 | `.claude/rules/27-zero-bypass-lifecycle.md` | EPIC-0059 enforcement rule |
