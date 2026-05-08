---
generated-by: x-internal-build-story-plan@0b3ac24e2eef2f8390557b05844ffd36f7176489
generated-at: 2026-05-08T00:27:32Z
story-id: story-0080-0001
---

# Compliance Assessment — story-0080-0001: `/x-create-bug` Skill Scaffolding

> **Epic:** EPIC-0080 — Bug Lifecycle Management
> **Date:** 2026-05-08
> **Author:** Security Engineer (AI-assisted)
> **Template Version:** 1.0

---

## 1. Escopo Regulatório

### 1.1 Natureza do Artefato

Story-0080-0001 entrega artefatos exclusivamente de automação de desenvolvimento:
- `_TEMPLATE-BUG.md` — template Markdown para relatórios de bug
- `x-create-bug/SKILL.md` — skill de scaffolding de pasta `ai/bugs/bug-XXXXXX/`
- `bug-lifecycle.yaml` — declaração de capability (frontmatter v3.0)
- `audit-bug-classification.sh` — script de auditoria Camada 2

Nenhum desses artefatos processa, armazena, transmite ou manipula dados pessoais de usuários finais. O scope é estritamente interno ao repositório de desenvolvimento.

### 1.2 Aplicabilidade de Frameworks Regulatórios

| Framework | Aplicável | Justificativa |
| :--- | :--- | :--- |
| GDPR (Regulamento Geral de Proteção de Dados) | **N/A** | Nenhum dado pessoal de titulares processa. A skill cria arquivos Markdown locais em `ai/bugs/`. Dados de autoria (nome do dev no `git commit`) já são cobertos pelo contrato geral de uso do repositório — fora do escopo desta story. |
| LGPD (Lei Geral de Proteção de Dados Pessoais) | **N/A** | Idem GDPR. Sem coleta, tratamento ou transferência de dados pessoais de terceiros. O campo `author` no `bug.md` é dado do próprio dev, não de titular externo. |
| HIPAA | **N/A** | Nenhum dado de saúde (PHI) processado. |
| PCI-DSS | **N/A** | Nenhum dado de cartão ou transação financeira envolvido. |
| SOX | **N/A** | Não há impacto em dados financeiros ou controles de relatório financeiro. |

**Conclusão regulatória:** Nenhum framework de proteção de dados pessoais é acionado por esta story. A assessment foca nos controles de governança interna do projeto: Rule 28 (capability frontmatter), ADR-0005 (telemetria), integridade de ciclo de vida, e conformidade de template.

---

## 2. Classificação de Dados

### 2.1 Inventário de Dados

| Elemento de Dado | Classificação | Propósito | PII? | Cross-Border? | Retenção |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `description` (argumento da skill) | **Internal** | Gera slug do branch name e título do `bug.md` | Não | Não | Duração do bug no repositório |
| `severity` (enum: low/medium/high/critical) | **Internal** | Metadado do bug para priorização | Não | Não | Duração do bug no repositório |
| `scope` (enum: single-file/single-module/cross-module) | **Internal** | Metadado de impacto técnico | Não | Não | Duração do bug no repositório |
| `bugId` (gerado: `bug-XXXXXX`) | **Internal** | Identificador sequencial do bug | Não | Não | Permanente (histórico git) |
| `prUrl` (URL do GitHub PR) | **Internal** | Referência ao docs PR aberto | Não | Não | Duração do PR |
| `elapsedMs` (telemetria de duração) | **Internal** | KPI de performance (P75 ≤ 8 min) | Não | Não | Arquivo `events.ndjson` por epic |
| Eventos `events.ndjson` | **Internal** | Rastreabilidade de invocações da skill | Não | Não | Por epic, committed ao git |

**Total de elementos:** 7
**Elementos contendo PII:** 0
**Elementos requerendo criptografia:** 0
**Elementos com transferência cross-border:** 0

### 2.2 Data Minimization Checklist

- [x] Apenas campos necessários para scaffolding são coletados (`description`, `severity`, `scope`)
- [x] Sem campos opcionais coletados silenciosamente (campos omitidos recebem defaults documentados)
- [x] Sem armazenamento de dados além dos arquivos de repositório (git-tracked)
- [x] Nenhuma coleta de dados de usuários finais do sistema — apenas dados do desenvolvedor-autor
- [x] Sem retention policy especial requerida — dados residem no histórico git padrão

---

