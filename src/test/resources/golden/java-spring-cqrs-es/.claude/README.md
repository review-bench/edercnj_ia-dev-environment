# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-spring-cqrs** project.
It includes coding rules, skills (slash commands), knowledge packs, agents, and hooks.

> **Note:** The `.claude/` directory is a **generated output** produced by `ia-dev-env`.
> Do not edit it manually -- regenerate instead.

> The `CLAUDE.md` file at the project root provides an executive summary loaded automatically in EVERY conversation.

## Structure

```
CLAUDE.md                   <-- Executive summary (project root, loaded automatically)
.claude/
|-- README.md               <-- You are here
|-- settings.json           <-- Shared settings (committed to git)
|-- settings.local.json     <-- Local overrides (gitignored)
|-- hooks/                  <-- Automations (post-compile, etc.)
|-- rules/                  <-- Project rules (loaded into system prompt)
|-- skills/                 <-- Skills invocable via /command
|   +-- {knowledge-packs}/  <-- Knowledge packs (not invocable, referenced internally)
+-- agents/                 <-- AI personas (used by skills and lifecycle)
```

## Platform Selection

The generator currently produces Claude Code artifacts only. Support for `copilot`, `codex`, and the generic `agents` target was removed (see EPIC-0034). Legacy `--platform` values are rejected by the CLI.

| Value | Description | Directories Generated |
|-------|-------------|-----------------------|
| `claude-code` | Anthropic Claude Code (only accepted value) | `.claude/` + docs |

### CLI Examples

```bash
# Generate Claude Code artifacts (default behavior)
ia-dev-env generate --platform claude-code --config my-config.yaml

# Platform flag can be omitted — claude-code is the only supported target
ia-dev-env generate --config my-config.yaml
```

### YAML Configuration

You can also specify the platform in your YAML config file:

```yaml
platform: claude-code
```

### Default Behavior

When no `--platform` flag is provided and no `platform:` key exists in the YAML config, the generator produces artifacts for `claude-code` (the only supported target). Any legacy value (`copilot`, `codex`, `agents`, `all`) is rejected with a clear error message.

### settings.json vs settings.local.json

- **`settings.json`**: Team settings (permissions, hooks). Committed to git.
- **`settings.local.json`**: Local overrides. In `.gitignore`. Overrides `settings.json`.

---

## Rules

Rules are loaded automatically into the system prompt of EVERY conversation.
They define mandatory standards that Claude MUST follow when generating code.

| # | File | Scope |
|---|------|-------|
| 00 | `00-essentials.md` | essentials |
| 10 | `10-anti-patterns.md` | anti patterns |
| 12 | `12-security-anti-patterns.md` | security anti patterns |

