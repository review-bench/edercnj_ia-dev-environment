---
name: x-migrate-frontmatter
description: Migrates artifact frontmatter from v2 to v3.0 (requires-capabilities) using path heuristics, keyword scan, and AI fallback
visibility: public
user-invocable: true
model: sonnet
allowed-tools: [Read, Edit, Write, Bash, Skill]
requires-capabilities: [governance.capability.frontmatter]
---

# Skill: x-migrate-frontmatter

Migrates one file or a directory of markdown artifacts from frontmatter v2 to schema v3.0,
adding `requires-capabilities` via a 4-stage hybrid pipeline.

## Triggers

```
/x-migrate-frontmatter <file>                    — migrate single file
/x-migrate-frontmatter <dir>                     — migrate all .md under dir
/x-migrate-frontmatter <file> --dry-run          — show diff without writing
/x-migrate-frontmatter <file> --force-ai         — skip heuristics, go straight to AI inference
```

## Parameters

| Flag | Type | Default | Description |
|------|------|---------|-------------|
| `target` | Path | — | Positional. File or directory to migrate. |
| `--dry-run` | Boolean | `false` | Show diff; do not write or commit. |
| `--force-ai` | Boolean | `false` | Skip heuristic stages; use AI inference directly. |
| `--commit` | Boolean | `true` | Commit via `x-commit-planning` after writing. |
| `--catalog-root` | Path | `capabilities/` | Catalog root for capability ID lookup. |

## Pipeline (4 stages per file)

### Stage 1 — Path Heuristic

Map file path segments to known capability IDs using the path-to-capability table:

| Path pattern | Inferred capabilities |
|-------------|----------------------|
| `skills/core/dev/x-spring-*` | `[framework.spring-boot.*]` |
| `skills/core/data/*postgres*` | `[data.database.postgres]` |
| `skills/core/dev/*jpa*` | `[data.persistence.jpa]` |
| `skills/core/security/*` | `[security.*]` |
| `rules/conditional/db-*` | `[data.database.*]` |
| `knowledge/testing*` | `[testing.framework.*]` |
| Path has no match | → Stage 2 |

### Stage 2 — Keyword Scan

Scan the file body for technology keywords and map to capabilities:

| Keyword(s) | Capability added |
|-----------|-----------------|
| `postgres`, `PostgreSQL`, `JPA`, `Hibernate` | `data.database.postgres`, `data.persistence.jpa` |
| `MongoDB`, `mongo` | `data.database.mongo` |
| `Redis`, `Lettuce` | `data.cache.redis` |
| `OAuth2`, `JWT`, `OIDC` | `security.authn.oauth2` |
| `Docker`, `Dockerfile` | `deployment.container.docker` |
| `Kafka`, `KafkaListener` | `messaging.kafka.consumer` |
| No keywords match | → Stage 3 |

### Stage 3 — AI Fallback (when stages 1+2 produce no results)

Invoke a `general-purpose` subagent with the file content to propose `requires-capabilities`.

    Agent(
      subagent_type: "general-purpose",
      model: "haiku",
      description: "Infer requires-capabilities for {file}",
      prompt: "Read this skill/rule/agent file and infer which capability IDs from the catalog it requires. Return ONLY a YAML list of capability IDs. If none apply, return []. File path: {path}"
    )

After AI inference: emit `WARNING: AI inference used for {file} — manual review recommended`.

### Stage 4 — Write + Validate

1. Parse existing frontmatter (preserve all existing fields).
2. Add `requires-capabilities: [<inferred IDs>]` (or `[]` if none inferred).
3. Preserve `visibility: internal` and `user-invocable: false` when present (Rule 22).
4. Validate output against `governance/schemas/frontmatter-3.0.json` via `FrontmatterValidator`.
5. Write back to file.
6. When `--commit=true`, commit via:

    Skill(skill: "x-commit-planning", args: "--files <file> --message 'migrate(frontmatter): {file} → v3.0'")

## Output Contract

| Field | Description |
|-------|-------------|
| `filesProcessed` | Count of .md files examined |
| `filesMigrated` | Count successfully migrated |
| `filesFailed` | Count failed (validator rejected output) |
| `warnings` | List of AI-inference warnings |
| `dryRun` | Boolean — whether writes were performed |

## Error Codes

| Code | Condition |
|------|-----------|
| `MIGRATION_FAILED` | Validator rejects output after migration |
| `CATALOG_MISSING` | `--catalog-root` path does not exist |
| `ARGS_INVALID` | Unknown flag or missing target |

## Recovery

If a file fails validation after migration, the original is preserved (backed up to `<file>.v2.bak`).
Re-run with `--force-ai` to use AI inference instead of heuristics.

## Integration Notes

Consumed by stories 0203-0214 (batch migration of 182 artifacts).
`FrontmatterValidator` (story-0064-0201) validates every output before writing.
`x-commit-planning` handles commit if `--commit=true`.
