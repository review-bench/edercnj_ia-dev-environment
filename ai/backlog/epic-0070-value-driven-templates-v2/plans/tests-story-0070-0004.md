# Test Plan — story-0070-0004

## AC Scenarios

### TC-001 — Happy: 11 sections with auto-fill
Verify `_TEMPLATE-ARCHITECTURE-SYSTEM.md` contains all 11 section headings:
```
grep -E "^## [0-9]+\." template | wc -l  → 11
```
Verify auto-fill placeholders present: `{{language_name}}`, `{{framework_name}}`, `{{database_name}}`, `{{message_broker}}`, `{{compliance}}`

### TC-002 — Degenerate: stateless project (no DB/cache/broker)
When context has `database_name=none`, `cache_name=none`, `message_broker=none`:
- `assembleSystemArchitecture()` renders "§2 Persistência" with "(não aplicável — projeto stateless)" text
- No TEMPLATE_PLACEHOLDER_UNRESOLVED error
- Output file created without warning

### TC-003 — Error: unresolved placeholder
If template contains `{{UNKNOWN_KEY}}` and key is absent from ContextBuilder context:
- TemplateEngine throws/reports `TEMPLATE_PLACEHOLDER_UNRESOLVED` indicating the key
- Output is NOT written (fail-fast before write)

### TC-004 — Boundary: golden file matches java-spring profile
Run `assembleSystemArchitecture()` with java-spring config (Java 21, Spring 3.x, postgres, flyway, maven, kafka):
- Output bytes match `src/test/resources/golden/java-spring/docs/architecture/system.md` exactly
- GoldenFileTest covering the `java-spring/` tree passes without harness changes

### TC-005 — Performance/SLA: generation within 2s
`assembleSystemArchitecture()` on java-spring config completes in ≤ 2000ms measured via stopwatch
Output file has ≤ 800 lines

### TC-006 — Security: no path traversal, no shell eval
Template rendering uses only the ContextBuilder context map — no raw YAML value injection
Path written is always `docs/architecture/system.md` under `outputDir`; no file outside `outputDir` created

### TC-007 — Frontmatter v3.0 present
Head of `_TEMPLATE-ARCHITECTURE-SYSTEM.md` shows `requires-capabilities` and `template-version` fields

### TC-008 — HTML comment documents auto-fill vs narrative
Template contains HTML comment `<!-- AUTO-FILL ... NARRATIVE ... -->` before section 1

### TC-009 — Idempotent: existing file not overwritten
When `docs/architecture/system.md` already exists, calling `assembleSystemArchitecture()` again does not overwrite it
