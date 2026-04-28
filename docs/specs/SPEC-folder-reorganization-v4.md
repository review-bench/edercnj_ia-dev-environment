# Prompt: Geração de Épico e Histórias — ia-dev-environment Folder Reorganization v4

> **Instrução de uso**: Execute `/x-epic-create` com este arquivo como especificação de entrada.
> Exemplo: `/x-epic-create specs/SPEC-folder-reorganization-v4.md`

---

## Sistema

**Projeto**: `ia-dev-environment` — CLI generator de ambientes de desenvolvimento assistidos por IA.

**Versão base analisada**: branch `epic/0059`, ~60 epics concluídas, ~95 skills no catálogo.

**Objetivo desta especificação**: Reorganizar a estrutura de pastas do repositório do "layout v3"
(plans/epic-XXXX/, audits/, results/, /adr/, /specs/, /steering/) para o "layout v4" (ai/epics/,
docs/, governance/), introduzindo unidades de trabalho auto-contidas (story/bug/spike/chore com
spec + plans + reviews + reports lado a lado em sub-pasta única) e consolidando templates,
documentação humana e baselines de governança em raízes intencionais.

**Princípio central de todas as histórias**: A estrutura atual cresceu organicamente ao longo de
60 epics e acumulou problemas que comprometem clareza, descoberta e governança: `plans/` é nome
mentiroso (contém epics, stories, plans, reports, reviews, telemetry sob o mesmo guarda-chuva);
story spec mora separada de seus artefatos (4 lugares para revisar uma única story); templates
espalhados em 3 lugares (`.claude/templates/`, `specs/_templates/`, `adr/_TEMPLATE-ADR.md`);
`docs/` quase vazio enquanto ADRs e SPECs system-level vivem soltos; `results/` com propósito
ambíguo; `audits/` mistura baselines de governança com saídas de runs locais; releases soltas em
`plans/`; bug/spike/chore sem cidadania (toda unidade de trabalho é tratada como "story");
inconsistência v3 com reviews em duas rotas; `plans/unknown/` órfão.

A migração deve ser retrocompatível para os 60 epics legados (flowVersion ≤ 2) que permanecem em
`plans/` em modo read-only, e os epics novos (flowVersion v4) nascem no novo layout. Backward
compat via probe automático em `PathResolver`.

---

## Escopo do Épico

### Contexto de negócio

A reorganização atende três classes de stakeholders:

1. **Operadores humanos** que abrem o repo para revisar uma story precisam atualmente abrir 4
   diretórios diferentes (`plans/epic-X/story-X-Y.md`, `plans/epic-X/plans/`, `plans/epic-X/reviews/`,
   `plans/epic-X/reports/`). No novo layout, abrir UMA pasta basta.

2. **Skills automatizadas** (42 skills + 4 rules + 6 hooks + 4 Java assemblers) que hoje têm paths
   hardcoded em strings raw. No novo layout, todas resolvem paths via helper central
   (`PathResolver`) que probe layout v3 vs v4 automaticamente.

3. **Governança CI** (Rule 24, 26, 27, 45) que hoje verifica artefatos via globs caso-a-caso. No
   novo layout, baselines vivem em `governance/baselines/` (separadas de runs locais) e a
   convenção de paths é uniforme entre todos os tipos de unidade de trabalho.

### Dimensões de melhoria

1. **Pasta `ai/`** — Substitui `plans/` como raiz de tudo que é gerado/consumido pelo ciclo de IA.
   Sub-estrutura: `ai/epics/<epic>/`, `ai/releases/`, `ai/runs/`.

2. **Sub-pastas por tipo de unidade** — Sob `ai/epics/<epic>/work/`: `stories/`, `bugs/`,
   `spikes/`, `chores/`. Cada unidade tem prefixo correspondente (`story-`, `bug-`, etc.) e
   anatomia idêntica: spec + `plans/` + `reviews/` + `reports/`.