**Total: 3 rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **helidon-scaffold** | `/helidon-scaffold` | Scaffolds a Helidon SE/MP service with routing, health, config, Dockerfile, and tests. |
| **micronaut-scaffold** | `/micronaut-scaffold` | Scaffolds a Micronaut service with @Controller, DI, health, Dockerfile, and tests. |
| **patterns** | `/patterns` |  |
| **picocli-command** | `/picocli-command` | Generates a Picocli @Command with subcommands, options, converters, and unit tests. |
| **quarkus-resource** | `/quarkus-resource` | Generates a Quarkus RESTEasy Reactive @Path resource with DTOs, mapper, and tests. |
| **spring-controller** | `/spring-controller` | Generates a Spring Boot @RestController with DTOs, mappers, advice, and unit tests. |
| **x-analyze-telemetry** | `/x-analyze-telemetry` | Analyzes telemetry NDJSON for an epic; produces Markdown report with Gantt and aggregates. |
| **x-analyze-telemetry-trends** | `/x-analyze-telemetry-trends` | Detects cross-epic P95 regressions and ranks top-10 slowest skills from global index. |
| **x-audit-code** | `/x-audit-code` | Full codebase review against project standards via parallel specialist subagents. |
| **x-audit-dependencies** | `/x-audit-dependencies` | Audits dependencies for CVEs, outdated versions, and license issues per stack. |
| **x-audit-supply-chain** | `/x-audit-supply-chain` | Supply-chain audit: maintainer risk, typosquatting, EPSS, SLSA; SARIF + report. |
| **x-cleanup-git-branches** | `/x-cleanup-git-branches` | Cleans local git: prune, remove non-main worktrees, delete branches except main/develop. |
| **x-commit-changes** | `/x-commit-changes` | Creates Conventional Commits with Task ID and pre-commit chain (format/lint/compile). |
| **x-commit-planning** | `/x-commit-planning` | Batch-commits planning artifacts under plans/** without the code pre-commit chain. |
| **x-create-capability** | `/x-create-capability` | Decompose a Product into Capabilities with explicit RNF inheritance and no-relax markers. |
| **x-create-feature** | `/x-create-feature` | Creates a complete feature (Epic + Stories + Map) from a spec file in an isolated worktree. |
| **x-create-git-branch** | `/x-create-git-branch` | Creates a bare git branch from a configurable base with naming validation and idempotency. |
| **x-create-jira-epic** | `/x-create-jira-epic` | Creates a Jira Epic from a local epic markdown and syncs the Jira key back to the file. |
| **x-create-jira-stories** | `/x-create-jira-stories` | Creates Jira Stories from local story markdowns; links parent Epic and dependencies. |
| **x-create-pr** | `/x-create-pr` | Task-level PR creation with formatted title, labels, structured body, and target branch. |
| **x-create-product** | `/x-create-product` | Transform an ideation file into a Product artifact with RNF roots and C1 capability stub. |
| **x-detect-spec-drift** | `/x-detect-spec-drift` | Detects spec-code drift: contracts, endpoints, Gherkin scenarios vs implementation. |
| **x-drive-tdd** | `/x-drive-tdd` | Executes Red-Green-Refactor TDD cycles for a task; delegates commits to x-commit-changes. |
| **x-epic-create** | `/x-epic-create` | Creates a focused Epic artifact from a Feature markdown, preserving lineage and RNFs. |
| **x-evaluate-hardening** | `/x-evaluate-hardening` | Evaluates hardening posture (headers, TLS, CORS, cookies) with weighted SARIF scoring. |
| **x-evaluate-parallelism** | `/x-evaluate-parallelism` | Detects file-collision risks between parallel work units; emits collision matrix. |
| **x-evaluate-runtime** | `/x-evaluate-runtime` | Evaluates runtime protection (rate limits, WAF, CSP) with SARIF + ASVS scoring. |
| **x-execute-tests** | `/x-execute-tests` | Runs tests with coverage reporting and threshold validation. |
| **x-feature-create** | `/x-feature-create` | Creates a complete feature (Epic + Stories + Map) from a spec file in an isolated worktree. |
| **x-feature-ideate** | `/x-feature-ideate` | Transforms prose into a structured RA9 spec and opens a docs/feature-* PR for review. |
| **x-fix-epic-pr** | `/x-fix-epic-pr` | Discovers all PRs from an epic, classifies comments, applies fixes, opens correction PR. |
| **x-fix-pr** | `/x-fix-pr` | Reads PR review comments and fixes actionable ones with Conventional Commits. |
| **x-format-code** | `/x-format-code` | Formats source code; first step of the pre-commit chain (format -> lint -> compile). |
| **x-generate-adr** | `/x-generate-adr` | Generates ADRs from architecture-plan mini-ADRs with sequential numbering and index update. |
| **x-generate-ci** | `/x-generate-ci` | Generates or updates CI/CD pipelines per project stack with actionlint validation. |
| **x-generate-docs** | `/x-generate-docs` | Documentation automation v2: stack-aware generation from documentation.targets. |
| **x-generate-release-changelog** | `/x-generate-release-changelog` | Hybrid changelog: narrative Highlights + Keep-a-Changelog sections from v2 epics. |
| **x-generate-security-dashboard** | `/x-generate-security-dashboard` | Aggregates results of all security scanning skills into a unified posture dashboard. |
| **x-generate-security-pipeline** | `/x-generate-security-pipeline` | Generates CI/CD pipelines with conditional security stages from SecurityConfig flags. |
| **x-handle-incident** | `/x-handle-incident` | Guides SEV1-SEV4 incident response with checklists, comms templates, and postmortems. |
| **x-ideate-feature** | `/x-ideate-feature` | Transforms prose into a structured RA9 spec and opens a docs/feature-* PR for review. |
| **x-implement-epic** | `/x-implement-epic` | Drives an epic end-to-end via 6 phases: plan, branch, story loop, integrity gate, final PR. |
| **x-implement-story** | `/x-implement-story` | Drives a story end-to-end via 4 phases: plan, task loop, verify, report. |
| **x-implement-task** | `/x-implement-task` | Implements a task via TDD Red-Green-Refactor with one atomic commit via x-commit-changes. |
| **x-lint-code** | `/x-lint-code` | Lints source code with the appropriate linter; second step in the pre-commit chain. |
| **x-lint-contract-tests** | `/x-lint-contract-tests` | Validates API contracts (OpenAPI, AsyncAPI, Protobuf) against their specifications. |
| **x-manage-pr-merge-train** | `/x-manage-pr-merge-train` | Merge-train automation: discovers, validates, and merges a sequence of PRs into develop. |
| **x-manage-worktrees** | `/x-manage-worktrees` | Manages git worktrees for parallel task/story execution under .claude/worktrees/. |
| **x-merge-branches** | `/x-merge-branches` | Merges source into target locally with strategy, conflict rollback, and idempotent no-op. |
| **x-merge-pr** | `/x-merge-pr` | Merges a single PR via gh CLI with configurable strategy and idempotent behavior. |
| **x-migrate-templates** | `/x-migrate-templates` | Migrates a v1 epic to v2 value-driven template (EPIC-0070), with optional --interactive. |
| **x-model-threats** | `/x-model-threats` | Generates STRIDE threat models: components, data flows, threats, severity, mitigations. |
| **x-orchestrate-epic** | `/x-orchestrate-epic` | Orchestrates multi-agent planning for all stories in an epic with checkpoint and resume. |
| **x-plan-arch-capability** | `/x-plan-arch-capability` | Generate C4 Container + Component diagrams for a capability in Mermaid or PlantUML. |
| **x-plan-arch-feature** | `/x-plan-arch-feature` | Generate C4 Context + Container diagrams for a feature in Mermaid or PlantUML. |
| **x-plan-arch-product** | `/x-plan-arch-product` | Generate C4 Context + Container + Component diagrams for a product in Mermaid or PlantUML. |
| **x-plan-architecture** | `/x-plan-architecture` | Generates an architecture plan with C4 diagrams, mini-ADRs, NFRs, and resilience strategy. |
| **x-plan-story** | `/x-plan-story` | Multi-agent story planning: 7 specialized agents produce task breakdown and plans. |
| **x-plan-task** | `/x-plan-task` | Generates plan-task-*.md with TDD cycles in TPP order, file-impact, and exit criteria. |
| **x-plan-tests** | `/x-plan-tests` | Generates a Double-Loop TDD test plan with TPP-ordered acceptance and unit scenarios. |
| **x-profile-performance** | `/x-profile-performance` | Automated profiling: detects runtime, runs profiler, generates flamegraph and hotspots. |
| **x-promote-ideation** | `/x-promote-ideation` | Promote a transient x-ideate-feature output to a persistent ideation artifact in ai/ideations/. |
| **x-push-branch** | `/x-push-branch` | Git workflow: branch, atomic Conventional Commits, push, and PR creation. |
| **x-recommend-mcp** | `/x-recommend-mcp` | Analyzes project tech stack and recommends relevant MCP servers from a built-in catalog. |
| **x-reconcile-status** | `/x-reconcile-status` | Reconciles execution-state.json telemetry against Status field of Epic/Story markdowns. |
| **x-refine-epic** | `/x-refine-epic` | Multi-persona 4-phase strategic epic refinement: parallel specialists + verdict. |
| **x-refine-story** | `/x-refine-story` | Multi-persona 4-phase story refinement: parallel specialists + verdict. |
| **x-release** | `/x-release` | Orchestrates Git Flow release: version bump, validation, PR, tag, back-merge. |
| **x-review-api** | `/x-review-api` | Validates REST endpoints: RFC 7807, pagination, versioning, OpenAPI, status codes, DTOs. |
| **x-review-codebase** | `/x-review-codebase` | Parallel code review with specialist engineers (Security, QA, Perf, DB, Obs, DevOps). |
| **x-review-devops** | `/x-review-devops` | DevOps specialist review: Dockerfile, CI/CD, resource limits, health probes, deploy. |
| **x-review-events** | `/x-review-events` | Reviews event schemas, producer/consumer patterns, error handling, and DLQ readiness. |
| **x-review-performance** | `/x-review-performance` | Performance review: N+1, pools, async, pagination, cache, timeouts, circuit breakers. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 45-point checklist; produces GO/NO-GO verdict. |
| **x-review-qa** | `/x-review-qa` | QA review: coverage, TDD compliance, naming, fixtures, parametrized tests, AC coverage. |
| **x-run-dynamic-pentest** | `/x-run-dynamic-pentest` | DAST gate: ZAP + Nuclei (passive/active); SARIF + Markdown; blocks on HIGH/CRITICAL. |
| **x-run-perf-tests** | `/x-run-perf-tests` | Runs performance tests for latency SLAs, throughput, and resource stability under load. |
| **x-scan-owasp** | `/x-scan-owasp` | Automated OWASP Top 10 (2021) verification mapped to ASVS L1/L2/L3 with SARIF output. |
| **x-search-memory** | `/x-search-memory` | Queries ai/memory/ for decisions, patterns, and anti-patterns across past epics |
| **x-setup-env** | `/x-setup-env` | Validates and configures local dev environment: stack detection, deps, IDE, build. |
| **x-story-create** | `/x-story-create` | Creates focused Story artifacts from a Feature markdown with epic linkage and RNFs. |
| **x-troubleshoot-operations** | `/x-troubleshoot-operations` | Diagnoses errors and failures: reproduce, locate, understand, fix, verify. |
| **x-update-architecture** | `/x-update-architecture` | Incrementally updates the service architecture document with changes from arch plans. |
| **x-update-system-architecture** | `/x-update-system-architecture` | Incrementally updates docs/architecture/system.md after an epic completes. |
| **x-validate-docs** | `/x-validate-docs` | Documentation freshness gate: validates 6 dimensions (readme, api, adr, etc.) per PR. |
| **x-watch-pr-ci** | `/x-watch-pr-ci` | Polls a PR's CI checks and Copilot review until completion or timeout (8 exit codes). |

**Total: 110 skills**

### Usage Examples

```bash
# Run a specific skill
/skill-name argument

# Get help on available skills
# Type / in the chat to see the full list
```

---

## Knowledge Packs (Internal Context)

Knowledge Packs do NOT appear in the `/` menu. They are referenced internally by agents and skills
to inject domain knowledge. Configured with `user-invocable: false`.

| Pack | Usage |
|------|-------|
| `planning-standards-kp` | Referenced internally by agents |
| `x-internal-build-epic-plan` | Referenced internally by agents |
| `x-internal-build-story-plan` | Referenced internally by agents |
| `x-internal-create-epic` | Referenced internally by agents |
| `x-internal-create-story` | Referenced internally by agents |
| `x-internal-decompose-bug` | Referenced internally by agents |
| `x-internal-ensure-epic-branch` | Referenced internally by agents |
| `x-internal-load-story-context` | Referenced internally by agents |
| `x-internal-map-bug` | Referenced internally by agents |
| `x-internal-map-epic` | Referenced internally by agents |
| `x-internal-normalize-args` | Referenced internally by agents |
| `x-internal-pr-body-render` | Referenced internally by agents |
| `x-internal-precheck-worktree` | Referenced internally by agents |
| `x-internal-render-pr-body` | Referenced internally by agents |
| `x-internal-resume-story` | Referenced internally by agents |
| `x-internal-summarize-epic` | Referenced internally by agents |
| `x-internal-update-status` | Referenced internally by agents |
| `x-internal-validate-rnf` | Referenced internally by agents |
| `x-internal-verify-epic-integrity` | Referenced internally by agents |
| `x-internal-verify-phase-gates` | Referenced internally by agents |
| `x-internal-verify-story` | Referenced internally by agents |
| `x-internal-write-report` | Referenced internally by agents |
| `x-internal-write-story-report` | Referenced internally by agents |
| `x-migrate-frontmatter` | Referenced internally by agents |

---

## Agents (AI Personas)

Agents are system prompts that define specialized personas. They are not invoked directly --
they are used by skills (via Task tool) to delegate work to agents with specific expertise.

| Agent | File |
|-------|------|
| **api-engineer** | `api-engineer.md` |
| **architect** | `architect.md` |
| **devops-engineer** | `devops-engineer.md` |
| **event-engineer** | `event-engineer.md` |
| **java-developer** | `java-developer.md` |
| **pentest-engineer** | `pentest-engineer.md` |
| **performance-engineer** | `performance-engineer.md` |
| **product-owner** | `product-owner.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **sre-engineer** | `sre-engineer.md` |
| **tech-lead** | `tech-lead.md` |

**Total: 12 agents**

---

## Hooks (Automations)

Hooks are scripts executed automatically in response to Claude Code events.
Configured in `settings.json` under the `hooks` key.

### Post-Compile Check

- **Event:** `PostToolUse` (after `Write` or `Edit`)
- **Script:** `.claude/hooks/post-compile-check.sh`
- **Behavior:** When a `.java` file is modified, runs `./gradlew compileJava -q` automatically
- **Purpose:** Catch compilation errors immediately after file changes

---

## Telemetry

Skill executions are captured as NDJSON under `plans/epic-*/telemetry/events.ndjson`, giving operators an auditable timeline of phase durations, subagent lifecycles, and tool calls. The architecture is documented in [ADR-0005 — Telemetry Architecture](../adr/ADR-0005-telemetry-architecture.md); the privacy contract lives in [Rule 20 — Telemetry Privacy](rules/20-telemetry-privacy.md).

Capture happens on two layers:

- **Hook-based (automatic).** Bash scripts under `hooks/` fire on `SessionStart`, `PreToolUse`, `PostToolUse`, `SubagentStop`, and `Stop`. Registration is handled by `SettingsAssembler`.
- **In-skill phase markers.** Instrumented skills call `telemetry-phase.sh start|end` around each numbered phase. The authoring template `_TEMPLATE-SKILL.md` includes a "Telemetry (Optional)" section ready to copy.

Analysis skills:

```bash
# Point-in-time report (aggregates + Mermaid Gantt)
/x-telemetry-analyze --epic EPIC-0040

