---
requires-capabilities: [governance.bug-lifecycle]
document-type: wiring-note
---

# Wiring: x-create-bug → x-internal-decompose-bug

**Story:** story-0080-0002, task-0080-0002-003

## Integration Point

After Step 6 (write bug file) in `x-create-bug`, the skill chain invokes the decomposition sub-skill:

```bash
# Step 7 — invoke decomposition (wired in story-0080-0002)
Skill(skill: "x-internal-decompose-bug", model: "haiku", args: "${BUG_ID}")
```

This call is placed AFTER the scaffold commit so the bug file exists on disk when the
sub-skill reads it. The decomposition sub-skill produces 2-4 story files in `ai/bugs/${BUG_ID}/`.

## Updated Response Envelope

When decomposition is enabled, the x-create-bug response includes a `stories` field:

```json
{
  "bugId": "bug-000001",
  "slug": "login-endpoint-returns-500-on-invalid-credentials",
  "bugFile": "ai/bugs/bug-000001/bug.md",
  "severity": "HIGH",
  "scope": "STANDARD",
  "status": "Pendente",
  "created": "2026-05-07T14:23:15Z",
  "stories": {
    "created": [
      "ai/bugs/bug-000001/story-01-regression-test.md",
      "ai/bugs/bug-000001/story-02-fix.md"
    ],
    "idempotent": false,
    "wallClockMs": 87
  }
}
```

## Error Handling

If `x-internal-decompose-bug` returns non-zero:
- Log a WARNING to stderr (bug file is already created successfully)
- Include `storiesError` in the response envelope with the exit code
- Do NOT fail the bug creation (decomposition failure is non-fatal at this layer)

## Updated x-create-bug Workflow Summary

```
Step 1  — Parse and validate arguments
Step 2  — Generate slug
Step 3  — Assign bug ID
Step 4  — Load template
Step 5  — Instantiate template
Step 6  — Write bug file + git commit scaffold
Step 7  — [WIRED] Invoke x-internal-decompose-bug <BUG_ID>
Step 8  — Emit response envelope (including stories field)
```
