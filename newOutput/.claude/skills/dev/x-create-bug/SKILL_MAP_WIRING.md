---
requires-capabilities: [governance.bug-lifecycle]
document-type: wiring-note
---

# Wiring: x-create-bug → x-internal-map-bug (story-0080-0003)

After x-internal-decompose-bug runs (Step 7), the skill chain invokes map generation:

```bash
# Step 8 — generate implementation map (wired in story-0080-0003)
Skill(skill: "x-internal-map-bug", model: "haiku", args: "${BUG_ID}")
```

## Updated Response Envelope

```json
{
  "bugId": "bug-000001",
  "slug": "...",
  "bugFile": "ai/bugs/bug-000001/bug.md",
  "severity": "HIGH",
  "scope": "single-module",
  "status": "Pendente",
  "created": "2026-05-07T14:23:15Z",
  "stories": {
    "created": ["ai/bugs/bug-000001/story-01-regression-test.md", "..."],
    "idempotent": false,
    "wallClockMs": 87
  },
  "implementationMap": {
    "mapPath": "ai/bugs/bug-000001/IMPLEMENTATION-MAP.md",
    "storiesAggregated": 2,
    "elapsedMs": 95
  }
}
```

## Complete x-create-bug Step Chain (post-0080-0003)

```
Step 1  — Parse and validate arguments
Step 2  — Generate slug
Step 3  — Assign bug ID
Step 4  — Load template
Step 5  — Instantiate template
Step 6  — Write bug file
Step 7  — [WIRED 0080-0002] x-internal-decompose-bug → story-NN-*.md files
Step 8  — [WIRED 0080-0003] x-internal-map-bug → IMPLEMENTATION-MAP.md
Step 9  — Emit response envelope (with stories + implementationMap)
```