# Cross-epic P95 regression detector
/x-telemetry-trend --last 5 --threshold-pct 20
```

Opt out with `CLAUDE_TELEMETRY_DISABLED=1` (per-session) or by adding the nested YAML block below to the generator YAML (per-project, requires regeneration):

```yaml
telemetry:
  enabled: false
```

EPIC-0040 shipped this stack — see the [CHANGELOG](../CHANGELOG.md) for the release it landed in.

---

## Settings

### settings.json

Permissions are configured in `settings.json` under `permissions.allow`.
This controls which Bash commands Claude Code can run without asking.

### settings.local.json

Local overrides (gitignored). Use for personal preferences or team-specific tools.

See the files directly for current configuration.

---

## Artifact Conventions

| Artifact | Extension | Naming | Frontmatter |
|----------|-----------|--------|-------------|
| Rules | `.md` | `NN-name.md` (numbered) | None |
| Skills | `SKILL.md` | `skills/{name}/SKILL.md` | YAML (name, description) |
| Agents | `.md` | `{name}.md` | None |
| Hooks | `.sh` / `.json` | Event-based naming | N/A |

---

## Tips

- **Rules are always active** -- no need to invoke them, Claude already knows them.
- **Skills are lazy** -- they only load when you type `/name`.
- **Knowledge Packs do not appear in the `/` menu** -- they are internal context for agents.
- **Agents are not invoked directly** -- they are used by skills internally.
- **Hooks run automatically** -- compilation after editing source files detects errors early.
- **To create a new skill**: create `.claude/skills/{name}/SKILL.md` and it appears automatically.
- **To create a new rule**: add a `.md` file in `.claude/rules/` with the appropriate numbering.
- **Both directories are generated** -- run `ia-dev-env generate` to regenerate.

---

## References

- **Audit Gates Catalog:** [`docs/audit-gates-catalog.md`](../docs/audit-gates-catalog.md) — canonical index of all governance gates (Hook runtime / CI script / Java test / Workflow) with exit codes, layer, and cross-refs. Maintained per RULE-004 (Catalog-before-Add, Rule 26).

---

## Generation Summary

| Component | Count |
|-----------|-------|
| Rules (.claude) | 3 |
| Skills (.claude) | 86 |
| Knowledge Packs (.claude) | 24 |
| Agents (.claude) | 12 |
| Hooks (.claude) | 17 |
| Settings (.claude) | 2 |
| Plan Templates (.claude) | 29 |

Generated by `ia-dev-env v0.1.0`.
