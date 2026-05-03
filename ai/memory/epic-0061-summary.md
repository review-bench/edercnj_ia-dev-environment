---
epic-id: EPIC-0061
slug: local-first-governance
summary-version: "1.0"
created: "2026-05-03"
last-updated: "2026-05-03"

indexable: true
archived: false
superseded-by: null

tags: [governance, lifecycle, non-interactive, stack-aware, camada-0]
capabilities-affected: []
rules-affected: [Rule 20, Rule 26]
adrs-referenced: [ADR-0017]

patterns-introduced:
  - non-interactive-default
  - camada-0-preventive-hooks
  - stack-aware-scripts-assembler
  - java-auditor-tests-in-mvn-verify
antipatterns-rejected:
  - interactive-default-in-llm-session
  - generator-centric-audit-yml
  - audit-yml-parallel-to-ci

dependencies-of: [EPIC-0058, EPIC-0049]
dependencies-for: [EPIC-0064, EPIC-0069]
---
# Memory: EPIC-0061 — Local-First Lifecycle & Stack-Aware Governance

## Why this epic existed

Three simultaneous problems: (a) `audit-*.sh` scripts lived in the generator and referenced Maven/JaCoCo — useless when generating for Spring Boot, Node, Python targets; (b) `.github/workflows/audit.yml` ran 8 bash audits in parallel to `ci.yml`, duplicating runner cost and introducing flakiness; (c) Rule 20 interactive default caused **LLM menu hangs** — orchestrators showed 3-option gates (PROCEED/FIX-PR/ABORT) that no LLM session ever answered.

## Hypothesis tested

Separating three layers (local hooks preventivo / stack-aware bash templates generated per target / Java `*AuditTest` in `mvn verify`) would fix all three problems simultaneously, delivering `flowVersion: "3"`. **Confirmed**: `audit.yml` deleted; `ScriptsAssembler` generates stack-correct audit scripts; `--interactive` becomes opt-in (Rule 20 flipped); 8 `*AuditorTest.java` classes replace workflow; Camada 0 added to Rule 26 taxonomy.

## Decisions taken (with why)

1. **Non-interactive default (Rule 20 flip)** — LLM sessions have no human waiting at menus; `--interactive` is explicit opt-in; `--non-interactive` flag deprecated (RULE-001). `CLAUDE_LEGACY_INTERACTIVE=1` escape hatch for 2-release window.
2. **`ScriptsAssembler` stack-aware** — audit templates under `targets/claude/scripts/{stack}/`; generator resolves by stack and copies correct bash scripts to project-target `.claude/scripts/`.
3. **`audit.yml` deleted (RULE-007, RULE-008)** — audit gates now run as `*AuditorTest.java` under `mvn verify`; single CI job; no parallel runner cost.
4. **Camada 0 added to Rule 26** — preventive hooks (`verify-*.sh`, `enforce-*.sh`) fire during LLM turn (PreToolUse / Stop events); distinction preventivo/detectivo formalized in ADR-0017.
5. **`flowVersion: "3"` for local-first epics** — `localFirstLifecycle: true` in state file; non-interactive default + Java audits + stack-aware scripts.

## Alternatives rejected (with why)

- **Keep `audit.yml`** — parallel runner cost + flakiness outweigh convenience; Java tests run faster in same JVM.
- **Interactive opt-out flag (`--non-interactive`)** — was opt-in to auto-mode; flipped: non-interactive is now the default since LLM sessions never have humans at menus.
- **Universal bash audit scripts (not stack-aware)** — Maven references in bash break Python/Node targets; must generate per-stack.

## Reusable patterns produced

- **`non-interactive-default`**: orchestrators proceed automatically; menus shown only with `--interactive`; prevents LLM stall.
- **`camada-0-preventive-hooks`**: fires during LLM turn; blocks before artifact produced; exit 0=OK, 2=WARNING; latency < 500ms.
- **`stack-aware-scripts-assembler`**: `ScriptsAssembler` resolves from `scripts/{stack}/` path; fallback to `scripts/default/` if stack-specific absent.
- **`java-auditor-tests-in-mvn-verify`**: `*AuditTest.java` replaces workflow jobs; structured assertions; same CI pipeline.

## Anti-patterns observed

- **Interactive default in LLM session** — menus never get answered; session hangs indefinitely; always use non-interactive as default.
- **Generator running its own audit scripts** — generator audits should validate generation output, not runtime concerns of target projects.

## Links

- Epic: `ai/epics/epic-0061-local-first-governance/epic-0061.md`
- ADRs: `docs/adr/ADR-0017-local-first-lifecycle.md`
- PRs: (merged into develop)
- Reports: `ai/epics/epic-0061-local-first-governance/reports/`