## 3. Conformidade com Rule 28 — Capability Frontmatter v3.0

### 3.1 Contexto

Rule 28 (EPIC-0064 — Capability-Driven Composition) exige que todo artefato gerado pelo `ia-dev-env` declare `requires-capabilities` no frontmatter v3.0. A ausência é build error; schema v2 quebra o build.

### 3.2 Artefatos desta Story e Status de Conformidade

| Artefato | `requires-capabilities` declarado | Schema v3.0 | Capability ID válido | Status |
| :--- | :--- | :--- | :--- | :--- |
| `x-create-bug/SKILL.md` | `[governance.bug-lifecycle]` | Sim (obrigatório pelo contrato da skill) | Validado contra `bug-lifecycle.yaml` após task-0080-0001-002 | **COMPLIANT** |
| `_TEMPLATE-BUG.md` | `[governance.bug-lifecycle]` | Sim | Idem | **COMPLIANT** |
| `bug-lifecycle.yaml` (capability descriptor) | `[]` (universal — artifacts dentro de `capabilities/` são fontes, não consumidores) | Sim | N/A (é o arquivo fonte da capability) | **COMPLIANT** |
| `audit-bug-classification.sh` | `[governance.bug-lifecycle]` | Sim | Validado | **COMPLIANT** |
| `bug.md` (output da skill — gerado em `ai/bugs/`) | `[governance.bug-lifecycle]` | Sim (AC-1 verifica frontmatter) | Idem | **COMPLIANT** |

### 3.3 Invariantes Rule 28 Verificadas

| Invariante | Requisito | Verificação para esta Story |
| :--- | :--- | :--- |
| INV-1: Declaração obrigatória | Todo artefato fora de `_common/` DEVE declarar `requires-capabilities` | Todos os 4 artefatos fonte declaram — verificado por task-0080-0001-002 e AC-1 |
| INV-2: Schema v3.0 estrito | Frontmatter validado por `governance/schemas/frontmatter-3.0.json` | `audit-frontmatter-schema.sh` cobrirá os novos artefatos no CI |
| INV-3: Capability ID válido | IDs devem existir em `capabilities/<category>/<id>.yaml` | `bug-lifecycle.yaml` criado em task-0080-0001-002 antes dos consumidores |
| INV-4: Excludes simétrico | Se A `excludes-capabilities: [B]`, B DEVE `excludes-capabilities: [A]` | Nenhum `excludes-capabilities` declarado nesta story — invariante N/A |
| INV-5: Composition-priority bounded | Se declarado, `composition-priority ∈ [0, 100]` | Não declarado explicitamente — default 50 aplicado |
| INV-6: Fragment ≠ artifact standalone | Arquivos em `fragments/` DEVEM declarar `fragment-slot` | Nenhum fragment nesta story — invariante N/A |
| INV-7: Slot ↔ Fragment coerência | Todo `fragment-slots` no parent deve ter `{{ slot: X }}` no body | N/A |
| INV-8: Sem retrocompatibilidade | Schema v3.0 obrigatório — sem fallback v2 | Artefatos criados já em v3.0 |

### 3.4 Risco Residual

**LOW.** O único risco é a criação de `bug-lifecycle.yaml` (task-0080-0001-002) antes dos demais artefatos (task-0080-0001-001, 003). A ordem de tasks garante isso implicitamente, mas deve ser validada pela smoke IT (task-0080-0001-007) que executa `audit-capability-coverage.sh` sobre os artefatos gerados.

---

## 4. Conformidade de Capability Declaration — `bug-lifecycle.yaml`

### 4.1 Schema v3.0 Obrigatório

O arquivo `capabilities/governance/bug-lifecycle.yaml` deve seguir o schema v3.0 conforme ADR-0016 e knowledge pack `capability-frontmatter.md`. Campos obrigatórios:

```yaml
# capabilities/governance/bug-lifecycle.yaml
id: governance.bug-lifecycle
category: governance
description: >
  Habilita criação, refinamento e auditoria de bugs como artefatos de primeira
  classe no fluxo ia-dev-environment. Requerido por x-create-bug, x-refine-bug,
  audit-bug-classification.sh, e pelo template _TEMPLATE-BUG.md.
version: "1.0"
introduced-by: EPIC-0080
stories:
  - story-0080-0001
artifacts:
  - type: skill
    path: "skills/core/plan/x-create-bug/SKILL.md"
  - type: template
    path: "templates/_TEMPLATE-BUG.md"
  - type: script
    path: "scripts/audit-bug-classification.sh"
```