3. **Pasta `docs/`** — Casa única para documentação humana: `docs/adr/` (era `/adr/`),
   `docs/specs/` (era `/specs/`), `docs/architecture/`, `docs/contracts/` (era `/contracts/`),
   `docs/runbooks/` (era `results/runbooks/`), `docs/operations/`, `docs/configuration/`.

4. **Pasta `governance/`** — Separa governança de execução: `governance/baselines/` (era
   `/audits/`), `governance/steering/` (era `/steering/`).

5. **Templates centralizados em `.claude/templates/<categoria>/`** — Absorve `specs/_templates/`,
   `adr/_TEMPLATE-ADR.md`. Sub-categorias: `epic/`, `story/`, `task/`, `plan/`, `review/`,
   `report/`, `audit/`, `adr/`, `spec/`.

6. **Schema v4 com discriminator `flowVersion: 4`** — Probe automático em `PathResolver` decide
   layout antigo vs novo por epic.

### Mapeamento de Estrutura Antiga → Nova

| De (v3) | Para (v4) |
|:---|:---|
| `plans/epic-XXXX/epic-XXXX.md` | `ai/epics/epic-XXXX-<slug>/epic.md` |
| `plans/epic-XXXX/IMPLEMENTATION-MAP.md` | `ai/epics/epic-XXXX-<slug>/implementation-map.md` |
| `plans/epic-XXXX/execution-state.json` | `ai/epics/epic-XXXX-<slug>/execution-state.json` |
| `plans/epic-XXXX/telemetry/events.ndjson` | `ai/epics/epic-XXXX-<slug>/telemetry/events.ndjson` |
| `plans/epic-XXXX/story-XXXX-YYYY.md` | `ai/epics/<epic>/work/stories/story-XXXX-YYYY-<slug>/story.md` |
| `plans/epic-XXXX/plans/{plan,arch,tests,security,...}-story-X-Y.md` | `ai/epics/<epic>/work/stories/<unit>/plans/{plan,arch,tests,security,...}.md` |
| `plans/epic-XXXX/reviews/review-{tipo}-story-X-Y.md` | `ai/epics/<epic>/work/stories/<unit>/reviews/{tipo}.md` |
| `plans/epic-XXXX/reports/{completion,verify,dependency-audit}-story-X-Y.{md,json,txt}` | `ai/epics/<epic>/work/stories/<unit>/reports/{...}.{md,json,txt}` |
| `plans/release-state-X.Y.Z.json` | `ai/releases/release-state-X.Y.Z.json` |
| `results/{audits,reviews,test-runs,runbooks}/` | `ai/runs/{audits,reviews,test-runs}/` + `docs/runbooks/` |
| `/adr/ADR-NNNN-*.md` | `docs/adr/ADR-NNNN-*.md` |
| `/adr/_TEMPLATE-ADR.md` | `.claude/templates/adr/_TEMPLATE-ADR.md` |
| `/specs/SPEC-*.md` | `docs/specs/SPEC-*.md` |
| `/specs/_templates/*` | `.claude/templates/spec/*` |
| `/audits/*-baseline.txt` | `governance/baselines/*-baseline.txt` |
| `/steering/*` | `governance/steering/*` |
| `/contracts/*` | `docs/contracts/*` |
| `plans/unknown/` | **REMOVIDO** |

---

## Regras de Negócio Transversais (Cross-Cutting Rules)

**RULE-001**: **Backward Compat via flowVersion** — Epics com `flowVersion ≤ 2` em
`execution-state.json` permanecem em `plans/` em modo read-only e NÃO são migrados. Epics
`flowVersion: 3` ou ausente são migrados para v4 quando o script é executado. Epics novos nascem
sempre com `flowVersion: 4`.

**RULE-002**: **PathResolver Como Fonte Única** — Toda referência a path operacional em skills,
rules, hooks e Java assemblers DEVE passar por `PathResolver` (helpers `epicDir`, `unitDir`,
`planDir`, `reviewDir`, `reportDir`, `epicTelemetry`, `epicState`, `releasesDir`). Strings raw
hardcoded são proibidas pós-EPIC.

