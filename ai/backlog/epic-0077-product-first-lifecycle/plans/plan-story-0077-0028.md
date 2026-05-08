# Implementation Plan — story-0077-0028

**Story:** Rule 19 normativa flowVersion 5 + ADR consolidando + Epic0077ProductFirstSmokeIT E2E
**Status:** Concluída
**Planned at:** 2026-05-05T19:45:00Z

## 1. Scope

Finalizes EPIC-0077 normative layer:
- Rule 19 updated with `flowVersion: "5"` and `productFirstLifecycle` field fallback matrix
- Rule 22 updated with x-internal-rnf-validate skill reference
- ADR-0032 published consolidating all Product-First Lifecycle decisions
- `docs/adr/README.md` updated with ADR-0032 entry
- `x-internal-rnf-validate` internal skill (new)
- `RNFOverrideArtifactParser` — parses RNF override markdown tables
- `RNFOverride` domain value object
- `Epic0077ProductFirstSmokeIT` — E2E smoke test for the entire Product-First lifecycle
- Golden files (9 profiles × README.md + rules/19 + rules/22 + x-internal-rnf-validate)

## 2. File Footprint

**write:**
- `src/main/resources/targets/claude/rules/19-backward-compatibility.md`
- `src/main/resources/targets/claude/rules/22-skill-visibility.md`
- `src/main/resources/targets/claude/skills/core/internal/ops/x-internal-rnf-validate/SKILL.md`
- `src/main/java/dev/iadev/adapter/inbound/cli/RNFOverrideArtifactParser.java`
- `src/main/java/dev/iadev/domain/capability/RNFOverride.java`
- `src/test/java/dev/iadev/adapter/inbound/cli/RNFOverrideArtifactParserTest.java`
- `src/test/java/dev/iadev/smoke/Epic0077ProductFirstSmokeIT.java`
- `docs/adr/ADR-0032-product-first-lifecycle-finalization.md`
- `docs/adr/README.md`
- Golden files (9 profiles × 4 files)

## 3. Architecture

- `flowVersion: "5"` entry in Rule 19 fallback matrix
- `productFirstLifecycle` field fallback matrix in Rule 19
- `x-internal-rnf-validate` internal skill for RNF inheritance validation
- `RNFOverride` domain value object for override table entries
- `RNFOverrideArtifactParser` reads override tables from story markdown

## 4. AC Coverage

| AC | Result |
|---|---|
| Rule 19 documents flowVersion=5 fallback matrix | PASS |
| Rule 19 documents productFirstLifecycle field | PASS |
| Rule 22 references x-internal-rnf-validate | PASS |
| ADR-0032 published with full rationale | PASS |
| Epic0077ProductFirstSmokeIT validates end-to-end flow | PASS |
| Golden files updated (9 profiles) | PASS |