### 4.2 Checklist de Conformidade do Descriptor

| Campo | Obrigatório | Presente no schema planejado | Status |
| :--- | :--- | :--- | :--- |
| `id` (formato `category.subcategory`) | Sim | `governance.bug-lifecycle` | PASS |
| `category` | Sim | `governance` | PASS |
| `description` | Sim | Presente | PASS |
| `version` | Sim | `"1.0"` | PASS |
| `introduced-by` | Recomendado | `EPIC-0080` | PASS |
| `artifacts` (lista de consumidores) | Sim | 3 artefatos listados | PASS |
| Capability ID formato `category.subcategory.atomic` | Sim | `governance.bug-lifecycle` (2 segmentos = válido para leaf capabilities) | PASS |

### 4.3 Audit Scripts que Validarão este Artefato

| Script | O que valida | Quando falha |
| :--- | :--- | :--- |
| `audit-capability-coverage.sh` | Todo artefato com `requires-capabilities: [governance.bug-lifecycle]` tem o ID existente | ID ausente em `capabilities/governance/bug-lifecycle.yaml` |
| `audit-frontmatter-schema.sh` | YAML do descriptor válido contra `frontmatter-3.0.json` | Campo obrigatório ausente ou tipo incorreto |
| `audit-capability-graph.sh` | IDs existem; sem mutex assimétrico; sem ciclos | Se `bug-lifecycle` referenciar capability inexistente |
| `audit-capability-determinism.sh` | Compor duas vezes = mesmo SHA | Geração não-determinística do descriptor |

---

## 5. Conformidade de Ciclo de Vida — Telemetria (ADR-0005)

### 5.1 Requisito

ADR-0005 define que skills de implementação e criação DEVEM emitir eventos de telemetria via `telemetry-phase.sh` para rastreabilidade. A skill `x-create-bug` é uma "creation skill" (instrumentada em stories 0040-0006 a 0040-0008 como categoria).

### 5.2 Eventos Obrigatórios para `x-create-bug`

| Evento | Emitido quando | Mecanismo | Campo `skill` |
| :--- | :--- | :--- | :--- |
| `session.start` | Antes de qualquer tool call | Hook `telemetry-session.sh` (automático) | N/A |
| `phase.start` (fase 1 — precheck) | Início do precheck de worktree | `telemetry-phase.sh start 1` no body da skill | `x-create-bug` |
| `phase.end` (fase 1 — precheck) | Após retorno de `x-internal-precheck-worktree` | `telemetry-phase.sh end 1` | `x-create-bug` |
| `phase.start` (fase 2 — scaffold) | Início da criação de pasta e `bug.md` | `telemetry-phase.sh start 2` | `x-create-bug` |
| `phase.end` (fase 2 — scaffold) | Após `Write bug.md` + `mkdir` | `telemetry-phase.sh end 2` | `x-create-bug` |
| `phase.start` (fase 3 — git ops) | Antes de branch + commit + PR | `telemetry-phase.sh start 3` | `x-create-bug` |
| `phase.end` (fase 3 — git ops) | Após PR URL disponível | `telemetry-phase.sh end 3` | `x-create-bug` |
| `tool.call` | Em cada Bash/Write/Skill tool call | Hook `telemetry-posttool.sh` (automático) | N/A |
| `session.end` | Após skill completa | Hook `telemetry-stop.sh` (automático) | N/A |

### 5.3 Destino dos Eventos

Conforme ADR-0005 §D4, eventos de `x-create-bug` serão gravados em:
```
ai/epics/epic-0080-bug-lifecycle-management/telemetry/events.ndjson
```
(contexto resolvido via `EPIC_CONTEXT` injetado pelo hook `telemetry-session.sh`).

### 5.4 Métricas de KPI Capturadas

A métrica de sucesso desta story — P75 time-to-file ≤ 8 min — é computável diretamente dos eventos:

```
elapsed = phase.end[fase 3].timestamp - phase.start[fase 1].timestamp
```

O campo `elapsedMs` no JSON de saída da skill (§5.3 do contrato da story) é derivado dessa diferença, tornando o KPI auditável sem instrumentação adicional.

### 5.5 Conformidade com Fail-Open

