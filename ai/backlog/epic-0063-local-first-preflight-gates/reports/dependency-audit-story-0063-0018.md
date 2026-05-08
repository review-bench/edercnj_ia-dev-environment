# Dependency Audit — story-0063-0018

**Story:** story-0063-0018 — Hooks --self-check Contract
**Epic:** EPIC-0063
**Audit date:** 2026-04-28
**Status:** OK

## Summary

This story adds two Bash scripts and one shell test. Dependencies:

| Dependency | Version | Source | Vulnerability |
|------------|---------|--------|---------------|
| bash | system | macOS / Linux | None |
| jq | ≥ 1.6 | system | None |
| git | ≥ 2.x | system | None |

No new Maven, npm, or pip dependencies introduced.

## Findings

No dependency vulnerabilities detected. All tools (`jq`, `git`, `bash`) are standard
system utilities already present in the development environment.
