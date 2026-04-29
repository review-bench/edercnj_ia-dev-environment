<!-- Returns to [slim body](../SKILL.md) after reading. -->

# x-review — Composition Architecture (RULE-007 — EPIC-0064)

`x-review` is the **canonical composite skill** — it uses fragment-slot composition to include only the specialists relevant to the project's active capabilities.

**Canonical 8 fragments** (under `x-review/fragments/`):

| Fragment | Requires | Order |
|----------|----------|-------|
| `qa.md` | `[]` — universal | 10 |
| `perf.md` | `[]` — universal | 20 |
| `security.md` | `[]` — universal | 30 |
| `devops.md` | `infra.docker.*` OR `infra.cicd.*` | 40 |
| `db.md` | `data.database.*` | 50 |
| `api.md` | any `web.*` framework | 60 |
| `event.md` | any `messaging.*` | 70 |
| `compliance.md` | `compliance.*` | 80 |

**Contributing a new specialist fragment:**

1. Create `x-review/fragments/<name>.md` with frontmatter:
   ```yaml
   ---
   name: x-review-fragment-<name>
   fragment-slot: { slot: review-specialist, fragment-id: <name>, fragment-order: <N> }
   requires-capabilities: [your.capability.id]
   ---
   ```
2. Body: describe the specialist's scope, max score, and what it reviews.
3. Run `Epic0064ReviewCompositionSmokeTest` to verify ordering invariants.
