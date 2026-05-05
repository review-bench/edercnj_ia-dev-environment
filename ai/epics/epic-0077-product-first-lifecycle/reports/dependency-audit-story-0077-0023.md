# Dependency Audit — story-0077-0023

**Story:** story-0077-0023 — Gate em DoR: RNF_INHERITANCE_VIOLATION exit 34
**Audited At:** 2026-05-05T21:00:00Z
**Result:** PASS

## New Dependencies

None. story-0077-0023 introduces no new Maven dependencies. The entire
implementation is Bash-only:

- `src/main/resources/targets/claude/hooks/enforce-refinement-gate.sh` — Bash hook (not Java)
- `src/test/bash/enforce_refinement_gate_test.sh` — Bash test
- 9 golden profile files regenerated (text-only, no binary dependencies)

No changes to `pom.xml`. No new `<dependency>` declarations. No NPM, PyPI,
or Go module changes.

## Existing Dependency Versions (unchanged)

| GroupId | ArtifactId | Version | License | CVEs |
| :--- | :--- | :--- | :--- | :--- |
| `info.picocli` | `picocli` | `4.7.6` | Apache-2.0 | None known |

## Runtime Tool Dependencies (Bash hook)

The hook invokes `jq` and `awk` at runtime (external tools, not Maven artifacts):

| Tool | Version requirement | Used for |
| :--- | :--- | :--- |
| `jq` | any | Parse `execution-state.json` |
| `awk` | POSIX | Parse RNF table rows via `IFS='|' read` + string ops |

Both are available in all supported environments. Fail-open contract: `command -v jq || exit 0`
means `jq` absence causes an immediate no-op (exit 0), not a hard dependency failure.

## Verdict

**PASS** — No new Maven or ecosystem dependencies introduced. Existing dependency
versions comply with project policy. No CVEs detected. Bash runtime tools are
already available in all supported environments with fail-open fallback.
