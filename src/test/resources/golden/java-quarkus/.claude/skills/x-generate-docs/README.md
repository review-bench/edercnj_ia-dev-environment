# x-generate-docs

> Documentation automation: detects documentation type needed (API, README, ADR, changelog) from code changes, delegates to specialized skills or generates inline. Single entry point for all documentation updates.

| | |
|---|---|
| **Category** | Documentation |
| **Invocation** | `/x-generate-docs [--type api\|readme\|adr\|changelog\|all] [--scope path] [--force]` |

> **Spec**: See [SKILL.md](./SKILL.md) for the complete execution specification.

## What It Does

Serves as the single entry point for all documentation generation and updates. Analyzes code changes via `git diff` to auto-detect which documentation types need updating, then delegates to specialized skills (`/x-generate-release-changelog`, `/x-generate-adr`, `/x-update-architecture`) or generates API docs and README updates inline. Ensures documentation stays in sync with code changes.

## Usage

```
/x-generate-docs
/x-generate-docs --type api
/x-generate-docs --type readme
/x-generate-docs --type changelog
/x-generate-docs --type adr
/x-generate-docs --type all
/x-generate-docs --type all --force
/x-generate-docs --type api --scope src/main/java/com/example/api/
```

## Flags

| Flag | Description |
|------|-------------|
| `--type` | Documentation type to generate: `api`, `readme`, `adr`, `changelog`, `all`. Omit for auto-detection. |
| `--scope` | Limit change analysis to a specific path |
| `--force` | Regenerate documentation even if no changes detected |

## Auto-Detection

When `--type` is omitted, the skill analyzes `git diff` to infer which documentation types need updating:

| Changed File Pattern | Inferred Type |
|---------------------|---------------|
| `*Controller*`, `*Resource*`, `*Handler*`, `*Endpoint*` | `api` |
| `*ADR*`, `*Decision*`, `architecture*` | `adr` |
| Any commits since last tag | `changelog` |
| `SKILL.md`, `README*`, `config*`, `setup*` | `readme` |

## Delegation

| Type | Delegated To | Method |
|------|-------------|--------|
| `changelog` | `/x-generate-release-changelog` | Skill tool invocation |
| `adr` | `/x-generate-adr` | Skill tool invocation |
| Architecture | `/x-update-architecture` | Skill tool invocation |
| `api` | Inline | Direct generation |
| `readme` | Inline | Direct generation |

## Workflow

1. **Parse** -- Parse arguments (`--type`, `--scope`, `--force`)
2. **Detect** -- Analyze `git diff` to determine documentation types needed
3. **Dispatch** -- Delegate to specialized skills or generate inline
4. **Verify** -- Confirm updates are idempotent (no duplicate content)
5. **Report** -- Summary of documentation actions taken

## Outputs

| Type | Artifact | Path |
|------|----------|------|
| `api` | API documentation | `docs/api/openapi.yaml` or API section in README |
| `readme` | Project README | `README.md` |
| `adr` | Architecture Decision Records | `docs/adr/NNNN-*.md` |
| `changelog` | Changelog | `CHANGELOG.md` |
| Architecture | Architecture document | `steering/service-architecture.md` |

## See Also

- [x-generate-release-changelog](../x-generate-release-changelog/) -- Changelog generation from Conventional Commits
- [x-generate-adr](../x-generate-adr/) -- ADR generation from architecture plans
- [x-update-architecture](../x-update-architecture/) -- Architecture document updates