ADR-0005 §D1 exige `set +e` nos hooks de telemetria (fail-open: falha de telemetria NUNCA aborta a skill). O body de `x-create-bug/SKILL.md` DEVE mencionar que a ausência do script `telemetry-phase.sh` não é bloqueante. Verificado como critério de DoD da task-0080-0001-003.

### 5.6 Conformidade com PII Scrubber

O campo `description` (argumento do dev) passa pelo fluxo de eventos. O `TelemetryScrubber` (story-0040-0005) usa regex de detecção de PII/secrets; nenhum padrão de `description` típico de bug ("checkout total wrong when promo applied") aciona o scrubber. Sem risco de PII em telemetria.

---

## 6. Conformidade de Template — `_TEMPLATE-BUG.md` (RA9 v2)

### 6.1 Convenção RA9 v2

A convenção RA9 v2 (EPIC-0070 — Value-Driven Templates) exige 9 seções canônicas em templates de artefatos de planejamento. Story-0080-0001 estende RA9 com seções bug-specific adicionais.

### 6.2 Mapeamento das 9 Seções RA9 → Seções Bug-Specific

| # | Seção RA9 v2 Canônica | Equivalente em `_TEMPLATE-BUG.md` | Obrigatória? |
| :--- | :--- | :--- | :--- |
| RA9-1 | Visão / Problema | `## 1. Visão & Problema` (com sub-seção: Comportamento Observado vs. Esperado) | Sim |
| RA9-2 | Persona & Stakeholders | `## 2. Persona & Contexto de Descoberta` | Sim |
| RA9-3 | Entrega de Valor | `## 3. Impacto & Severidade` (severity enum + scope enum) | Sim |
| RA9-4 | Critérios de Aceite | `## 4. Critérios de Reprodução` (Gherkin com passos exatos) | Sim |
| RA9-5 | Contratos | `## 5. Receita de Reprodução` (ambiente, passos, pré-condições) | Sim |
| RA9-6 | Tasks | `## 6. Root-Cause Hypothesis` (hipótese inicial + classification tag) | Sim |
| RA9-7 | Dependências | `## 7. Regression Test Slot` (placeholder para teste RED antes do fix) | Sim |
| RA9-8 | Decision Rationale | `## 8. Decisões & Histórico` | Sim |
| RA9-9 | Refinement Verdict | `## Refinement Verdict` (YAML block: status/verdictHash/refinedBy/blockers) | Sim |

**Seções bug-specific adicionais (além das 9 RA9):**

| Seção Adicional | Propósito | Obrigatória em bug.md? |
| :--- | :--- | :--- |
| `## Observed vs. Expected` (sub-seção de RA9-1) | Diferencia comportamento atual do esperado em termos concretos | Sim |
| `## Root-Cause Classification` (sub-seção de RA9-6) | Tag: `regression` / `new-defect` / `environment` / `config` | Sim |
| `## Regression Test Slot` (RA9-7) | Slot explícito para o teste RED que deve existir ANTES do fix | Sim |

### 6.3 Frontmatter do `bug.md` (output da skill)

Todo `bug.md` gerado pela skill deve ter frontmatter v3.0 conforme AC-1:

```yaml
---
requires-capabilities: [governance.bug-lifecycle]
template-version: "3.0"
bug-id: "bug-XXXXXX"
severity: high
scope: single-module
status: scaffold-ready
created-at: "{{ISO8601_TIMESTAMP}}"
created-by: x-create-bug
---
```

### 6.4 Checklist de Conformidade do Template

- [x] 9 seções RA9 v2 presentes e na ordem canônica
- [x] Seções bug-specific adicionadas SEM remover seções RA9 obrigatórias
- [x] `## Refinement Verdict` no formato YAML block (compatível com `enforce-refinement-gate.sh`)
- [x] `## Regression Test Slot` é placeholder explícito, não seção opcional — reforça TDD regression-first
- [x] Frontmatter v3.0 com `requires-capabilities: [governance.bug-lifecycle]` presente
- [x] `template-version: "3.0"` declarado (Rule 28 invariante INV-2)

---

## 7. Conformidade de Ciclo de Vida — Lifecycle Integrity

### 7.1 Lifecycle Integrity Audit (EPIC-0046)

O `LifecycleIntegrityAuditTest` (story-0046-0007) escaneia todo `SKILL.md` sob `java/src/main/resources/targets/claude/skills/` para 3 regressões de Rule 22:

| Regressão | O que detecta | Risco para esta Story |
| :--- | :--- | :--- |
| `ORPHAN_PHASE` | Sub-seção documentada mas não referenciada | BAIXO — skill tem fases 1-3 documentadas e referenciadas |
| `WRITE_WITHOUT_COMMIT` | Write em `ai/epics/epic-*/reports/` sem `x-commit-changes` nas 20 linhas seguintes | N/A — skill escreve em `ai/bugs/`, não `reports/` |
| `SKIP_IN_HAPPY_PATH` | `--skip-verification` fora de `## Recovery` | BAIXO — skill não usa flags de skip |

**Ação requerida:** Após task-0080-0001-003, executar `LifecycleAuditCli scan` sobre `x-create-bug/SKILL.md` para confirmar 0 violações antes do merge.

### 7.2 Refinement Gate (Rule 29)

A story-0080-0001 tem `refinementVerdict.status: approved` (verificado no arquivo da story). O PreToolUse hook `enforce-refinement-gate.sh` não bloqueará a invocação de `x-implement-story story-0080-0001`.

### 7.3 Zero-Bypass Lifecycle (Rule 27)

Artefatos de evidência obrigatórios antes do merge do PR de implementação:

**Fase 1 — Plans (6 artefatos em `ai/epics/epic-0080-bug-lifecycle-management/plans/`):**
- [ ] `arch-story-0080-0001.md` — PRESENTE (gerado anteriormente)
- [ ] `compliance-story-0080-0001.md` — ESTE DOCUMENTO
- [ ] `security-story-0080-0001.md`
- [ ] `test-plan-story-0080-0001.md`
- [ ] `contract-story-0080-0001.md`
- [ ] `implementation-plan-story-0080-0001.md`

**Fase 3 — Reports (4 artefatos em `ai/epics/epic-0080-bug-lifecycle-management/reports/`):**
- [ ] `story-completion-report-0080-0001.md`
- [ ] `phase-completion-report-0080-0001.md`
- [ ] `specialist-review-story-0080-0001.md`
- [ ] `tech-lead-review-story-0080-0001.md`

**Telemetria:**
- [ ] Evento `tool.call` de `x-implement-story` em `events.ndjson` — requerido por Rule 27 §Invariante 3

---

## 8. Controles de Segurança

### 8.1 CWE-22 — Path Traversal (AC-4)

A skill aceita `description` como string livre. O slug-generation step DEVE:

1. Converter para lowercase
2. Remover qualquer ocorrência de `../`, `./`, `/`, `\`, `..`
3. Substituir espaços e caracteres não-`[a-z0-9]` por `-`
4. Truncar a 40 caracteres
5. Rejeitar slugs vazios após normalização com exit code 2 (`ARGS_INVALID`)

O branch name resultante DEVE satisfazer `^bug/[0-9]{6}-[a-z0-9-]{1,40}$` e o folder name `^bug-[0-9]{6}$`.

**Referência:** Rule 12 §J6 (CWE-22 — Improper Limitation of a Pathname to a Restricted Directory).

### 8.2 Hardcoded Credentials

- Nenhum token, secret ou credential presente ou necessário na skill.
- A skill invoca `gh pr create` (GitHub CLI) que usa credenciais do ambiente local do dev — nunca hardcoded.

### 8.3 Injeção via `description`

O `description` é transformado em slug antes de qualquer uso em comandos de shell. A skill DEVE usar `--` em flags do git para separar o slug de flags acidentais:
```bash
git checkout -b -- "bug/${BUG_ID}-${SLUG}"
```

---

## 9. Auditoria e Rastreabilidade

### 9.1 Audit Trail da Skill

| Evento | Campos mínimos | Rastreabilidade |
| :--- | :--- | :--- |
| Invocação de `/x-create-bug` | `skill`, `timestamp`, `args.description`, `args.severity`, `args.scope` | `events.ndjson` via hook `telemetry-posttool.sh` |
| Criação de `ai/bugs/bug-XXXXXX/` | `mkdir` stdout, `bugId` | JSON envelope stdout + git commit |
| Branch criado | `branch` name | `events.ndjson` + git log |
| PR aberto | `prUrl` | JSON envelope stdout + GitHub API |
| Duração total | `elapsedMs` | JSON envelope + `phase.end` event |

### 9.2 Audit Script Camada 2 — `audit-bug-classification.sh`

O script entregue em task-0080-0001-004 (via `src/main/resources/targets/claude/scripts/`) valida:
- Todo `ai/bugs/bug-XXXXXX/bug.md` presente tem `severity` declarado no frontmatter
- Todo `bug.md` tem `## Root-Cause Classification` com tag válida (`regression` / `new-defect` / `environment` / `config`)
- Todo `bug.md` tem `## Regression Test Slot` não-vazio após refinamento (`status != scaffold-ready`)

