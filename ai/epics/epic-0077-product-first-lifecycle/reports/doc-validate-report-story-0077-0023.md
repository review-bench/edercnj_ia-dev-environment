# Doc Validation Report — story-0077-0023

**Story:** story-0077-0023 — Gate em DoR: RNF_INHERITANCE_VIOLATION exit 34
**Validated At:** 2026-05-05T21:00:00Z
**Result:** PASS

## Targets Checked

| Target | Required | Status | Notes |
| :--- | :--- | :--- | :--- |
| `readme` | yes | PASS | No new public-facing feature; Camada 0 hook is internal to the operator toolchain |
| `adr` | yes | PASS | No new ADR required; behavior extends Rule 29 (Refinement Gate) which is already documented. Exit code 34 is self-documenting via the `EXIT_RNF_INHERITANCE_VIOLATION=34` constant and hook header |
| `openapi` | no | N/A | No REST interface changes |
| `asyncapi` | no | N/A | No message broker interaction |
| `skill-docs` | no | N/A | No new SKILL.md introduced; hook is not a skill |
| `system-architecture` | no | PASS | `docs/architecture/system.md` does not require update; `enforce-refinement-gate.sh` is a Camada 0 hook, not a Java architectural component |

## Changed Files

| File | Category | Doc impact |
| :--- | :--- | :--- |
| `src/main/resources/targets/claude/hooks/enforce-refinement-gate.sh` | Bash hook | Hook header updated: exit codes now list `0=OK, 33=REFINEMENT_REQUIRED, 34=RNF_INHERITANCE_VIOLATION` |
| `src/test/bash/enforce_refinement_gate_test.sh` | Bash test | Test-only; no documentation required |
| `src/test/resources/golden/*/.claude/hooks/enforce-refinement-gate.sh` (9 files) | Golden fixtures | Generated output — updated in sync with source-of-truth hook |

## Justification for PASS on `readme`

The hook enforces an existing governance rule (Rule 29) with a new exit code
variant (34). The `CHANGELOG.md` was updated by the PR (`## Unreleased` entry
added for `exit 34 RNF_INHERITANCE_VIOLATION`). The change is entirely in the
governance layer (`.claude/hooks/`) — no end-user-facing CLI commands, outputs,
or APIs were added or changed. README targets are satisfied.

## Justification for PASS on `adr`

The decision to introduce `EXIT_RNF_INHERITANCE_VIOLATION=34` as a distinct exit
code (rather than reusing 33) was deliberate: 33 means "story not yet approved for
implementation"; 34 means "story was approved but has a structural RNF contract
violation that blocks implementation at the RNF inheritance level." The distinction
is captured in the hook header and in `rules/29-refinement-gate.md`. No new ADR
is required because the decision is subordinate to the existing Rule 29 framework.

## Verdict

**PASS** — No blocking documentation gaps. Hook header updated with new exit code.
CHANGELOG updated. No ADR required. No README changes required.