**RULE-003**: **Anatomia Uniforme por Tipo de Unidade** — Story, bug, spike e chore compartilham
o MESMO esquema interno: `<tipo>.md` + `plans/` + `reviews/` + `reports/`. O tipo é discriminado
por **(a)** sub-pasta (`stories/` vs `bugs/` vs `spikes/` vs `chores/`) e **(b)** prefixo do
diretório (`story-`, `bug-`, `spike-`, `chore-`). Frontmatter carrega `type:` redundantemente.

**RULE-004**: **Templates em Lar Único** — Todos os templates (`_TEMPLATE-*.md`) ficam sob
`.claude/templates/<categoria>/` na source-of-truth (`framework/.../resources/targets/claude/templates/`).
Templates em `specs/_templates/`, `adr/_TEMPLATE-ADR.md` ou qualquer outra rota são proibidos
após EPIC.

**RULE-005**: **Migração Idempotente** — Script `migrate-layout.sh` DEVE ser idempotente: rodar
duas vezes produz o mesmo estado final. Suporta `--dry-run` (sem efeitos) e `--apply`.

**RULE-006**: **Reviews em Rota Canônica Única** — A rota legacy `plans/epic-X/plans/review-*.md`
(que coexiste com `plans/epic-X/reviews/`) é eliminada. Após EPIC, reviews ficam
EXCLUSIVAMENTE em `ai/epics/<epic>/work/<tipo>/<unit>/reviews/`.

**RULE-007**: **`plans/` Congelado Pós-Migração** — Pre-commit hook recusa criação de NOVOS
arquivos sob `plans/`. Apenas leitura de epics legados é permitida. Esta regra entra em vigor
apenas na Story 6.

**RULE-008**: **Tag Pré-Migração para Rollback** — Antes de aplicar `migrate-layout.sh --apply`,
criar tag git `pre-layout-v4` apontando para o último commit pré-migração. Permite rollback
determinístico se algum problema for detectado.

**RULE-009**: **Janela de Freeze Para Migração** — Story 2 (script + apply) demanda janela de
freeze de 1 dia útil. Notificar equipe e pausar PRs em voo. Documentar no `governance/baselines/migration-report-2026.md`.

**RULE-010**: **CI Workflows Atualizados Atomicamente** — Quando baselines movem de `audits/`
para `governance/baselines/`, os arquivos `.github/workflows/*.yml` que os referenciam DEVEM
ser atualizados na MESMA PR para evitar quebra de CI.

**RULE-011**: **Compat Layer Removível** — Probe v3/v4 em `PathResolver` é mantido por 2 sprints
após a migração. Story 6 remove o probe e congela `plans/` definitivamente.

**RULE-012**: **SemVer MAJOR Bump** — A mudança de schema v3→v4 é breaking conforme Rule 08
(release process). CHANGELOG.md ganha entrada `BREAKING CHANGE: layout v4 — see migration guide`
e a versão recebe MAJOR bump.

---

## Histórias

---

### STORY-0001: PathResolver helper + introdução de schema v4

**Escopo**: Criar módulo central `PathResolver` em `framework/src/main/java/dev/iadev/util/`
com helpers que resolvem paths para layout v3 (legacy) ou v4 (novo) baseado em probe automático.
Introduzir `flowVersion: 4` como discriminator no `execution-state.json`.

**Detalhes**:
- Classe `PathResolver` com métodos:
  - `epicDir(epicId): Path` → `ai/epics/epic-<id>-<slug>` se v4, senão `plans/epic-<id>`
  - `unitDir(epicId, type, unitId): Path` (type ∈ {story, bug, spike, chore})
  - `planDir(...)`, `reviewDir(...)`, `reportDir(...)`
  - `epicTelemetry(epicId)`, `epicState(epicId)`, `releasesDir()`
  - `runsDir()` → `ai/runs/`
- Probe: `Files.exists(Paths.get("ai/epics", "epic-" + id + "-*"))` via glob
- Schema bump: `flowVersion: 4` em `ExecutionState` POJO
- Testes unitários: probe v3 em fixture legacy + probe v4 em fixture novo + mixed mode

