# .claude/ -- Usage Guide

This directory contains all Claude Code configuration for the **my-spring-event-driven** project.
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
| 01 | `01-project-identity.md` | project identity |
| 02 | `02-domain.md` | domain |
| 03 | `03-coding-standards.md` | coding standards |
| 04 | `04-architecture-summary.md` | architecture summary |
| 05 | `05-quality-gates.md` | quality gates |
| 06 | `06-security-baseline.md` | security baseline |
| 07 | `07-operations-baseline.md` | operations baseline |
| 08 | `08-release-process.md` | release process |
| 09 | `09-branching-model.md` | branching model |
| 10 | `10-anti-patterns.md` | anti patterns |
| 12 | `12-security-anti-patterns.md` | security anti patterns |
| 13 | `13-skill-invocation-protocol.md` | skill invocation protocol |
| 14 | `14-project-scope.md` | project scope |
| 19 | `19-backward-compatibility.md` | backward compatibility |
| 20 | `20-interactive-gates.md` | interactive gates |
| 21 | `21-epic-branch-model.md` | epic branch model |
| 22 | `22-skill-visibility.md` | skill visibility |
| 23 | `23-model-selection.md` | model selection |
| 24 | `24-execution-integrity.md` | execution integrity |
| 25 | `25-task-hierarchy.md` | task hierarchy |
| 26 | `26-audit-gate-lifecycle.md` | audit gate lifecycle |
| 27 | `27-zero-bypass-lifecycle.md` | zero bypass lifecycle |
| 28 | `28-capability-frontmatter-contract.md` | capability frontmatter contract |
| 28 | `28-tool-call-grammar.md` | tool call grammar |
| 29 | `29-refinement-gate.md` | refinement gate |
| 30 | `30-value-driven-templates.md` | value driven templates |
| 31 | `31-documentation-freshness-gate.md` | documentation freshness gate |
| 32 | `32-dependency-policy-gate.md` | dependency policy gate |
| 33 | `33-ai-memory-production.md` | ai memory production |
| 45 | `45-ci-watch-integrity.md` | ci watch integrity |

**Total: 30 rules**

### Numbering

- Gaps in numbering allow future insertion without renumbering existing rules.

---

## Skills (Slash Commands)

Skills are invoked by the user via `/name` in chat. They are lazy-loaded (only load when invoked).