Integrado ao CI como auditoria Camada 2 (Rule 26 §Auditoria Camada 2).

---

## 10. Ações de Remediação

| ID | Finding | Severidade | Área | Remediação | Owner | Prazo | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| REM-001 | `bug-lifecycle.yaml` deve ser criado (task-0080-0001-002) ANTES dos artefatos que o referenciam, ou o CI `audit-capability-coverage.sh` falhará | Medium | Rule 28 | Garantir que task-0080-0001-002 seja executada primeira na sequência de implementação | Implementador | Antes do PR merge | Open |
| REM-002 | Slug de `description` com apenas separadores (ex: `"///"`): exit code 2 (`ARGS_INVALID`) deve ser testado explicitamente em AC-4 e task-0080-0001-004 | Low | CWE-22 | Adicionar caso de teste unitário com input `"///"` e verificar exit code 2 | QA (task-0080-0001-004) | Durante implementação | Open |
| REM-003 | Telemetria fail-open: verificar que ausência de `telemetry-phase.sh` no PATH não aborta skill | Low | ADR-0005 | Adicionar `|| true` (ou verificação condicional) nas chamadas de `telemetry-phase.sh` no SKILL.md | Implementador (task-0080-0001-003) | Durante implementação | Open |

---

## 11. DoD Compliance Checklist

### Global DoD (EPIC-0080)

- [ ] `_TEMPLATE-BUG.md` gerado em `.claude/templates/` com 9 seções RA9 v2 + seções bug-specific
- [ ] `x-create-bug/SKILL.md` gerado em `.claude/skills/` com frontmatter v3.0
- [ ] `bug-lifecycle.yaml` existe em `capabilities/governance/`
- [ ] `audit-bug-classification.sh` gerado em `.claude/scripts/` e executável
- [ ] `audit-capability-coverage.sh` executa sem violação sobre os novos artefatos
- [ ] `LifecycleAuditCli scan` retorna exit 0 sobre `x-create-bug/SKILL.md`
- [ ] Refinement verdict `status: approved` presente em story-0080-0001.md (CONFIRMADO)

### Story DoD Local (story-0080-0001)

- [ ] AC-1 (happy-path): `bug.md` criado com frontmatter v3.0, branch e PR abertos — verificado por `BugCreationSmokeIT`
- [ ] AC-2 (dirty worktree): exit 15 (`WORKTREE_AMBIGUOUS`), sem side-effects — verificado por smoke test
- [ ] AC-3 (performance): P95 ≤ 90s em modo não-interativo — `time` wrapper em smoke test
- [ ] AC-4 (path traversal): slug `../../../etc/passwd` → branch válido; `///` → exit 2 — verificado por testes unitários (task-0080-0001-004)
- [ ] `README.md` skill index atualizado com `x-create-bug` (task-0080-0001-006)
- [ ] 6 artefatos Fase 1 em `plans/` presentes antes do PR merge
- [ ] 4 artefatos Fase 3 em `reports/` presentes antes do PR merge
- [ ] Eventos de telemetria registrados em `events.ndjson` após execução do `BugCreationSmokeIT`

---

## 12. Sign-off

| Role | Nome | Decisão | Data | Notas |
| :--- | :--- | :--- | :--- | :--- |
| Security Engineer | AI-assisted (x-internal-build-story-plan) | **Approved with Conditions** | 2026-05-08 | Condições: REM-001 (ordem de tasks), REM-002 (caso de teste AC-4 vazio), REM-003 (fail-open telemetria) devem ser resolvidas durante implementação. Nenhum blocker regulatório identificado (zero PII, zero frameworks de proteção de dados acionados). |
| Compliance Officer | — | Pending | — | Revisão de REM-001 a REM-003 após implementação |
| Tech Lead | — | Pending | — | Sign-off após smoke test AC-1 a AC-4 executados com sucesso |
