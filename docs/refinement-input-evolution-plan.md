# Plano de Evolucao: Entradas de Refinement em Epic/Story Creation

## Problema

As skills `x-epic-create` e `x-story-create` no modo `--from-feature` foram desenhadas para omitir secoes que o refinement trata como obrigatorias ou bloqueantes.

Contradicoes atuais:

- `x-epic-create` e `x-internal-create-epic` omitem `## 2`, `## 4` e `## 8` em epicos derivados de feature.
- `x-story-create` e `x-internal-create-story` omitem `## 2`, `## 4` e `## 8` em stories derivadas de feature.
- `x-refine-epic` exige Persona, Hipotese de Valor, OKRs/KPIs, Alternativas, Riscos e Escopo.
- `x-refine-story` exige Persona, Valor, AC, Contratos, Metricas e Alternativas.
- O template de epico pedia OKR/KPI, mas nao expunha explicitamente o metodo de medicao cobrado pelo refinement.

Efeito observado:

- refinement encontra gaps recorrentes logo apos a criacao do artefato
- o operador responde perguntas que deveriam ter sido resolvidas na geracao
- epicos e stories nascem com baixa taxa de aprovacao no primeiro refinement

## Objetivo

Fazer com que artefatos gerados por `x-epic-create` e `x-story-create` ja saiam com o conjunto minimo de dados exigido por `x-refine-epic` e `x-refine-story`, reduzindo refinements rejeitados por ausencia estrutural.

## Principios

- Product-First lineage e complementar, nao substitui contexto de refinement.
- Secoes obrigatorias para refinement nunca devem ser omitidas em `--from-feature`.
- Se a fonte nao trouxer evidencia suficiente, a skill deve falhar com erro de validacao em vez de gravar placeholders.
- A validacao de completude deve ocorrer antes do write final.

## Evolucao Proposta

### Fase 1 — Corrigir contrato de geracao

- Remover das skills publicas e internas a instrucao de omitir secoes criticas para refinement.
- Tornar obrigatorio preencher, em epicos derivados de feature:
  - Persona & Stakeholders
  - Hipotese & OKRs
  - Alternativas Consideradas
  - Riscos
  - Quality Gates
- Tornar obrigatorio preencher, em stories derivadas de feature:
  - Persona & Cenario
  - Entrega de Valor com metrica observavel
  - AC Gherkin completas
  - Contratos tipados
  - Decision Rationale

### Fase 2 — Gate de completude pre-write

- Adicionar checklist de validacao nas skills internas:
  - epic: `problem`, `persona`, `value`, `okrs`, `alternatives`, `risks`, `scope`
  - story: `persona`, `value`, `ac`, `contracts`, `metrics`, `alternatives`
- Em caso de falta material, abortar com erro explicito citando a dimensao faltante.

### Fase 3 — Enriquecimento orientado por fonte

- Ao gerar a partir de feature, coletar contexto tambem de capability/product quando disponiveis.
- Mapear systematicamente:
  - personas a partir de atores, stakeholders e ownership declarados
  - OKRs/KPIs a partir de objetivos, sucesso esperado e sinais observaveis
  - alternativas a partir de limites de escopo e decisoes rejeitadas/upstream constraints
  - risks a partir de dependencias, RNFs e integracoes externas

### Fase 4 — Templates alinhados ao gate

- Manter os templates como surface canonica das informacoes exigidas.
- No template de epico, explicitar `Metodo de Medicao` para cada KR/KPI.
- Evitar secoes paralelas que escondam informacao fora do template padrao.

### Fase 5 — Auditoria automatizada

- Adicionar teste/auditoria textual para detectar regressao quando alguma skill de criacao:
  - instruir omissao de secao obrigatoria de refinement
  - escrever epic/story sem declarar validacao de completude
- Opcionalmente, validar exemplos/golden gerados por `--from-feature` contra as dimensions de refinement.

## Criterios de Sucesso

- `x-epic-create --from-feature` nao gera epico sem Persona, OKR/KPI mensuravel, Alternativas, Riscos e Quality Gates.
- `x-story-create --from-feature` nao gera story sem Persona, Metricas, AC completas, Contratos e Decision Rationale.
- refinements deixam de falhar por ausencia estrutural recorrente e passam a focar em qualidade do conteudo.

## Backlog Recomendado

1. Criar auditoria CI para contrato criacao -> refinement.
2. Atualizar exemplos/golden de epic/story derivadas de feature.
3. Medir taxa de aprovacao no primeiro refinement antes/depois da mudanca.
