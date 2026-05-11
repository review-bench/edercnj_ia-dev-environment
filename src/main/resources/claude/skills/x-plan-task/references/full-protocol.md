# x-plan-task — Full Protocol

> Verbose detail moved out of `SKILL.md` per ADR-0012 (skill body slim-by-default).
> SKILL.md remains the minimum viable behavioral contract; this document carries the
> complete phase-by-phase protocol, sub-step prompts, telemetry hooks, schemas, the
> full mandatory plan template, and editorial rules. Loaded on-demand via `Read` only
> when the slim contract is insufficient for the task at hand.

---

## 1. Invocation Modes (Detailed)

### 1.1 Task-file-first mode (`--task-file <path>`) — EPIC-0038 canonical

Consumes a standalone `task-TASK-XXXX-YYYY-NNN.md` contract previously emitted by `x-plan-story` Phase 4a. The task file follows the `task-schema.md` specification (story-0038-0001) and is validated by `TaskFileParser`. Required sections:

- Header with `**Task ID:**`, `**Story:**`, `**Status:**`
- `## 1. Objetivo`
- `## 2. Contratos I/O` with `### 2.1 Inputs`, `### 2.2 Outputs`, `### 2.3 Testabilidade`
- `## 3. Definition of Done`
- `## 4. Dependências`
- `## 5. Plano de implementação` (filled by this skill)

Validation rules:

- **RULE-TF-01 Testability:** §2.3 must contain exactly one checked declaration among `[x] INDEPENDENT` / `[x] REQUIRES_MOCK` / `[x] COALESCED`. Empty or multi-selected aborts with exit 3.
- **RULE-TF-02 Outputs:** §2.2 must list at least one grep/assert/test-verifiable output.

### 1.2 Story-scoped mode — legacy (epics 0025-0037)

Reads the task from `## 8. Tasks` of a story file. The task ID is located by exact string match (`### TASK-XXXX-YYYY-NNN:`).

---

## 2. P1-P5 Lifecycle (EPIC-0049)

This skill adopts the canonical planning-versioning lifecycle (RULE-007). Placement inside the workflow:

| Step | When | Child skill | Behavior on `--no-commit` | Behavior on `--dry-run` |
|------|------|-------------|---------------------------|-------------------------|
| P1 — detect worktree | Start | `x-manage-worktrees detect-context` | Skip (orchestrator owns lifecycle) | Skip |
| P2 — ensure `epic/<ID>` branch | After P1 | `x-internal-ensure-epic-branch` | Skip | Skip |
| Phases 0-4 (original) | Middle | (inline) | Run unchanged | Run unchanged |
| Phase 5 (Write Plan) | Middle | (inline) | Run; file written | Run; file written |
| **Phase 5.4 — Planning Status Propagation** (alias Step P4) | After Phase 5 | `x-commit-changes` (v1) / staged-only (v2 batch) | SKIP commit; log `"[no-commit] Plan written; commit deferred to caller"` | SKIP commit; log `"dry-run, skipping commit"` |
| P5 — push | End | `x-push-branch` | Skip | Skip |

The P4 step is an **alias** over the pre-existing Phase 5.4 (Planning Status Propagation) which already orchestrates the commit. No additional P4 invocation is issued — adding one would double-commit. The alias section exists in the skill body purely so the P1-P5 naming is readable end-to-end.

---

## 3. `--no-commit` Contract (story-0049-0017)

| Aspect | `--no-commit=false` (default) | `--no-commit=true` (batch) |
|--------|-------------------------------|----------------------------|
| Plan file written to disk | Yes | Yes |
| Status flipped `Pendente -> Planejada` | Yes | Yes |
| `git add` of plan + task file | Yes | Yes |
| `x-commit-changes` invoked | Yes | **NO** (deferred to caller) |
| Response `commitSha` | non-null SHA | `null` |
| Re-invocation semantics | Idempotent (staleness check) | Idempotent; flipping the flag between runs alternates commit behavior |

**Caller contract (e.g., `x-plan-story`):** when invoking N tasks with `--no-commit=true`, the caller MUST aggregate all written paths and issue ONE consolidated `x-commit-planning` call covering every plan + status update — producing a single commit per story instead of N commits.

