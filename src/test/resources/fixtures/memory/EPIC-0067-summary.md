---
epic-id: EPIC-0067
slug: review-yaml-frontmatter
summary-version: "1.0"
created: "2026-04-29"
last-updated: "2026-04-29"

indexable: true
archived: false
superseded-by: null

tags: [governance, templates, documentation, refactor]
capabilities-affected: []
rules-affected: [Rule 28]
adrs-referenced: []

patterns-introduced:
  - kp-frontmatter-name-description-only
  - skill-frontmatter-strict-fields
antipatterns-rejected:
  - skill-fields-in-kp-frontmatter

dependencies-of: [EPIC-0064]
dependencies-for: []
---
# Memory: EPIC-0067 — Review YAML Frontmatter

## Why this epic existed

After EPIC-0064 introduced schema v3.0 frontmatter, several knowledge-pack (KP) `index.md` files
were found to carry skill-specific fields (`visibility: internal`, `user-invocable: false`) that are
only valid for SKILL.md files. The `KnowledgeAssembler` pipeline rejected them with a
`"declares skill-only field"` error, causing generation failures.

EPIC-0067 audited all KP `index.md` files and skill `SKILL.md` files, removed the misplaced fields,
and documented the canonical frontmatter shape for each artifact type.

## Hypothesis tested

**Hypothesis:** a targeted audit of KP frontmatter to remove skill-only fields would unblock
generation without introducing any behavioral regression in generated `.claude/` output.

**Result:** confirmed. All KP `index.md` files cleaned; `KnowledgeAssemblerTest` and CLI integration
tests passed; no golden-file diffs.

## Decisions taken (with why)

- **KP frontmatter limited to `name`, `description`, `requires-capabilities`** — KPs are not
  user-invocable; `visibility` and `user-invocable` add no semantic value and the assembler
  explicitly rejects them to prevent confusion.
- **Validation error over silent ignore** — the assembler throws `ConfigValidationException` on
  unknown skill fields in KP context; silent ignore would hide future authoring mistakes.

## Alternatives rejected (with why)

- **Allow skill fields in KP frontmatter (ignore them)** — would mask authoring errors where a KP
  was accidentally written using the SKILL.md template; the strict rejection surfaces the mistake
  immediately.

## Reusable patterns produced

- `kp-frontmatter-name-description-only` — KP index.md files declare only `name`, `description`,
  and `requires-capabilities`; no visibility or invocability fields.
- `skill-frontmatter-strict-fields` — SKILL.md files must declare `visibility` and
  `user-invocable`; KP files must not.

## Anti-patterns observed

- `skill-fields-in-kp-frontmatter` — using the SKILL.md frontmatter template for a KP
  `index.md`; causes `KnowledgeAssembler` to reject the artifact at generation time.

## Links

- Epic: `ai/epics/epic-0067-review-yaml-frontmatter/epic-0067.md`
- ADRs: none
- PRs: epic/0067 → develop
- Reports: `ai/epics/epic-0067-review-yaml-frontmatter/reports/`