**Critérios de Aceite**:
- `mvn test -Dtest=PathResolverTest` verde
- Probe detecta v3 em fixture `plans/epic-0042/` e v4 em fixture `ai/epics/epic-0050-foo/`
- Métodos retornam paths absolutos via `Path.toAbsolutePath()`
- Schema v4 documentado em `governance/rules/19-backward-compatibility.md`
- 100% line coverage do `PathResolver`

---

### STORY-0002: Script `migrate-layout.sh` idempotente

**Escopo**: Criar `scripts/migrate-layout.sh` que migra epics com `flowVersion ≥ 3` do layout v3
(`plans/`) para layout v4 (`ai/epics/`), preservando histórico via `git mv`.

**Detalhes**:
- Recebe `--dry-run` (default) e `--apply`
- Para cada `plans/epic-XXXX/`:
  1. Lê `flowVersion` em `execution-state.json`
  2. Se ≤ 2: mantém (legado strito); reporta "skipped: legacy"
  3. Se 3+: extrai slug do epic.md, cria `ai/epics/epic-XXXX-<slug>/`, move arquivos via `git mv`:
     - `epic-XXXX.md` → `epic.md`
     - `IMPLEMENTATION-MAP.md` → `implementation-map.md`
     - `execution-state.json` (atualiza `flowVersion: 4`)
     - `telemetry/events.ndjson` (preserva)
     - Para cada `story-XXXX-YYYY.md`: cria `work/stories/story-XXXX-YYYY-<slug>/` e move spec + plans/* + reviews/* + reports/*
     - Detecta `type: bug|spike|chore` no frontmatter e roteia para `work/bugs/`, etc.
- Gera `governance/baselines/migration-report-2026.md` com tabela: epic | flowVersion | action | files_moved
- Cria tag git `pre-layout-v4` antes do apply
- Idempotência: re-run em estado já migrado é no-op

**Critérios de Aceite**:
- `bash scripts/migrate-layout.sh --dry-run` em fixture com 3 epics (1 v2, 2 v3) reporta apenas 2 ações
- `bash scripts/migrate-layout.sh --apply` move corretamente; re-execução é no-op
- Tag `pre-layout-v4` criada antes do apply
- Relatório `migration-report-2026.md` gerado com diff por epic
- Testes integration validam migration + idempotência
- Script tem `--self-check` exit 0 quando dependências (jq, git) estão presentes

---

### STORY-0003: Mover ADRs, specs, templates, baselines, steering, contracts, results

**Escopo**: Movimentar atomicamente as pastas raiz para seus novos lares conforme RULE-004 e o
mapeamento da seção "Mapeamento de Estrutura Antiga → Nova". Atualizar 4 Java assemblers que
escrevem nesses paths.

**Detalhes**:
- `git mv adr/ docs/adr/` (preserva 20 ADRs + README, exceto `_TEMPLATE-ADR.md`)
- `git mv adr/_TEMPLATE-ADR.md framework/.../resources/targets/claude/templates/adr/`
- `git mv specs/SPEC-*.md docs/specs/` (10 arquivos)
- `git mv specs/_templates/* framework/.../resources/targets/claude/templates/spec/` (3 arquivos)
- `rmdir specs/`
- `git mv audits/ governance/baselines/`
- `git mv steering/ governance/steering/`
- `git mv contracts/ docs/contracts/`
- `git mv results/runbooks docs/runbooks/`
- `git mv results/ ai/runs/` + criar `ai/runs/README.md` explicando propósito
- `rm -rf plans/unknown/`
- Atualizar Java assemblers afetados:
  - `DocsAdrAssembler.java` → emit em `docs/adr/`
  - `DocsContributingAssembler.java` → emit em `docs/specs/` (templates de specs)
  - `ReleaseChecklistAssembler.java` → idem
  - `SloSliTemplateAssembler.java` → idem
- Atualizar `expected-artifacts.json` (test resources) e `AssemblerRegressionSmokeTest.java`
- Adicionar shim `adr/README.md` redirecionando para `docs/adr/` (1 release apenas)

**Critérios de Aceite**:
- `mvn verify` passa após movimentos (assemblers gravam nos novos paths)
- `find . -maxdepth 2 -type d \( -name adr -o -name specs -o -name audits -o -name steering -o -name contracts -o -name results \) ! -path './docs/*' ! -path './governance/*' ! -path './ai/*'` retorna vazio
- ADRs preservam histórico git (`git log --follow docs/adr/ADR-0001*.md` mostra commits originais)
- `AssemblerRegressionSmokeTest` verde com fixtures atualizadas
- `plans/unknown/` removido
- Shim `adr/README.md` funciona por 1 release; planejado para remoção em Story 6

---

### STORY-0004: Atualizar 42 SKILLs para usar PathResolver

**Escopo**: Substituir paths hardcoded em strings raw nas 42 SKILL.md por chamadas a
`PathResolver`. Skills críticas listadas no plano.

**Detalhes**:
- Inventário: `grep -rnE "plans/epic-|audits/|results/|specs/|adr/|steering/|contracts/" framework/src/main/resources/targets/claude/skills/ --include=SKILL.md`
- Para cada match: substituir literal por placeholder `{{epicDir}}/...` ou similar resolvido em runtime via `PathResolver`
- Skills agrupadas por categoria:
  - **Implementação** (3): x-task-implement, x-story-implement, x-epic-implement
  - **Planejamento** (10): x-epic-create, x-story-create, x-task-plan, x-story-plan, x-arch-plan, x-test-plan, x-epic-decompose, x-epic-map, x-epic-orchestrate, x-task-plan
  - **Review** (6+): x-review, x-review-pr, x-review-qa, x-review-perf, x-review-devops, x-review-security
  - **PR** (5): x-pr-create, x-pr-watch-ci, x-pr-fix, x-pr-fix-epic, x-pr-merge-train, x-pr-merge
  - **Telemetria** (2): x-telemetry-analyze, x-telemetry-trend
  - **Auditoria** (7+): x-code-audit, x-dependency-audit, x-supply-chain-audit, x-owasp-scan, x-spec-drift, x-status-reconcile, x-security-dashboard
  - **Release/docs** (5): x-release, x-release-changelog, x-adr-generate, x-arch-update, x-doc-generate
  - **Jira** (2): x-jira-create-stories, x-jira-create-epic
  - **Internas** (10): x-internal-* skills

**Critérios de Aceite**:
- `grep -rnE "plans/epic-|audits/|results/|specs/|adr/|steering/|contracts/" framework/src/main/resources/targets/claude/skills/ --include=SKILL.md` retorna 0 linhas (excluindo `## Triggers`, `## Examples`, notas históricas marcadas)
- Smoke test executa `x-epic-create` + `x-story-implement` em fixture dummy e cria artefatos no novo layout
- Cada skill tocada tem fixture de smoke test verde
- Documentação (CLAUDE.md, READMEs por skill) atualizada

---

### STORY-0005: Atualizar Rules, Hooks e Java Assemblers

**Escopo**: Atualizar artefatos críticos de governança (Rules 24, 26, 27, 45), hooks shell e
Java assemblers para refletir novos paths.

**Detalhes**:
- **Rules** (4 arquivos em `framework/src/main/resources/targets/claude/rules/`):
  - Rule 24 (Execution Integrity): `plans/epic-X/reports/verify-envelope-*` → `ai/epics/<epic>/work/<tipo>/<unit>/reports/verify-envelope.json`
  - Rule 26 (Audit Gate Lifecycle): `audits/` → `governance/baselines/`
  - Rule 27 (Zero-Bypass Lifecycle): atualiza 12 surfaces de evidência
  - Rule 45 (CI-Watch Integrity): paths de state-file
- **Hooks** (6+ scripts em `framework/src/main/resources/targets/claude/hooks/`):
  - `verify-story-completion.sh`: probe paths via PathResolver wrapper Bash
  - `verify-phase-gates.sh`, `enforce-phase-sequence.sh`, `telemetry-emit.sh`, `telemetry-phase.sh`, `enforce-no-bypass-flags.sh`
  - Atualizar fixtures golden em `framework/src/test/resources/golden/`
- **Java Assemblers** (4 classes):
  - `FileCategorizer.java` — regex de categorização de paths
  - `DocsAdrAssembler.java`, `DocsContributingAssembler.java`, `ReleaseChecklistAssembler.java`, `SloSliTemplateAssembler.java` (já tocados na Story 3, validar)
- **CI workflows** (`.github/workflows/*.yml`):
  - Atualizar paths de baseline (`governance/baselines/...`)
  - Atualizar caminhos de scripts CI
- **Docs raiz**: `CLAUDE.md`, `Conventions.md`, `AGENTS.md`, `README.md` — atualizar paths citados

**Critérios de Aceite**:
- `mvn verify` verde com fixtures golden atualizadas
- `bash scripts/audit-execution-integrity.sh --self-check` exit 0
- `bash scripts/audit-skill-visibility.sh` verifica catálogo `docs/audit-gates-catalog.md` atualizado
- CI passa com baselines em `governance/baselines/`
- `grep -rnE "plans/epic-|audits/|results/|specs/" framework/src/main/resources/targets/claude/{rules,hooks}/` retorna 0 (exceto notas de migração explícitas)
- Smoke test end-to-end executa `x-epic-implement` em fixture dummy e produz todos os 12 surfaces de evidência (Rule 27) nos novos paths

---

### STORY-0006: Compat layer cleanup + congelamento de `plans/`

**Escopo**: Após 2 sprints rodando ambos os layouts em produção, remover compat layer do
`PathResolver`, congelar `plans/` para writes e documentar a mudança breaking no CHANGELOG.

**Detalhes**:
- Remover probe v3 do `PathResolver`; manter apenas leitura legacy se houver epics ainda em
  `plans/` (sem migração possível por serem flowVersion ≤ 2)
- Adicionar pre-commit hook `forbid-writes-to-legacy-plans.sh` que recusa criação de novos
  arquivos sob `plans/` (apenas modificações em arquivos existentes são permitidas para hotfixes
  documentais)
- Atualizar `governance/baselines/execution-integrity-baseline.txt` com cutoff de schema v4
- Remover shim `adr/README.md` da Story 3
- Atualizar `CHANGELOG.md` com entrada `BREAKING CHANGE: layout v4 — see governance/baselines/migration-report-2026.md`
- Bumpa versão MAJOR conforme Rule 08
- Atualizar `audits/lifecycle-integrity-baseline.txt` (que agora vive em `governance/baselines/`)
  para refletir novo cutoff

**Critérios de Aceite**:
- Pre-commit hook recusa `git add plans/epic-XXXX/new-file.md` com mensagem clara
- `PathResolver.probeLayout()` retorna apenas v4 para epics novos; leitura de v2 legacy ainda funciona
- `CHANGELOG.md` tem entrada `## [X+1.0.0]` com nota de breaking change
- Shim `adr/README.md` removido
- `mvn verify` continua verde
- Baselines `governance/baselines/*.txt` atualizadas com cutoff timestamp
- Tag `layout-v4-frozen` aponta para o commit de Story 6

---

## Definition of Done (DoD) Global

- [ ] Todos os critérios de aceite de cada story atendidos
- [ ] `mvn verify` verde após cada story (≥ 95% line, ≥ 90% branch coverage mantidos)
- [ ] Nenhum teste existente quebrado sem ADR documentando exceção
- [ ] CI workflows atualizados para refletir novos paths
- [ ] CHANGELOG.md atualizado com entradas conforme Rule 08
- [ ] Tag `pre-layout-v4` criada antes da Story 2 apply
- [ ] Tag `layout-v4-frozen` criada após Story 6
- [ ] `governance/baselines/migration-report-2026.md` documenta tudo que foi migrado
- [ ] Documentação operacional (CLAUDE.md, README.md, AGENTS.md) atualizada
- [ ] Smoke test end-to-end (`x-epic-implement` em fixture dummy) passa pelos 12 surfaces de
      evidência (Rule 27) nos novos paths