---

## Steps P1 + P2 — Worktree + Epic Branch Prelude

The canonical orchestrator prelude for these two steps lives in
[`../../_shared/orchestrator-prelude.md`](../../_shared/orchestrator-prelude.md).

Apply it with:

- `<HOST-SKILL>` = `x-plan-task`
- `<EPIC-ID>` resolved from the task source (see slim SKILL.md for the resolution rule).

Notes specific to this skill:

- P1 is fail-open (RULE-006 advisory). Continue on any `x-manage-worktrees detect-context` failure — log a WARNING and proceed to Step P2.
- P2 aborts with `EPIC_BRANCH_ENSURE_FAILED` on non-zero exit.
- When `--no-commit` is set (orchestrator mode), skip both steps — the parent owns branch lifecycle.

---

## Phase 0 — Validate and Pre-Check

### 0.1 Parse Arguments

Extract from story ID and task ID:

- Epic ID (XXXX) from `story-XXXX-YYYY`
- Story sequence (YYYY) from `story-XXXX-YYYY`
- Task number (NNN) from `TASK-XXXX-YYYY-NNN`

Validation rules:

- Story ID MUST match pattern `story-XXXX-YYYY` (4-digit groups)
- Task ID MUST match pattern `TASK-XXXX-YYYY-NNN` (3-digit task number)
- Epic ID from story MUST match epic ID in task ID
- Story sequence from story MUST match story sequence in task ID

If validation fails, abort with descriptive error message.

### 0.2 Resolve Epic Directory

1. Extract epic ID from story ID (`story-XXXX-YYYY` -> `epic-XXXX`)
2. Resolve `EPIC_DIR` with a glob that supports both exact and suffix variants:
   - Exact match: `ai/epics/epic-XXXX`
   - Suffix variant: `ai/epics/epic-XXXX-*`
3. If exactly one directory matches, use that as `EPIC_DIR`
4. If both exist, prefer exact match `ai/epics/epic-XXXX`
5. If no directory matches, abort: `"Epic directory not found for epic-XXXX"`

### 0.3 Resolve Paths

| Path | Pattern | Example |
|------|---------|---------|
| Story file | `<EPIC_DIR>/story-XXXX-YYYY.md` | `ai/epics/epic-XXXX/story-XXXX-YYYY.md` |
| Plan output | `<EPIC_DIR>/plans/plan-task-TASK-XXXX-YYYY-NNN.md` | `ai/epics/epic-XXXX/plans/plan-task-TASK-XXXX-YYYY-NNN.md` |
| Output dir | `<EPIC_DIR>/plans/` | `ai/epics/epic-XXXX/plans/` |

### 0.4 Idempotency Check (Staleness)

Before generating, verify whether a valid plan already exists:

1. If the plan file does NOT exist, proceed to Phase 1.
2. If the plan file exists AND `--force` is set, log: `"Regenerating task plan (--force)"`. Proceed to Phase 1.
3. If the plan file exists AND `--force` is NOT set:
   - If `mtime(story file) <= mtime(plan file)` — plan is **fresh**. Log: `"Task plan already exists and is up-to-date"`. Return existing plan. Stop.
   - If `mtime(story file) > mtime(plan file)` — plan is **stale**. Log: `"Regenerating stale task plan for TASK-XXXX-YYYY-NNN"`. Proceed to Phase 1.

---

## Phase 1 — Extract Contracts

