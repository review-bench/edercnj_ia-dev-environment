---
generated-by: x-story-implement-security@0792be069d39537b5f4c7c76d7e68372b585697f
generated-at: 2026-04-27T16:50:49Z
story-id: story-0059-0002
---

# Security Assessment — story-0059-0002: Origin Markers in Artifacts + Anti-Backfill Audit

## Threat Model Summary

**STRIDE analysis scope:** Bash audit script extensions + SKILL.md modifications.

## Identified Threats

### T1: SHA Collision / Length Extension Attack

**Category:** Tampering
**Severity:** LOW
**Mitigation:** Git SHA-1 collisions require hardware-accelerated effort and are detectable by Git's own integrity checks. The 40-char hex format regex rejects truncated or malformed values. For practical purposes, git cat-file -t validation is sufficient.

**Status:** Accepted (residual risk negligible)

### T2: Timestamp Manipulation in Frontmatter

**Category:** Spoofing
**Severity:** LOW
**Mitigation:** The audit does NOT rely on the `generated-at` timestamp field for security decisions. The `generated-at` field is informational only. Anti-backfill logic uses `git log --pretty=format:%ct` (Git's authoritative commit timestamp), not the frontmatter timestamp.

**Status:** Accepted (timestamp field is metadata only)

### T3: Regex Bypass (crafted skill names)

**Category:** Tampering
**Severity:** LOW
**Mitigation:** The `generated-by` regex `^[a-z-]+@[0-9a-f]{40}$` only allows lowercase letters and hyphens for the skill name portion. Characters like `/`, `\`, `..`, `;` are rejected. Shell injection via the skill name field is not possible because the SHA is extracted and passed to `git cat-file` as a positional argument (not interpolated in a string command).

**Status:** Mitigated

### T4: Git Log Injection via Story ID

**Category:** Injection
**Severity:** MEDIUM
**Mitigation:** The `discover_merged_stories()` function extracts story IDs via `grep -oE 'story-[0-9]{4}-[0-9]{4}'`, which only matches digits. The story ID passed to `check_anti_backfill` cannot contain shell metacharacters.

**Status:** Mitigated (numeric-only story IDs)

### T5: Timing Attack (artifact committed exactly at merge timestamp)

**Category:** Tampering
**Severity:** NEGLIGIBLE
**Mitigation:** The anti-backfill check uses strict `>` (after), not `>=`. An artifact committed at the exact same timestamp as the merge is treated as legitimate. Since merge timestamps have 1-second granularity and artifacts must be authored in the same commit as the merge (edge case), this is acceptable.

**Status:** Accepted

## Security Controls

| Control | Type | Implementation |
| :--- | :--- | :--- |
| Input validation on SHA | Preventive | Regex `^[0-9a-f]{40}$` before git call |
| git cat-file isolation | Detective | SHA passed as argument, not interpolated |
| Story ID sanitization | Preventive | Pattern match `story-[0-9]{4}-[0-9]{4}` only |
| Fail-open for git errors | Resilience | `git cat-file` failure → warn + skip, not fail build |
| Baseline exemption preserved | Resilience | Grandfathered stories skip new checks |

## OWASP Mapping

| OWASP Category | Relevance | Control |
| :--- | :--- | :--- |
| A03 — Injection | Low (Bash script) | Positional arg passing, numeric-only story IDs |
| A04 — Insecure Design | Low | Fail-open design intentional for resilience |
| A09 — Security Logging | Partial | Audit outputs clear error messages to stderr |

## Security Sign-off

**Risk Level:** LOW — The changes are confined to SKILL.md documentation files and a Bash audit script. No runtime code or network calls are introduced. The SHA validation uses existing Git primitives. The primary security benefit (detecting retroactive backfill) outweighs the minimal attack surface introduced.