| Skill | Path | Description |
|-------|------|-------------|
| **helidon-scaffold** | `/helidon-scaffold` | Scaffold a Helidon SE or MP service with routing, health checks, config, Dockerfile, and integration tests. |
| **micronaut-scaffold** | `/micronaut-scaffold` | Scaffold a Micronaut service with @Controller, @Singleton DI, health indicators, compile-time DI, Dockerfile, and integration tests. |
| **patterns** | `/patterns` |  |
| **picocli-command** | `/picocli-command` | Generate a Picocli @Command with subcommands, @Option/@Parameters, type converters, exit code constants, and unit tests. |
| **quarkus-resource** | `/quarkus-resource` | Generate a Quarkus RESTEasy Reactive @Path resource with DTOs, @RegisterForReflection, ExceptionMapper, and @QuarkusTest unit tests. |
| **spring-controller** | `/spring-controller` | Generate a Spring Boot @RestController with matching DTOs, mappers, @ControllerAdvice handler, and unit tests following hexagonal architecture. |
| **x-analyze-telemetry** | `/x-analyze-telemetry` | Analyze telemetry NDJSON for one or more epics and produce a Markdown report with skill/phase/tool aggregates, Mermaid Gantt timeline, and optional JSON/CSV exports. Use to answer 'which phase is the bottleneck?' and 'is skill X getting slower?' questions for operator visibility. |
| **x-analyze-telemetry-trends** | `/x-analyze-telemetry-trends` | Detect cross-epic P95 regressions (>= threshold %) and rank top-10 slowest skills from the global telemetry index. Single-responsibility partner of /x-analyze-telemetry focused on trend detection, not point-in-time reporting. Use to answer 'is skill X getting slower over the last N epics?' with evidence. |
| **x-arch-plan-capability** | `/x-arch-plan-capability` | Generate C4 Container + Component diagrams for a capability in Mermaid or PlantUML. |
| **x-arch-plan-feature** | `/x-arch-plan-feature` | Generate C4 Context + Container diagrams for a feature in Mermaid or PlantUML. |
| **x-arch-plan-product** | `/x-arch-plan-product` | Generate C4 Context + Container + Component diagrams for a product in Mermaid or PlantUML. |
| **x-audit-code** | `/x-audit-code` | Full codebase review against all project standards. Launches parallel subagents per audit dimension (Clean Code, SOLID, Architecture, Tests, Security, Cross-file), consolidates findings into a severity-categorized report with score. Use for periodic quality validation. |
| **x-audit-dependencies** | `/x-audit-dependencies` | Checks project dependencies for vulnerabilities, outdated versions, and license issues. Detects build tool automatically, runs language-specific audit commands, and generates a severity-categorized report. |
| **x-audit-supply-chain** | `/x-audit-supply-chain` | Enhanced supply chain security audit beyond x-audit-dependencies. Analyzes maintainer risk, typosquatting detection, phantom dependencies, dependency age, EPSS scoring, and SLSA assessment. Produces SARIF 2.1.0 output with weighted risk scoring. |
| **x-cleanup-git-branches** | `/x-cleanup-git-branches` | Cleans local git state in one pass: fetches origin with prune, removes all non-main worktrees (any path), and deletes all local branches except main/master/develop. Destructive by default with an interactive y/N confirmation gate; supports --dry-run (preview) and --yes (non-interactive). |
| **x-commit-changes** | `/x-commit-changes` | Creates Conventional Commits with Task ID in scope and pre-commit chain (format -> lint -> compile). Central commit point in the task-centric workflow with TDD tag support. |
| **x-commit-planning** | `/x-commit-planning` | Batch-commits planning artifacts under plans/** (or whitelisted template paths) without triggering the code pre-commit chain (format/lint/compile). Sibling of x-commit-changes for docs/markdown/json generated by planning skills. |
| **x-create-capability** | `/x-create-capability` | Decompose a Product into Capabilities with explicit RNF inheritance and no-relax markers. |
| **x-create-feature** | `/x-create-feature` | Create a complete feature (Epic + N Stories + Implementation Map) from a spec file, with a worktree-isolated docs/ branch, consolidated commit, and auto-merged PR into epic/XXXX. |
| **x-create-git-branch** | `/x-create-git-branch` | Creates a bare git branch (no worktree) from a configurable base with naming validation and idempotency. Single source of truth for branch creation logic consumed by orchestrators (x-internal-ensure-epic-branch, x-implement-story) and users. |
| **x-create-jira-epic** | `/x-create-jira-epic` | Create a Jira Epic from an existing local epic markdown file. Read the epic file, map fields to Jira, create the issue via MCP, and sync the Jira key back to the local file. |
| **x-create-jira-stories** | `/x-create-jira-stories` | Create Jira Stories from existing local story markdown files. Read all story files in an epic directory, map fields to Jira, create issues with parent epic link, create dependency links between stories, and sync Jira keys back to local files. |
| **x-create-pr** | `/x-create-pr` | Task-level PR creation with formatted title, automatic labels, structured body, and target branch logic. Creates standardized PRs for individual tasks with Task ID traceability. |
| **x-create-product** | `/x-create-product` | Transform an ideation file into a Product artifact with RNF roots and C1 capability stub. |
| **x-detect-spec-drift** | `/x-detect-spec-drift` | Detects spec-code drift by comparing story data contracts, endpoints, and Gherkin scenarios against implemented code. Supports standalone mode (full report) and inline mode (compact output for TDD loop integration in x-implement-story Phase 2). |
| **x-drive-tdd** | `/x-drive-tdd` | Executes systematic Red-Green-Refactor TDD cycles for a task. Reads the task plan generated by x-plan-task, runs each cycle in TPP order, validates RED/GREEN/REFACTOR phases, delegates atomic commits to x-commit-changes with TDD tags, and supports resume and dry-run. |
| **x-epic-create** | `/x-epic-create` | Create a focused Epic artifact from an existing Feature markdown, preserving sourceFeature lineage and inherited RNFs without generating stories or implementation map. |
| **x-evaluate-hardening** | `/x-evaluate-hardening` | Evaluates application hardening posture against CIS and OWASP benchmarks: HTTP security headers, TLS configuration, CORS policy, cookie security, error handling, input limits, and information disclosure. Produces SARIF output with weighted scoring. |
| **x-evaluate-parallelism** | `/x-evaluate-parallelism` | Detects and classifies file-collision risks between work units (tasks, stories, or epic phases) before parallel execution. Reads ## File Footprint blocks produced by x-plan-task / x-plan-story, applies the parallelism-heuristics knowledge pack (hard / regen / soft categories + hotspot overrides), and emits a collision matrix + serialization recommendation in Markdown (default) or JSON. |
| **x-evaluate-runtime** | `/x-evaluate-runtime` | Evaluate runtime protection controls: rate limiting, WAF rules, bot protection, DDoS mitigation, account lockout, brute force protection, CSP enforcement, and permissions policy. Produce SARIF 2.1.0 output with ASVS compliance mapping and scored Markdown report. |
| **x-execute-tests** | `/x-execute-tests` | Runs tests with coverage reporting and threshold validation. Use whenever writing, running, or analyzing tests. Triggers on: test, coverage, TDD, unit test, integration test, test failure, coverage gap, or Definition of Done validation. |
| **x-feature-create** | `/x-feature-create` | Create a complete feature (Epic + N Stories + Implementation Map) from a spec file, with a worktree-isolated docs/ branch, consolidated commit, and auto-merged PR into epic/XXXX. |
| **x-feature-ideate** | `/x-feature-ideate` | Transform free-form prose or a text file into a structured RA9 spec (5 mandatory sections) and open a PR on docs/feature-<slug> targeting develop for human review before invoking x-feature-create. |
| **x-fix-epic-pr** | `/x-fix-epic-pr` | Discovers all PRs from an epic via execution-state.json, fetches and classifies review comments in batch, generates a consolidated findings report, applies fixes, and creates a single correction PR. Supports dry-run, explicit PR list fallback, and idempotent re-execution. |
| **x-fix-pr** | `/x-fix-pr` | Reads PR review comments and fixes actionable ones automatically. Detects PR from argument or branch, classifies comments (actionable/suggestion/question/praise), implements fixes, and commits with proper conventional commit messages. |
| **x-format-code** | `/x-format-code` | Formats source code using the appropriate formatter for {{LANGUAGE}}. First step of the pre-commit chain (format -> lint -> compile -> commit). Supports --check (dry-run) and --changed-only modes. |
| **x-generate-adr** | `/x-generate-adr` | Automates ADR generation from architecture plan mini-ADRs: extracts inline decisions, expands to full ADR format, assigns sequential numbering, updates the ADR index, and adds cross-references. |
| **x-generate-ci** | `/x-generate-ci` | Generate or update CI/CD pipelines based on project stack: detect language, analyze existing workflows, generate CI/CD/release/security pipelines, validate with actionlint, support monorepo triggers. |
| **x-generate-docs** | `/x-generate-docs` | Documentation automation v2: stack-aware generation consuming documentation.targets from ProjectConfig. Detects documentation type needed (API, README, ADR, changelog, system-architecture) from code changes and stack config, delegates to specialized skills or generates inline. Invokes x-update-system-architecture on architectural change detection. |
| **x-generate-release-changelog** | `/x-generate-release-changelog` | Changelog generator v2: hybrid format combining narrative Highlights block (derived from 'Entrega de Valor' of merged v2 epics) with Keep-a-Changelog sections (Added/Changed/Fixed/Breaking/Deprecated). Stack-aware via documentation.changelog.format config. Falls back gracefully when EPIC-0070 epics unavailable (D-R10). |
| **x-generate-security-dashboard** | `/x-generate-security-dashboard` | Aggregates results from all security scanning skills into a unified posture view with score 0-100, trend tracking, OWASP risk heatmap, per-dimension breakdown, and remediation priority queue. Never executes scans — reads existing results only (RULE-011). |
| **x-generate-security-pipeline** | `/x-generate-security-pipeline` | Generate CI/CD pipeline configurations with conditional security stages based on SecurityConfig flags. Support GitHub Actions, GitLab CI, and Azure DevOps with minimal and full stage modes, configurable severity thresholds, and SARIF artifact upload. |
| **x-handle-incident** | `/x-handle-incident` | Guides incident response with severity-based checklists, communication templates, and postmortem triggers. Interactive guide for SEV1-SEV4 incidents covering classification, response coordination, and action item tracking. |
| **x-ideate-feature** | `/x-ideate-feature` | Transform free-form prose or a text file into a structured RA9 spec (5 mandatory sections) and open a PR on docs/feature-<slug> targeting develop for human review before invoking x-create-feature. |
| **x-implement-epic** | `/x-implement-epic` | Thin orchestrator (~460 lines — story-0049-0018 refactor) that drives an epic end-to-end via 6 delegated phases: Phase 0 (args via x-internal-normalize-args), Phase 1 (load+plan via x-internal-build-epic-plan), Phase 2 (epic branch via x-internal-ensure-epic-branch), Phase 3 (sequential-by-default story loop via x-implement-story), Phase 4 (integrity gate + report via x-internal-verify-epic-integrity + x-internal-write-report), Phase 5 (final PR epic/XXXX → develop via x-merge-branches + x-create-pr). Defaults flipped by EPIC-0049: sequential execution (opt-in parallel via --parallel), auto-merge of story PRs into epic/XXXX (target changed from develop). Legacy EPIC-0042 behavior preserved under --legacy-flow (auto-detected via execution-state.json flowVersion=1). Zero inline git/gh/jq/mvn calls — orchestrator uses only Read/Glob + Skill. |
| **x-implement-story** | `/x-implement-story` | Thin orchestrator (~320 lines — story-0049-0019 refactor) that drives a story end-to-end via 4 delegated phases: Phase 0 (args via x-internal-normalize-args + context via x-internal-load-story-context + resume via x-internal-resume-story), Phase 1 (parallel planning via x-internal-build-story-plan), Phase 2 (task execution loop via x-implement-task per task, then final story PR via x-create-pr), Phase 3 (verify via x-internal-verify-story + report via x-internal-write-story-report + optional worktree cleanup via x-manage-worktrees). New EPIC-0049 flags --target-branch / --auto-merge / --epic-id propagate OO-style to x-implement-task and x-create-pr. Backward compatible: absent flags preserve legacy EPIC-0048 behavior (target=develop, auto-merge=none). |
| **x-implement-task** | `/x-implement-task` | Implements a feature/story/task using TDD (Red-Green-Refactor) workflow. Schema-aware: v1 (legacy) runs the original Double-Loop TDD flow with story-section task extraction; v2 (task-first, EPIC-0038) reads task-TASK-XXXX-YYYY-NNN.md + plan-task-TASK-XXXX-YYYY-NNN.md, honours declared I/O contracts, respects task-implementation-map dependencies, verifies post-conditions via grep/assert, and produces a single atomic commit per task via x-commit-changes. |
| **x-lint-code** | `/x-lint-code` | Analyzes source code with the appropriate linter for {{LANGUAGE}}. Second step in the pre-commit chain (RULE-007: format -> lint -> compile -> commit). Supports --fix, --changed-only, and --strict modes. |
| **x-lint-contract-tests** | `/x-lint-contract-tests` | Validates API contracts (OpenAPI 3.1, AsyncAPI 2.6, Protobuf 3) against their specifications. Reports structural errors, missing fields, and spec violations. |
| **x-manage-pr-merge-train** | `/x-manage-pr-merge-train` | Merge-train automation: discovers, validates, and merges a sequence of PRs into develop in deterministic order. Supports --prs, --epic, and --pattern discovery modes with pre-merge validation and dry-run auditing. |
| **x-manage-worktrees** | `/x-manage-worktrees` | Manages git worktrees for parallel task and story execution. Operations: create, list, remove, cleanup, detect-context. Follows Rule 14 (Worktree Lifecycle) naming convention under .claude/worktrees/{identifier}/. |
| **x-merge-branches** | `/x-merge-branches` | Merges a source branch into a target branch locally with configurable strategy (merge/squash/rebase), automatic conflict detection + rollback, and idempotent no-op when target already contains source HEAD. Centralizes the ~120 lines of inline Bash previously in x-implement-epic Phase 1.4e auto-rebase. |
| **x-merge-pr** | `/x-merge-pr` | Merges a single PR via gh CLI with configurable strategy (merge/squash/rebase), idempotency for already-merged PRs, pre-checks for CI and approvals in synchronous mode, GitHub native auto-merge in --auto mode, and structured error codes. Extracted from x-implement-epic Phase 1.3b to provide a testable, reusable merge primitive callable from x-create-pr --auto-merge and x-implement-epic. |
| **x-migrate-templates** | `/x-migrate-templates` | Assists migration of a v1 epic document to the v2 value-driven template (EPIC-0070). Parses v1 technical blocks (Packages, Contratos, SOLID, Observabilidade), classifies each block with a safe default heuristic, and optionally asks the operator for confirmation per block (--interactive). Side-effects: writes epic.md in v2 format atomically, creates ADRs for 'virar ADR' decisions, updates system.md via x-update-system-architecture. Supports --dry-run and recovery from interrupted sessions. |
| **x-model-threats** | `/x-model-threats` | Generate threat models using STRIDE analysis: identify components, map data flows, analyze threats per category, classify severity, suggest mitigations, and produce threat model document. |
| **x-orchestrate-epic** | `/x-orchestrate-epic` | Orchestrates multi-agent planning for all stories in an epic, respecting dependency order, with checkpoint and resume support. |
| **x-plan-architecture** | `/x-plan-architecture` | Generates a comprehensive architecture plan with component diagrams, sequence diagrams, deployment topology, mini-ADRs, NFRs, and resilience/observability strategies. Use before implementation to document design decisions. |
| **x-plan-story** | `/x-plan-story` | Multi-agent story planning: launches 7 specialized agents (Architect, QA, Security, PentestEngineer, TechLead, ProductOwner, PerformanceEngineer) in parallel to produce a consolidated task breakdown, individual task plans, planning report, and DoR validation. Schema-aware: v1 (legacy) runs the original 6-phase flow; v2 (task-first, EPIC-0038) adds Phases 4a-4c that emit task-TASK-NNN.md + plan-task-TASK-NNN.md per task and a task-implementation-map-STORY-*.md, wiring every task through x-plan-task in parallel. |
| **x-plan-task** | `/x-plan-task` | Generates a detailed per-task implementation plan (plan-task-TASK-XXXX-YYYY-NNN.md) with TDD cycles in TPP order, file impact analysis by architecture layer, security checklist by task type, and exit criteria. Two invocation modes: task-file-first (--task-file) consumes a standalone task-TASK-XXXX-YYYY-NNN.md contract (EPIC-0038); story-scoped (STORY-ID --task TASK-ID) reads the task from story Section 8 (legacy). Invocable standalone OR via x-plan-story (future). |
| **x-plan-tests** | `/x-plan-tests` | Generates a Double-Loop TDD test plan with TPP-ordered scenarios before implementation. Delegates KP reading to a context-gathering subagent, then produces structured Acceptance Tests (outer loop) and Unit Tests in Transformation Priority Premise order (inner loop). |
| **x-profile-performance** | `/x-profile-performance` | Automated profiling: detect language/runtime, select appropriate profiler, execute session, generate flamegraph, identify hotspots, and suggest optimizations referencing the performance-engineering knowledge pack. |
| **x-promote-ideation** | `/x-promote-ideation` | Promote a transient x-ideate-feature output to a persistent ideation artifact in ai/ideations/. |
| **x-push-branch** | `/x-push-branch` | Git operations: branch creation, atomic commits (Conventional Commits), push, and PR creation. Use for any git workflow task including branching, committing, pushing, creating PRs, or managing version control. |
| **x-recommend-mcp** | `/x-recommend-mcp` | Analyzes project tech stack and recommends relevant MCP (Model Context Protocol) servers. Auto-detects language, framework, database, cache, and message broker from project config, then matches against a built-in catalog of MCP servers with installation instructions. |
| **x-reconcile-status** | `/x-reconcile-status` | Reconciles execution-state.json (telemetry) against the **Status:** field of Epic / Story markdown artifacts. Default mode (diagnose) is read-only and prints a divergence table. Opt-in --apply rewrites the markdowns atomically via StatusFieldParser and commits via x-commit-changes. Respects Rule 19 (legacy v1 epics skip silently) and Rule 22 (markdown is SoT; state.json is telemetry). Use for manual recovery of legacy epics whose markdown status drifted from execution checkpoints. |
| **x-refine-epic** | `/x-refine-epic` | Multi-persona 4-phase strategic epic refinement dispatcher. Phase A: 5-6 parallel specialist agents analyse the epic for strategic gaps. Phase B: single consolidated question batch to the operator. Phase C: 5-6 parallel specialists refine with answers. Phase D: Architect (opus) consolidates a Refinement Verdict with scope=epic and dual-writes to execution-state.json + epic markdown. |
| **x-refine-story** | `/x-refine-story` | Multi-persona 4-phase story refinement dispatcher. Phase A: 5-7 parallel specialist agents analyse the story for gaps. Phase B: single consolidated question batch to the operator. Phase C: 5-7 parallel specialists refine with answers. Phase D: Architect (opus) consolidates a Refinement Verdict and dual-writes to execution-state.json + story markdown. |
| **x-release** | `/x-release` | Orchestrates complete release flow using Git Flow release branches with approval gate, PR-flow (gh CLI) and deep validation: version bump (auto-detect or explicit), release branch creation from develop, deep validation (coverage, golden files, version consistency), version file updates, changelog generation, release commit, release PR via gh (optionally reviewed by x-review-pr), human approval gate with persistent state file, tag on main after merged PR, back-merge PR to develop with conflict detection, and cleanup. Supports hotfix releases from main, dry-run mode, resume via --continue-after-merge, in-session pause via --interactive, GPG-signed tags, skip-review opt-out, and custom state file path. |
| **x-review-api** | `/x-review-api` | Validates REST API endpoints for RFC 7807 error responses, pagination, URL versioning, OpenAPI documentation, status codes, and DTO patterns. |
| **x-review-codebase** | `/x-review-codebase` | Parallel code review with specialist engineers (Security, QA, Performance, Database, Observability, DevOps, API, Event). Invokes individual review skills in parallel via Skill tool, then consolidates into a scored report. Use for pre-PR quality validation. |
| **x-review-devops** | `/x-review-devops` | DevOps specialist review: validates Dockerfile, container security, CI/CD pipeline, resource limits, health probes, graceful shutdown, and deployment configuration. |
| **x-review-events** | `/x-review-events` | Validates event schemas, producer/consumer patterns, error handling, dead letter topics, and operational readiness for event-driven architectures. |
| **x-review-performance** | `/x-review-performance` | Performance specialist review: validates N+1 queries, connection pools, async patterns, pagination, caching, timeouts, circuit breakers, and resource cleanup. |
| **x-review-pr** | `/x-review-pr` | Tech Lead holistic review with 53-point checklist covering Clean Code, SOLID, architecture, framework conventions, tests, TDD process, security, and cross-file consistency. Produces GO/NO-GO decision. Use for final review before merge. |
| **x-review-qa** | `/x-review-qa` | QA specialist review: validates test coverage, TDD compliance, test naming, fixtures, parametrized tests, and acceptance criteria coverage. |
| **x-run-dynamic-pentest** | `/x-run-dynamic-pentest` | DAST gate: reads QualityConfig.dast, dispatches OWASP ZAP (passive smoke tier / active full tier) and Nuclei, produces SARIF 2.1.0 + Markdown report, blocks merge on HIGH/CRITICAL findings. |
| **x-scan-owasp** | `/x-scan-owasp` | Automated OWASP Top 10 (2021) verification mapped to ASVS levels (L1/L2/L3). Checks all 10 categories (A01-A10) with per-category pass/fail, ASVS coverage percentage, score grading, SARIF 2.1.0 output, and CI integration. Delegates A06 to x-audit-dependencies. |
| **x-search-memory** | `/x-search-memory` | Queries ai/memory/ for decisions, patterns, and anti-patterns across past epics |
| **x-setup-env** | `/x-setup-env` | Validate and configure local development environment: detect stack, check prerequisites, verify versions, validate IDE config, test database connectivity, run initial build, and report status with fix suggestions. |
| **x-story-create** | `/x-story-create` | Create focused Story artifacts from an existing Feature markdown with epic linkage, sourceFeature metadata, inherited RNFs, and Product-First streamlined sections. |
| **x-troubleshoot-operations** | `/x-troubleshoot-operations` | Diagnoses errors, stacktraces, build failures, and unexpected behavior. Systematic approach: reproduce, locate, understand, fix, verify. Use whenever something fails: compilation errors, test failures, runtime exceptions, coverage gaps, or performance issues. |
| **x-update-architecture** | `/x-update-architecture` | Incrementally updates the service architecture document with changes from architecture plans. Adds new components, integrations, flows, and ADR references without rewriting existing content. Use after implementation to keep architecture documentation current. |
| **x-update-system-architecture** | `/x-update-system-architecture` | Incrementally updates docs/architecture/system.md after an epic completes. Appends new entries to the Decision Log (§11) using x-internal-write-report --append (dedup by ## ID: marker) and surgically inserts component/integration changes into sections 1-10 via Edit. Idempotent: re-running with the same epic produces a byte-identical system.md. |
| **x-validate-docs** | `/x-validate-docs` | Documentation freshness gate: validates 6 dimensions (readme, api-specs, grpc-proto, adr, skill-docs, system-architecture) against code changes in a PR. Stack-aware — only validates targets declared/auto-detected from ProjectConfig.documentation.targets. Returns exit non-zero on staleness; produces structured report. |
| **x-watch-pr-ci** | `/x-watch-pr-ci` | Polls a PR's CI checks and Copilot review status, blocking until checks complete or timeout. Returns one of 8 stable exit codes (SUCCESS=0, CI_PENDING_PROCEED=10, CI_FAILED=20, TIMEOUT=30, PR_ALREADY_MERGED=40, NO_CI_CONFIGURED=50, PR_CLOSED=60, PR_NOT_FOUND=70). Writes a versioned state-file for session resume. |

**Total: 107 skills**

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
| `x-internal-ensure-epic-branch` | Referenced internally by agents |
| `x-internal-load-story-context` | Referenced internally by agents |
| `x-internal-map-epic` | Referenced internally by agents |
| `x-internal-normalize-args` | Referenced internally by agents |
| `x-internal-pr-body-render` | Referenced internally by agents |
| `x-internal-precheck-worktree` | Referenced internally by agents |
| `x-internal-render-pr-body` | Referenced internally by agents |
| `x-internal-resume-story` | Referenced internally by agents |
| `x-internal-rnf-validate` | Referenced internally by agents |
| `x-internal-summarize-epic` | Referenced internally by agents |
| `x-internal-update-status` | Referenced internally by agents |
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
| **devsecops-engineer** | `devsecops-engineer.md` |
| **event-engineer** | `event-engineer.md` |
| **java-developer** | `java-developer.md` |
| **pentest-engineer** | `pentest-engineer.md` |
| **performance-engineer** | `performance-engineer.md` |
| **product-owner** | `product-owner.md` |
| **qa-engineer** | `qa-engineer.md` |
| **security-engineer** | `security-engineer.md` |
| **sre-engineer** | `sre-engineer.md` |
| **tech-lead** | `tech-lead.md` |

**Total: 13 agents**

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
| Rules (.claude) | 30 |
| Skills (.claude) | 85 |
| Knowledge Packs (.claude) | 22 |
| Agents (.claude) | 13 |
| Hooks (.claude) | 17 |
| Settings (.claude) | 2 |
| Plan Templates (.claude) | 29 |

Generated by `ia-dev-env v0.1.0`.