> **RA9 guidance (EPIC-0056 / planning-standards-kp):** Before reading contracts, read
> `.claude/skills/planning-standards-kp/SKILL.md` to understand the RA9 9-section model.
> When the task file uses the RA9 v2 format (sections 1-9), map its Section 2 (Packages)
> and Section 8 (Decision Rationale — N/A accepted for tasks) to the plan output.

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-task Phase-1-Context-Gathering
```

### 1A. Task-file-first branch (EPIC-0038 — `--task-file` present)

1. Read the file at `<task-file>`. Abort with exit code 1 if missing.
2. Validate structure per story-0038-0001 schema (`ai/epics/epic-XXXX/schemas/task-schema.md`):
   - `**ID:** TASK-XXXX-YYYY-NNN` present and matches filename.
   - `**Story:** story-XXXX-YYYY` present and well-formed.
   - `**Status:**` in the allowed enum (`Pendente | Em Andamento | Concluída | Bloqueada | Falha`).
   - `## 2. Contratos I/O` with subsections `### 2.1 Inputs`, `### 2.2 Outputs`, `### 2.3 Testabilidade` — all three present.
   - `### 2.3 Testabilidade` has exactly one checked declaration (INDEPENDENT / REQUIRES_MOCK / COALESCED).
   - `## 3. Definition of Done` present with ≥ 6 items (warn otherwise).
3. If validation fails, abort with exit code 1 and message `Task file invalid: {violations}`.
4. If testability is absent, abort with exit code 3: `Testability not declared (RULE-TF-01)`.
5. Project extracted fields into the internal TaskContract:
   - **Objective**: body of `## 1. Objetivo`.
   - **Inputs**: body of `### 2.1 Inputs`.
   - **Outputs**: body of `### 2.2 Outputs` (drives File Impact analysis in Phase 3).
   - **TestabilityKind**: the single checked option (drives Phase 2 cycle shape).
   - **TestabilityReferences**: TASK-IDs cited (REQUIRES_MOCK / COALESCED partners).
   - **DependsOn**: TASK-IDs from the first column of `## 4. Dependências`.

### 1B. Story-scoped branch (legacy — no `--task-file`)

1. Read the story file at the resolved path.
2. Locate **Section 8** (heading `## 8.` or `## 8 `).
3. Find the task heading matching the task ID: `### TASK-XXXX-YYYY-NNN:` (case-insensitive match on the ID portion).
4. Extract the task definition including all fields:
   - **Title**: from the heading text after the task ID
   - **Layer**: Domain, Port, Adapter, Application, Config, Test, Doc
   - **Test Type**: Unit, Integration, API, Contract, E2E, Smoke, Verification
   - **Size**: S, M, L
   - **Dependencies**: list of TASK IDs or `--`
   - **Testability**: valid pattern from Section 8 table
   - **Files**: list of affected file paths
   - **Acceptance Criteria**: list of criteria
5. If the task ID is NOT found in Section 8, abort: `"Task TASK-XXXX-YYYY-NNN not found in story-XXXX-YYYY Section 8"`

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-task Phase-1-Context-Gathering ok
```

---

## Phase 2 — Map TDD Cycles (TPP Order)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-task Phase-2-Task-Breakdown
```

Generate TDD cycles based on the task's layer, test type, and acceptance criteria. Cycles MUST follow strict **Transformation Priority Premise** order:

### TPP Cycle Order

| Order | TPP Level | Transform | Description |
|-------|-----------|-----------|-------------|
| 1 | Degenerate | `{} -> nil` | Null/empty/zero inputs return default or error |
| 2 | Constant | `nil -> constant` | Single hardcoded return value |
| 3 | Scalar | `constant -> variable` | Parameterized return based on input |
| 4 | Conditional | `unconditional -> conditional` | Single if/else branching |
| 5 | Collection | `scalar -> collection` | Multiple items, iteration, map/filter |
| 6 | Complex | `collection -> complex` | Compound logic, nested conditions, state |

### Per-Cycle Structure

Each TDD cycle MUST be described in **natural language** (not just field names). A developer or AI must be able to read the cycle and understand exactly what to write, why it fails, and what to implement — without needing to infer intent.

```markdown
### Ciclo N — [TPP Level] (`[tpp_transform]`)

**RED — Teste a escrever:**
Nome: `[methodUnderTest_scenario_expectedBehavior]`
O que testar: [plain-language description of what the test covers — 1-2 sentences]
Por que vai falhar: [explain why no implementation exists yet — which class/method is missing]
Comando: `{{TEST_COMMAND}}`

**GREEN — Implementação mínima:**
[Plain-language description of the minimum code change to make the test pass.
Reference the specific class/method to create or modify. Example: "Create class Foo with
method bar() returning a hardcoded empty list."]
Comando: `{{COMPILE_COMMAND}}` → `{{TEST_COMMAND}}`

**REFACTOR:**
[Plain-language description of what to improve without changing behavior. Example:
"Extract the null-check into a private method isValid(). No behavior change."]

**Commit:** `feat(TASK-XXXX-YYYY-NNN): [description] [TDD:RED|GREEN|REFACTOR]`
```

