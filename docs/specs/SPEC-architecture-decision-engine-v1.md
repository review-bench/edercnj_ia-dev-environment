# SPEC-architecture-decision-engine-v1 — Architecture Decision Engine para Arquitetura de Produto de Software

> **Status:** Rascunho  
> **Autor:** GitHub Copilot CLI  
> **Data:** 2026-05-04  
> **Branch:** `docs/feature-architecture-decision-engine`

---

## Sistema

Esta SPEC define a feature **architecture-decision-engine**: um motor de decisão para escolher a arquitetura mais adequada de uma nova feature, produto, módulo ou serviço de software. O objetivo não é premiar a arquitetura "mais sofisticada", mas recomendar a **menor arquitetura capaz de resolver o problema com segurança, clareza, evolução e governança**.

O documento trata **somente de arquitetura de produto de software**. Ele não cobre solution architecture, enterprise architecture, desenho de plataforma, topologia de times ou arquitetura de integração em nível corporativo, exceto quando esses fatores impactam diretamente a estrutura interna do software avaliado.

O motor separa dois eixos que costumam ser misturados incorretamente:

1. **Topologia do produto** — aplicação única, Modular Monolith, microservice ou federação de serviços.
2. **Intensidade arquitetural interna** — **Flat Architecture**, **Structured Architecture** ou **Hardened Architecture**.

Essa separação é central. Um microservice pode ter internals Flat, Structured ou Hardened. Um Modular Monolith também. O erro comum é usar nomes de topologia como sinônimo de sofisticação arquitetural; esta SPEC elimina essa ambiguidade.

### Tese central

Arquitetura deve ser:

- **proporcional ao problema**;
- **explícita nas fronteiras que importam**;
- **evolutiva por necessidade**, não por antecipação;
- **econômica em camadas, contratos e indireção**;
- **rigorosa quando risco, compliance, escala ou instabilidade justificarem**.

### Resultado esperado do motor

O `architecture-decision-engine` deve produzir uma recomendação explícita no formato:

- **topologia recomendada**;
- **intensidade arquitetural recomendada**;
- **pattern packs** necessários;
- **rules packs** obrigatórios;
- **padrão de módulos** aplicável;
- **sinais de promoção** para evoluções futuras;
- **anti-patterns** a evitar.

### Premissas fundamentais

1. **Bounded Context não é microservice.** Bounded Context é fronteira semântica e de modelo; microservice é fronteira operacional e de deploy.
2. **Composição híbrida é válida.** Um sistema pode ter um núcleo mais isolado e zonas periféricas mais flat, desde que a fronteira seja explícita.
3. **Patterns não são defaults universais.** Hexagonal, Tactical DDD, CQRS e Event Sourcing são instrumentos seletivos, não dogmas.
4. **A arquitetura precisa ser consumível por humanos e IA.** Custo cognitivo, boilerplate e custo de contexto são restrições legítimas.

---

## Escopo

### Incluído

- Definição formal do motor de decisão arquitetural para novos projetos, features e módulos.
- Separação explícita entre topologia do produto e intensidade arquitetural interna.
- Catálogo dos três perfis de intensidade: Flat Architecture, Structured Architecture e Hardened Architecture.
- Catálogo das topologias: aplicação única, Modular Monolith, microservice e federação de serviços.
- Definição de padrões de módulo por perfil arquitetural.
- Definição de **pattern packs** para Hexagonal, Tactical DDD, CQRS e Event Sourcing.
- Definição de **rules packs** transversais, incluindo SOLID.
- Definição do **Software Product Complexity Score (SPCS)**, com dimensões, pesos, thresholds e regras eliminatórias.
- Definição de regras de promoção entre perfis e de combinações híbridas válidas.
- Estrutura do documento em formato consumível por `x-ideate-feature` e `x-create-feature`.

### Excluído

- Decisões de solution architecture, arquitetura corporativa ou arquitetura de plataforma fora do escopo do software avaliado.
- Catálogo completo de frameworks, vendors ou stacks específicos.
- Mapeamento automático de todo Bounded Context para microservice.
- Adoção obrigatória de Hexagonal, DDD, CQRS ou Event Sourcing como defaults.
- Regras acopladas ao tree atual do repositório quando elas não forem generalizáveis para a feature.

