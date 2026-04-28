# Dependency Audit — story-0063-0014

**Story:** Sub-Skill Wave Dispatch Audit
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28
**Status:** CLEAN

## Runtime Dependencies

| Dependency | Version | Source | Risk |
|---|---|---|---|
| `jq` | ≥ 1.6 | System package | LOW — standard JSON processor, no CVEs |
| `bash` | ≥ 4.0 | System shell | LOW — uses mapfile (bash 4+), standard in CI |
| `date` | GNU coreutils / BSD date | System tool | LOW — cross-platform fallback implemented |

## Dependency Analysis

### jq
- Required for NDJSON filtering (`.type == "subagent.start"`)
- Version ≥ 1.6 for `select()` with compound conditions
- Available on all standard CI runners (Ubuntu, macOS)
- `--self-check` validates availability before any audit operation

### bash
- `mapfile` (aka `readarray`) requires bash 4+
- CI environments (GitHub Actions ubuntu-latest) ship bash 5+
- macOS with Homebrew ships bash 5+; system bash on macOS 14 is 3.2 (insufficient)
- Mitigation: script shebang uses `/usr/bin/env bash` — resolves to Homebrew bash on macOS

### date
- GNU `date -d` (Linux) vs BSD `date -jf` (macOS) handled via fallback in `ts_to_epoch`
- No external date library required

## Vulnerabilities

None identified. All dependencies are standard system tools with no known security advisories
relevant to this use case (offline script, no network calls, no user input passed to shell eval).

## Verdict: CLEAN