### Minimum Cycles

Every task plan MUST contain at least 3 TDD cycles. The first cycle MUST always be a degenerate case (TPP Level 1). For domain logic tasks, target 4-6 cycles. For simple config/doc tasks, 3 cycles suffice.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-task Phase-2-Task-Breakdown ok
```

---

## Phase 3 — Analyze Affected Files by Layer

Organize all affected files by architecture layer following the {{ARCHITECTURE}} structure. For each file, indicate whether it is **new** (to be created) or **modified** (existing).

### Layer Order (Implementation Sequence)

Files MUST be listed in implementation order (inner layers first):

| Order | Layer | Package Pattern | Description |
|-------|-------|----------------|-------------|
| 1 | Domain | `domain/model/`, `domain/engine/` | Entities, value objects, business logic |
| 2 | Port | `domain/port/` | Inbound and outbound interfaces |
| 3 | Adapter (outbound) | `adapter/outbound/` | DB repositories, external clients |
| 4 | Adapter (inbound) | `adapter/inbound/` | REST controllers, CLI handlers, DTOs, mappers |
| 5 | Application | `application/` | Use cases, orchestration |
| 6 | Config | `config/` | Framework configuration |
| 7 | Test | `test/` | Test classes (mirrors production structure) |

### File Entry Format

| Field | Required | Description |
|-------|----------|-------------|
| Path | M | Relative file path |
| Action | M | `CREATE` or `MODIFY` |
| Layer | M | Architecture layer |
| Purpose | M | Brief description of what this file does/changes |

---

## Phase 4 — Generate Security Checklist

Generate a security checklist based on the task type. The checklist adapts to the nature of the task:

### Security Checklist by Task Type

| Task Type | Security Items |
|-----------|---------------|
| Endpoint / API | Input validation on all parameters; Output encoding (prevent XSS); Authentication check (endpoint protected); Authorization check (role/permission); Rate limiting consideration; CORS policy adherence |
| Persistence / DB | Parameterized queries only (no SQL concatenation); Sensitive data encryption at rest; Column-level access control; Audit logging for data mutations; No PII in log statements |
| Domain Logic | Business rule bypass prevention; State manipulation guards; Privilege escalation checks; Input boundary validation; Immutable value objects for sensitive data |
| Config | No hardcoded secrets or credentials; Secure defaults (fail-closed); Environment variable externalization; No default credentials; Configuration validation on startup |
| Integration | TLS validation (no trust-all); Certificate pinning where applicable; Timeout configuration (prevent hanging); Retry with backoff (prevent amplification); Circuit breaker for external calls |

For each applicable security item, generate a checklist entry with:

- [ ] Item description
- Severity: CRITICAL / HIGH / MEDIUM
- Reference: CWE identifier or OWASP category where applicable

---

## Phase 4.5 — Compute File Footprint

Emit a structured machine-readable footprint so downstream tooling (e.g., `/x-evaluate-parallelism`) can detect write-conflicts deterministically, without relying on prose parsing of "Affected Files".

### Inference Rules

For each path in the task's `Files:` list (Section 8 of the story):

| Rule | Condition | Target sub-section |
|------|-----------|--------------------|
| R1 (default) | Any path declared on the task | `write:` |
| R2 (golden regen — pom) | Path ends in `pom.xml` | Add `regen:` entry for corresponding artifacts |
| R3 (golden regen — skill source) | Path matches `src/main/resources/targets/claude/**/SKILL.md` | Add `regen:` entry in `.claude/skills/**` at mirror path |
| R4 (golden regen — targets tree) | Path under `src/main/resources/targets/claude/` (non-SKILL.md) | Add matching `regen:` entry in `.claude/` or `src/test/resources/golden/` |
| R5 (reads) | Path listed in task's `Dependencies -> reads` section | `read:` |

Empty sub-sections MUST be omitted from the plan output. Paths within each sub-section MUST be sorted alphabetically for determinism (RULE-008).

### Output Layout

Inject a `## File Footprint` section into the plan document, immediately BEFORE `## Definition of Done`:

```markdown
## File Footprint

### write:
- path/to/writeA
- path/to/writeB

### read:
- path/to/readA

### regen:
- path/to/regenA
```

### Knowledge Pack Reference

The inference rules above are the working contract documented in the `parallelism-heuristics` knowledge pack (`knowledge/parallelism-heuristics.md`). Read that KP when extending the rules (e.g., adding new regen patterns) to keep consumer tooling in sync.

---

## Phase 5 — Write Plan

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-task Phase-3-Validation
```

### 5.1 Ensure Output Directory

```bash
mkdir -p <EPIC_DIR>/plans/
```

### 5.2 Assemble Plan Document

Write the plan to `<EPIC_DIR>/plans/plan-task-TASK-XXXX-YYYY-NNN.md` using `_TEMPLATE-TASK-PLAN.md` as the canonical structure.

**MANDATORY — Origin Marker (EPIC-0059):** Prepend the YAML frontmatter block before any Markdown content:

```yaml
---
generated-by: x-plan-task@$(git rev-parse HEAD 2>/dev/null || echo "unknown")
generated-at: $(date -u +%Y-%m-%dT%H:%M:%SZ)
task-id: TASK-XXXX-YYYY-NNN
story-id: story-XXXX-YYYY
---
```

This frontmatter is required by `audit-execution-integrity.sh` Phase-1 validation (EPIC-0059, Rule 24). Artifacts without this block fail the CI audit with `EIE_EVIDENCE_MISSING`.

The document MUST follow the sections below in order. Each section marked OBRIGATÓRIO must be filled; omitting or leaving placeholders is a plan quality violation.

```markdown
# Plano de Task — TASK-XXXX-YYYY-NNN

## Cabeçalho

| Campo | Valor |
|-------|-------|
| Task ID | TASK-XXXX-YYYY-NNN |
| Story ID | story-XXXX-YYYY |
| Épico | epic-XXXX |
| Layer | [extracted from task source] |
| Tipo de Teste | [UT / AT / IT] |
| Tamanho | [S / M / L] |
| Status | Planejada |
| Gerado por | x-plan-task@[sha] |
| Gerado em | [ISO-8601 date] |

---

## 1. Contexto na Story

[OBRIGATÓRIO — Back-reference explícita: qual parte da story esta task implementa?
If plan-story-XXXX-YYYY.md exists, cite the specific section (e.g., "Seção 2.2 — criação de Foo").
If plan-story does not exist yet (standalone execution), derive from the story file description.
A reader must understand WHAT this task delivers without reading other files.]

Esta task implementa: **[what part of the story this task covers]**
Referência no plano da story: `plan-story-story-XXXX-YYYY.md > [section reference or "N/A — plan-story not yet generated"]`

---

## 2. Objetivo da Task

[OBRIGATÓRIO — 1-2 paragraphs in plain language.
Reference the specific files to be created/modified and what they will do.
Example: "This task creates the FooValidator class in the domain layer. It will validate
incoming Foo payloads by checking that field X is non-null and field Y is within range.
The validator is consumed by FooService.create() which will be modified in a subsequent task."]

---

## 3. Guia de Implementação

### 3.1 Alvo Principal

| Campo | Valor |
|-------|-------|
| Arquivo principal | `[main file path]` |
| Ação | CREATE / MODIFY |
| Classe / Função / Componente | `[ClassName or functionName]` |
| Padrão de Design | [Strategy / Factory / Repository / Value Object / etc.] |

### 3.2 Diagrama de Classes / Componentes desta Task

[OBRIGATÓRIO when this task creates or modifies structural components (classes, interfaces, skills).
Show ONLY the components touched by THIS task — a subset of the plan-story component diagram.
For Skills/Templates stories: use a flowchart instead of classDiagram.
If the task does not touch structural components (e.g., pure config or doc task), write "N/A — task sem impacto estrutural".]

\`\`\`mermaid
[classDiagram or flowchart scoped to this task's components]
\`\`\`

### 3.3 Testes Existentes Impactados por esta Task

[OBRIGATÓRIO — list every existing test that will be affected when THIS task is implemented.
If a business rule changes, any test covering that rule must appear here.
If no existing test is impacted, write: "Nenhum teste existente é impactado por esta task."]

| Arquivo de Teste | Método | Tipo de Impacto | O que muda na asserção |
|-----------------|--------|----------------|----------------------|
| [test file path] | [test method name] | MODIFY / DELETE | [what assertion changes and why] |

### 3.4 O que fazer em cada arquivo

[OBRIGATÓRIO — for each file impacted by this task: describe IN NATURAL LANGUAGE what to write or change.
Do NOT write the final code — describe the intent and expected result.
Follow the layer order: Domain → Port → Adapter → Application → Config → Test.]

**`[file path 1]`** ([CREATE/MODIFY] — [Layer])
> [Plain-language description of what to write/change in this file and why]

**`[file path 2]`** ([CREATE/MODIFY] — [Layer])
> [Plain-language description]

### 3.5 Ordem de Implementação

[In what sequence to create/modify the files? Rule: inner layers first.]

| Passo | Arquivo | Motivo da Ordem |
|-------|---------|----------------|
| 1 | `[file]` | [why this file comes first] |
| 2 | `[file]` | [why this file comes second] |

---

## 4. Ciclos TDD (Ordem TPP)

[Generated narrative cycles from Phase 2, in TPP order.
Each cycle must have full natural-language descriptions as specified in Phase 2 — not just field names.
Minimum 3 cycles. Cycle 1 MUST be degenerate.]

[Insert cycles here]

---

## 5. Checklist de Segurança

[Generated checklist from Phase 4, adapted to task type.
Mark as [ ] = pending, [x] = verified/not-applicable with justification.]

- [ ] [security item] ([CRITICAL/HIGH/MEDIUM])

---

## 6. File Footprint

[Generated block from Phase 4.5 — sub-sections write:, read:, regen: with alphabetically-sorted paths. Empty sub-sections omitted.]

### write:
[paths]

### read:
[paths]

### regen:
[paths]

---

## 7. Dependências

| Depende de | Motivo |
|-----------|--------|
| [TASK-ID or cross-story ref] | [why this dependency exists] |

---

## 8. Critérios de Conclusão desta Task

Ao terminar esta task, o executor DEVE:
- [ ] Todos os [N] ciclos TDD completados (RED → GREEN → REFACTOR)
- [ ] Todos os testes passando: `{{TEST_COMMAND}}`
- [ ] Artefato válido/compilando: `{{COMPILE_COMMAND}}`
- [ ] Checklist de segurança (Seção 5) verificada
- [ ] Testes existentes impactados (Seção 3.3) modificados/excluídos conforme mapeado
- [ ] Nenhum TODO/FIXME/HACK no escopo desta task
- [ ] Critérios de aceite da story aplicáveis a esta task satisfeitos
- [ ] Escrever `**Status:** Concluída` no arquivo `task-TASK-XXXX-YYYY-NNN.md`
- [ ] Atualizar `execution-state.json`: `tasks.TASK-XXXX-YYYY-NNN.status = COMPLETE`
```

### 5.3 Report

After writing, log: `"Task plan generated: plan-task-TASK-XXXX-YYYY-NNN.md (N TDD cycles, M affected files)"`.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-task Phase-3-Validation ok
```

---

## Planning Status Propagation (Rule 22 / EPIC-0046)

> V2-gated: only runs when `SchemaVersionResolver.resolve(ai/epics/epic-XXXX/execution-state.json) == V2`. v1 epics: skip silently (Rule 19).

After writing `plan-task-TASK-XXXX-YYYY-NNN.md`, propagate the lifecycle status of the source task artifact (`task-TASK-XXXX-YYYY-NNN.md` in v2, or the Section 8 task entry in v1) from `Pendente` to `Planejada` in the SAME commit as the plan artefact.

**Steps (end of Phase 3, BEFORE the final commit):**

1. Detect v2 via SchemaVersionResolver on the epic's `execution-state.json`. If v1: skip this entire block.
2. For the source task file `ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md`, read current status with the CLI:

   ```bash
   CURRENT=$(java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       read ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md)
   ```

3. If `CURRENT == "Pendente"`, write `Planejada` atomically:

   ```bash
   java -cp $CLAUDE_PROJECT_DIR/java/target/classes \
       dev.iadev.adapter.inbound.cli.StatusFieldParserCli \
       write ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md Planejada
   ```

   If `CURRENT == "Planejada"`: idempotent re-run, skip the write.

4. Stage both the source task file and the plan artefact:

   ```bash
   git add ai/epics/epic-XXXX/plans/task-TASK-XXXX-YYYY-NNN.md ai/epics/epic-XXXX/plans/plan-task-TASK-XXXX-YYYY-NNN.md
   ```

5. **Commit gate (`--no-commit` aware):**
   - If `--no-commit=false` (default): commit via `x-commit-changes` (Rule 13 Pattern 1 INLINE-SKILL):

     ```text
     Skill(skill: "x-commit-changes", args: "docs(task-TASK-XXXX-YYYY-NNN): add plan + update status to Planejada")
     ```

   - If `--no-commit=true` (EPIC-0049 batch mode): **SKIP the commit step**. The plan file and status write remain on disk (staged) but no commit is produced. Log: `"[no-commit] Plan written; commit deferred to caller"`. Return response with `commitSha: null`.

**Fail-loud:** non-zero CLI exit → abort skill with the same exit code (RULE-046-08).

---

## Step P4 — Planning Status Commit (alias of Phase 5.4)

The planning-commit step for `x-plan-task` is performed by **Phase 5.4 — Planning Status Propagation** (above). When `--no-commit=true` or `--dry-run=true`, that step becomes a no-op (logged as `"[no-commit] Plan written; commit deferred to caller"` or `"dry-run, skipping commit"` respectively). No additional P4 invocation is issued; this alias exists solely so the P1-P5 convention is readable end-to-end in the skill body (EPIC-0049 / RULE-007).

---

## Step P5 — Push to Origin (optional, EPIC-0049)

```text
<!-- TELEMETRY: phase.start -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh start x-plan-task Phase-P5-Push
```

If `--dry-run` or `--no-commit` is set, log `"dry-run, skipping push"` / `"orchestrated mode, skipping push"` and skip.

Delegate the push to `x-push-branch` so the canonical `epic/<ID>` branch is synchronized with origin:

```text
Skill(skill: "x-push-branch", args: "--branch epic/<XXXX>")
```

On push failure (remote rejection, no connectivity), log a WARNING and continue — the local commit is preserved; the operator can re-run Step P5 or `git push` manually. Do NOT abort.

```text
<!-- TELEMETRY: phase.end -->
Bash command: $CLAUDE_PROJECT_DIR/.claude/hooks/telemetry-phase.sh end x-plan-task Phase-P5-Push ok
```

---

## 4. Failure Matrix

| Scenario | Exit code | Mitigation |
|----------|-----------|------------|
| `x-internal-ensure-epic-branch` fails (P2) | 5 | Repair remote; re-run |
| `StatusFieldParserCli` exit 20 | 20 | Story-0049-0010 integrity gate recovery |
| `StatusFieldParserCli` exit 40 | 40 | Invalid transition — check source markdown |
| `x-commit-changes` failure in Phase 5.4 | propagate | Investigate pre-commit chain |
| `x-push-branch` failure (P5) | 0 (soft-fail) | Operator pushes manually |

---

## 5. Telemetry

Phase markers emitted:

- `Phase-P1-Worktree-Detect`
- `Phase-P2-Epic-Branch-Ensure`
- Original Phase 1-5 markers
- `Phase-P5-Push`

P4 reuses the existing telemetry of Phase 5.4 (no new phase marker to avoid double-counting).

---

## Source-of-truth note

Source: `src/main/resources/claude/skills/x-plan-task/references/full-protocol.md`. Generated output at `.claude/skills/x-plan-task/references/full-protocol.md` is byte-equivalent.