---

## Regras

| ID | Regra | Impacto |
|----|-------|---------|
| RULE-001 | O motor governa arquitetura de produto de software, não solution architecture. | Mantém o escopo coerente e evita que a feature vire um framework genérico de arquitetura corporativa. |
| RULE-002 | Toda recomendação deve separar **topologia** de **intensidade arquitetural interna**. | Evita confundir formato de deploy com profundidade de design. |
| RULE-003 | O default é a menor arquitetura suficiente para o problema; promoção estrutural exige sinais explícitos. | Reduz overengineering e preserva evolução incremental. |
| RULE-004 | Bounded Context deve ser tratado primeiro como fronteira de modelo e ownership, não como microservice obrigatório. | Evita distribuição prematura e fragmentação indevida do domínio. |
| RULE-005 | Composições híbridas são válidas, desde que as zonas e seus contratos sejam explícitos. | Permite combinar núcleo mais isolado com zonas mais flat sem ambiguidade de regra. |
| RULE-006 | Cada perfil arquitetural deve definir um padrão obrigatório de módulo. | Garante consistência estrutural dentro de cada recomendação. |
| RULE-007 | Cada pattern pack deve declarar componentes obrigatórios, componentes opcionais, critérios de entrada, critérios de não uso e anti-patterns. | Evita uso retórico ou indiscriminado de patterns. |
| RULE-008 | Rules packs, como SOLID, são transversais e independentes da topologia. | Evita tratar princípios de design como se fossem estilos arquiteturais. |
| RULE-009 | O SPCS deve ser calculado com dimensões explícitas, pesos claros e thresholds revisáveis. | Torna a decisão auditável, calibrável e menos subjetiva. |
| RULE-010 | Regras eliminatórias têm precedência sobre o score agregado quando houver risco regulatório, necessidade real de deploy independente, múltiplas bordas instáveis ou forte assimetria leitura/escrita. | Impede que um score mediano esconda sinais críticos. |
| RULE-011 | Novos módulos e serviços devem começar no perfil mais simples compatível e evoluir por promoção, não por cerimônia antecipada. | Favorece clareza, velocidade e optionalidade. |
| RULE-012 | `shared` ou equivalente deve permanecer mínimo e estritamente transversal em qualquer perfil. | Evita erosão de fronteiras e acoplamento acidental entre contextos. |

### 1. Perfis de intensidade arquitetural

| Perfil | Quando usar | Padrão obrigatório de módulo | O que evitar por default |
|--------|-------------|------------------------------|--------------------------|
| **Flat Architecture** | Problemas com baixa a moderada complexidade estrutural, poucas integrações relevantes, um time coordenado, um deploy aceitável e forte pressão por simplicidade. | Organização por capacidade/feature, profundidade rasa, API pública clara por módulo, dependências diretas e intencionais, baixo orçamento de abstração. | Pacotes globais por tipo de classe, interfaces 1:1 sem motivo, base classes genéricas, ports/adapters preventivos em todo lugar. |
| **Structured Architecture** | Problemas com múltiplos módulos relevantes, mais de uma borda sensível, crescimento contínuo, necessidade de contratos internos mais claros e alguma governança adicional. | API explícita de módulo, subdivisões internas moderadas (`api`, `application`, `core`, `data` ou equivalente), ports seletivos, contratos internos mais estáveis, orçamento moderado de abstração. | Hexagonal completa em todos os módulos, microservices por antecipação, duplicação de contratos sem justificativa. |
| **Hardened Architecture** | Problemas com alta criticidade, compliance forte, múltiplas bordas instáveis, múltiplos adapters relevantes, alto custo de falha, necessidade de rastreabilidade e substituibilidade elevadas. | Fronteiras fortes, ports/adapters explícitos onde necessário, testes de contrato, observabilidade e governança reforçadas, aplicação seletiva de Tactical DDD, anti-corruption layers e contratos de integração bem definidos. | Cerimônia em áreas triviais, abstrações sem evidência de valor, propagação do hardening para todo o sistema sem hotspot real. |

