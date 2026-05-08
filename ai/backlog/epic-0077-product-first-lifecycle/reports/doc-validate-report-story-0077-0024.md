# Doc Validation Report — story-0077-0024

**Story:** story-0077-0024 — x-epic-create --from-feature (v5 obrigatório); drops Sections 2/4/8
**Validated At:** 2026-05-05T22:00:00Z
**Result:** PASS

## Targets Checked

| Target | Required | Status | Notes |
| :--- | :--- | :--- | :--- |
| `readme` | yes | PASS | No change to CLI user-facing README; `x-epic-create` skill is new and self-documented via SKILL.md |
| `adr` | yes | PASS | No new ADR required; design decisions (section drop, RNF chain) are documented in the techlead review and story plan |
| `openapi` | no | N/A | No REST interface changes |
| `asyncapi` | no | N/A | No message broker interaction |
| `skill-docs` | yes | PASS | `x-epic-create/SKILL.md` updated with `--from-feature` flag, argument-hint, and RNF chain documentation; `x-internal-create-epic/SKILL.md` updated accordingly |
| `system-architecture` | no | PASS | `docs/architecture/system.md` does not require update; `EpicFromFeatureArtifactWriter` is an I/O adapter, not a new architectural pattern |

## Changed Files

| File | Category | Doc impact |
| :--- | :--- | :--- |
| `src/main/resources/targets/claude/skills/core/plan/x-epic-create/SKILL.md` | Skill documentation | Updated with `--from-feature` flag and RNF chain behavior |
| `src/main/resources/targets/claude/skills/core/internal/plan/x-internal-create-epic/SKILL.md` | Internal skill documentation | Updated to reflect `--from-feature` delegation and section-drop contract |
| `src/test/resources/golden/*/skills/x-epic-create/SKILL.md` (9 profiles) | Generated golden fixtures | Updated in sync with source-of-truth SKILL.md |
| `src/test/resources/golden/java-spring/platform-claude-code/.claude/skills/x-epic-create/SKILL.md` | Platform golden | Added (new skill not previously in platform profile) |

## Justification for PASS on `readme`

`x-epic-create` is a new public skill added by this story. Its `SKILL.md` contains
the `## Triggers`, `## Examples`, and `## Argument Reference` sections required by
Rule 22. The `CHANGELOG.md` was updated with an `## Unreleased` entry for
`feat: x-epic-create --from-feature`. No change to any end-user README was required
because the skill-catalog README is regenerated from SKILL.md files (via `GoldenFileTest`).

## Justification for PASS on `adr`

The section-drop design decision (dropping Sections 2/4/8 when using `--from-feature`)
is a product-level design choice consistent with the Product-First Lifecycle hierarchy
established by EPIC-0077. It does not introduce a new architectural pattern, overriding
framework, or cross-cutting concern that would warrant an ADR. The decision is captured
in the story plan and techlead review artifact.

## Justification for PASS on `skill-docs`

Both modified SKILL.md files pass the completeness check:
- `## Triggers` block present and updated with `--from-feature` trigger conditions
- `## Examples` block present with at least one realistic invocation example
- Argument-hint updated to reflect new `--from-feature <feature-file|feature-id>` parameter
- RNF chain behavior (feature → capability → product) documented inline

**Result: PASS** — All applicable documentation targets are current and accurate.
