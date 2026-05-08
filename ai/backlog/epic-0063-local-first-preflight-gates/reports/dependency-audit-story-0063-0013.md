# Dependency Audit — story-0063-0013

**Story:** PreToolUse Hook v2 — 15 Additional Bypass Vectors
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28

## External Dependencies

| Dependency | Type | Version Req | Reason |
| :--- | :--- | :--- | :--- |
| `bash` | Shell runtime | 4.0+ | Associative arrays, `set -uo pipefail` |
| `jq` | JSON processor | 1.6+ | Parse PreToolUse JSON payload |
| `git` | VCS | Any | Branch detection via `symbolic-ref` |

## Vulnerability Assessment

| Dependency | Known CVEs | Risk |
| :--- | :--- | :--- |
| bash | None relevant | Low — hook uses safe patterns (no eval, no unsafe expansion) |
| jq | None relevant | Low — used for read-only JSON extraction |
| git | None relevant | Low — used for branch name detection only |

## File Footprint

```
write:
  - java/src/main/resources/targets/claude/hooks/enforce-preflight-gates-v2.sh (NEW)
  - .claude/hooks/enforce-preflight-gates-v2.sh (NEW, copy)
  - src/test/shell/enforce_preflight_gates_v2_test.sh (NEW)
  - docs/preflight-bypass-vectors.md (NEW)
read:
  - .claude/hooks/enforce-preflight-gates.sh (reference for v1 patterns)
  - .claude/rules/24-execution-integrity.md (Rule 24 §Camada 0)
  - .claude/rules/27-zero-bypass-lifecycle.md (bypass contract)
  - plans/unknown/telemetry/events.ndjson (NDJSON telemetry target)
regen:
  - (none — v2 is a new file, not a regenerated output)
```

## Dependency Graph (story)

```
story-0063-0004 (hook v1, schema base)
    └── story-0063-0013 (this story — v2 hook + vector field)
            ├── story-0063-0017 (recovery audit — consumes vector field)
            └── story-0063-0016 (rollout WARN→FAIL — consumes PREFLIGHT_PHASE)
```

## License

All dependencies (bash, jq, git) are open source under compatible licenses (GPL-2+, MIT, GPL-2+). No license conflicts.

## Verdict

**PASS** — no vulnerabilities, all dependencies standard system tools, no transitive dependency risks.