### 2. Topologias do produto

| Topologia | Quando é a melhor hipótese inicial | Observação |
|-----------|------------------------------------|------------|
| **Aplicação única** | Escopo ainda em descoberta, uma equipe, baixo custo de coordenação, um deploy aceitável. | Pode ser Flat, Structured ou Hardened internamente. |
| **Modular Monolith** | Há múltiplos contextos claros, mas ainda sem razão operacional suficiente para distribuir. | É o melhor default para escalar fronteiras sem pagar custo distribuído cedo demais. |
| **microservice** | Há necessidade real de deploy independente, ownership forte, borda operacional clara ou assimetria forte de escala/falha. | A arquitetura interna do serviço pode continuar Flat no início. |
| **Federação de serviços** | O produto já exige múltiplos serviços coexistindo com governança e contratos explícitos. | A complexidade principal passa a estar em contratos, plataforma e operação distribuída. |

### 3. Pattern packs

| Pack | Componentes obrigatórios | Quando aplicar | Quando não aplicar |
|------|--------------------------|----------------|--------------------|
| **Hexagonal** | Ports, adapters, composition root, casos de uso claros, testes de contrato ou adapter tests. | Bordas instáveis, múltiplos adapters relevantes, substituibilidade importante, isolamento de I/O. | CRUD simples, uma única implementação estável, baixa variabilidade de borda. |
| **Tactical DDD** | Entities, Value Objects, Aggregates, Repositories, Domain Services, linguagem ubíqua e Bounded Contexts explícitos. | Domínio denso, regras invariantes importantes, alto valor semântico, mudança frequente de regra de negócio. | Domínio raso, workflow simples e baixo ganho semântico. |
| **CQRS** | Command model, Query model, handlers, políticas de consistência e projeções. | Forte assimetria entre leitura e escrita, modelos de leitura muito diferentes, otimização clara de throughput. | Apenas porque "parece mais escalável" ou por preferência estética. |
| **Event Sourcing** | Eventos imutáveis como source of truth, replay, projeções e versionamento de eventos. | Auditoria forte, replay com valor de negócio, trilha histórica como ativo central. | Quando a trilha de evento não traz valor real e apenas aumenta complexidade operacional. |

### 4. Rules packs transversais

#### SOLID

SOLID deve ser tratado como um **rules pack** transversal, aplicável a qualquer topologia e a qualquer perfil de intensidade:

- **SRP** — um módulo, classe ou componente deve ter uma razão principal para mudar;
- **OCP** — extensões preferem composição e contratos claros a reescritas invasivas;
- **LSP** — abstrações precisam manter contrato sem surpresa semântica;
- **ISP** — interfaces devem ser pequenas e focadas;
- **DIP** — dependências devem apontar para contratos estáveis quando a substituibilidade justificar.

#### Regras transversais mínimas

- nomeação por capacidade e responsabilidade;
- fronteiras explícitas entre módulos;
- `shared` mínimo;
- dependências entre módulos via API pública, eventos ou contratos claros;
- documentação de exceções arquiteturais.

### 5. Padrões de módulo por perfil

#### 5.1 Flat Architecture

Estrutura canônica:

```text
src/
  app/
  shared/
  billing/
    BillingController
    BillingService
    BillingPolicy
    BillingRepository
    BillingDto
```

Regras:

- organização por feature/capacidade;
- profundidade rasa;
- controller/service/policy/repository podem coexistir no mesmo módulo se a coesão continuar alta;
- ports/adapters só entram quando houver ganho real;
- um módulo cresce para uma subdivisão leve (`api`, `core`, `data`) antes de virar cerimonial.

#### 5.2 Structured Architecture

Estrutura canônica:

```text
src/
  billing/
    api/
    application/
    core/
    data/
```

Regras:

- API do módulo explícita;
- contratos internos e DTOs delimitados;
- ports seletivos em bordas instáveis;
- testes de módulo e contratos internos mais fortes;
- clareza de ownership entre capacidades.

#### 5.3 Hardened Architecture

Estrutura canônica:

```text
src/
  billing/
    domain/
    application/
    infrastructure/
      input/
      output/
```

Regras:

- fronteiras fortes entre domínio e bordas;
- ports e adapters explícitos;
- Tactical DDD onde houver densidade de domínio;
- observabilidade, testes de contrato e rastreabilidade reforçados;
- anti-corruption layers quando integrações externas justificarem.

### 6. Software Product Complexity Score (SPCS)

Cada dimensão recebe nota **0, 1, 2 ou 3**, onde 0 significa baixa pressão estrutural e 3 significa pressão máxima.

| Dimensão | Peso | Descrição |
|----------|------|-----------|
| Densidade de regras de domínio | 3 | Quantidade e volatilidade de invariantes e políticas de negócio. |
| Acoplamento de workflow | 2 | Quantas etapas, orquestrações e dependências internas precisam permanecer consistentes. |
| Volatilidade e multiplicidade de integrações | 3 | Quantidade de integrações relevantes e frequência de mudança em suas bordas. |
| Pressão de compliance/auditoria | 3 | Necessidade de rastreabilidade, trilha, isolamento e controle regulatório. |
| Criticidade operacional | 2 | Custo da falha, sensibilidade a indisponibilidade e impacto de erro. |
| Assimetria de escala/performance | 2 | Desbalanceamento entre caminhos críticos, throughput e latência. |
| Autonomia de times/ownership | 2 | Nível de independência organizacional exigido pelos contextos. |
| Independência de deploy | 3 | Necessidade real de deploy isolado por contexto ou serviço. |
| Divergência de canais | 1 | Quanto web, API, CLI, eventos ou outros canais exigem comportamentos distintos. |
| Assimetria leitura vs escrita | 2 | Grau de diferença entre modelo transacional e modelo ideal de leitura. |

Fórmula:

> **SPCS = Σ (nota da dimensão × peso da dimensão)**  
> Score máximo = 69

Thresholds iniciais:

| Faixa SPCS | Intensidade sugerida |
|------------|----------------------|
| 0–18 | **Flat Architecture** |
| 19–38 | **Structured Architecture** |
| 39–69 | **Hardened Architecture** |

### 7. Regras eliminatórias

Antes de aceitar o perfil sugerido apenas pelo SPCS, aplicar:

1. **Compliance/auditoria = 3** com requisito forte de isolamento → mínimo **Structured Architecture**; avaliar **Hardened Architecture** se também houver múltiplas bordas críticas.
2. **Independência de deploy = 3** + ownership forte + benefício operacional claro → avaliar topologia `microservice` ou federação de serviços.
3. **Volatilidade de integrações = 3** com múltiplos adapters relevantes → exigir **Hexagonal** no hotspot afetado.
4. **Assimetria leitura vs escrita = 3** → avaliar **CQRS**; se replay/auditoria for central, avaliar também **Event Sourcing**.

### 8. Composições híbridas válidas

O motor deve aceitar explicitamente composições como:

- aplicação única com maioria Flat e um módulo Hardened;
- Modular Monolith com módulos Flat e um núcleo Structured/Hexagonal;
- microservice com internals Flat;
- federação de serviços em que cada serviço escolhe sua própria intensidade;
- domínio crítico Hardened cercado por tooling ou delivery zones mais Flat.

### 9. Saída do motor de decisão

A recomendação final deve ser explícita neste formato:

```text
Topologia: <aplicação única | Modular Monolith | microservice | federação de serviços>
Intensidade: <Flat Architecture | Structured Architecture | Hardened Architecture>
Pattern packs: <Hexagonal, Tactical DDD, CQRS, Event Sourcing, nenhum, ...>
Rules packs: <SOLID, regras transversais mínimas, ...>
Padrão de módulos: <resumo aplicável>
Sinais de promoção: <gatilhos objetivos>
Anti-patterns a evitar: <lista objetiva>
```

### 10. Caminho evolutivo recomendado

Em termos gerais, a evolução padrão deve ser:

1. começar simples;
2. modularizar sem distribuir;
3. endurecer hotspots;
4. extrair serviços apenas quando o problema existir;
5. aplicar CQRS/Event Sourcing apenas quando o contexto pedir.

Trajetórias válidas:

- **Flat Architecture → Structured Architecture → Hardened Architecture**
- **aplicação única → Modular Monolith → microservice/federação de serviços**

As duas trajetórias são independentes, embora se influenciem.

---

## Histórias

Lista preliminar para decomposição posterior em `x-create-feature`:

| # | Título | Stakeholder |
|---|--------|-------------|
| 1 | Definir o modelo conceitual do architecture-decision-engine e seu output formal | Arquiteto de Software |
| 2 | Definir o catálogo de topologias do produto e suas condições de uso | Arquiteto de Software |
| 3 | Definir o catálogo de perfis de intensidade arquitetural | Arquiteto de Software |
| 4 | Definir o padrão de módulos para Flat Architecture | Desenvolvedor |
| 5 | Definir o padrão de módulos para Structured Architecture | Arquiteto de Software |
| 6 | Definir o padrão de módulos para Hardened Architecture | Arquiteto de Software |
| 7 | Definir o pattern pack de Hexagonal com critérios de entrada e não uso | Líder Técnico |
| 8 | Definir os pattern packs de Tactical DDD, CQRS e Event Sourcing | Líder Técnico |
| 9 | Definir o rules pack transversal de SOLID e regras mínimas de fronteira | Líder Técnico |
| 10 | Definir e calibrar o Software Product Complexity Score (SPCS) | Arquiteto de Software |
| 11 | Implementar a lógica de recomendação com thresholds e regras eliminatórias | Engenheiro de Plataforma |
| 12 | Definir combinações híbridas válidas, sinais de promoção e anti-patterns | Arquiteto de Software |

### Dependências indicativas entre histórias

- Histórias 2 e 3 dependem da História 1.
- Histórias 4, 5 e 6 dependem da História 3.
- Histórias 7, 8 e 9 dependem das Histórias 1 e 3.
- História 10 depende da História 1.
- História 11 depende das Histórias 2, 3, 7, 8, 9 e 10.
- História 12 depende da História 11.

---

## DoR / DoD

### Definition of Ready

- [ ] O documento distingue claramente arquitetura de produto de software de solution architecture.
- [ ] Topologia e intensidade arquitetural estão separadas de forma explícita e sem ambiguidade.
- [ ] Os três perfis arquiteturais possuem semântica clara e padrão mínimo de módulo.
- [ ] O SPCS possui dimensões, pesos, thresholds e regras eliminatórias revisáveis.
- [ ] Os pattern packs e rules packs possuem critérios claros de entrada, uso e não uso.
- [ ] As histórias preliminares estão concretas o suficiente para decomposição por `x-create-feature`.

### Definition of Done

- [ ] O motor produz uma recomendação completa de topologia, intensidade, packs, padrão de módulos e sinais de promoção.
- [ ] A SPEC está autocontida e não depende de um ADR separado para explicar composições híbridas ou transições arquiteturais.
- [ ] O documento evita duplicação entre racional, regras, catálogo e histórias.
- [ ] Flat Architecture, Structured Architecture e Hardened Architecture estão delimitadas por critérios objetivos e exemplos de módulo.
- [ ] O SPCS está descrito de forma mensurável e auditável.
- [ ] O documento continua legível como entrada única para `x-create-feature`.

---

## Riscos

| Risco | Impacto | Mitigação |
|-------|---------|-----------|
| O motor virar apenas um catálogo opinativo sem critérios realmente operacionais. | Alto | Exigir score explícito, regras eliminatórias, saída formal e critérios de promoção. |
| Misturar novamente topologia com intensidade arquitetural e reintroduzir ambiguidade. | Alto | Manter os dois eixos separados em toda a narrativa, tabelas e output do motor. |
| Transformar pattern packs em burocracia obrigatória para qualquer contexto. | Alto | Declarar claramente quando cada pack **não** deve ser usado. |
| O SPCS ser subjetivo demais ou fácil de manipular. | Médio | Usar pesos explícitos, thresholds versionáveis, exemplos de calibração e revisão por pares. |
| O documento absorver detalhes demais do repositório atual e perder generalidade como feature. | Médio | Manter apenas conceitos reutilizáveis e tratar exemplos do repositório como ilustrações, não como contrato universal. |
