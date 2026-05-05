# SPEC-flat-architecture-decision-engine-v1 — Perfis de Arquitetura de Produto e Motor de Decisão

> **Status:** Rascunho  
> **Autor:** GitHub Copilot CLI  
> **Date:** 2026-05-04  
> **Branch:** docs/feature-flat-architecture-decision-engine

---

## Sistema

Esta feature define um modelo de governança apenas para **arquitetura de produto de software**. Ela exclui explicitamente solution architecture, desenho de landscape corporativo, platform architecture e blueprints de integração em nível organizacional. O objetivo é classificar quanta estrutura interna um produto de software, módulo ou serviço deve carregar e evoluir essa decisão com base em complexidade mensurável, e não em ideologia, moda ou viés de framework.

O modelo separa dois eixos independentes. O primeiro eixo é a **topologia do produto**: aplicação única, modular monolith, federação de serviços ou microservice. O segundo eixo é a **intensidade arquitetural**: **Flat Architecture** para estruturas internas enxutas, **Structured Architecture** para estrutura explícita e equilibrada, e **Hardened Architecture** para estruturas internas fortemente isoladas e altamente governadas. Como os eixos são independentes, um microservice pode ser Flat, Structured ou Hardened internamente, e o mesmo vale para módulos dentro de um modular monolith.

Para tornar o modelo operacional, a feature também introduz **pattern packs** e **rules packs**. Hexagonal, Tactical DDD, CQRS, Event Sourcing e abordagens similares são tratados como pacotes seletivos que podem ser associados a uma combinação de topologia e intensidade quando sinais mensuráveis justificarem isso. SOLID e restrições de design relacionadas continuam sendo regras transversais, e não escolhas de topologia. Em seguida, um score de complexidade de produto de software passa a alimentar o motor de recomendação com dimensões explícitas, thresholds e sinais de promoção.

---

## Escopo

### Incluído

- Definir três faixas formais de intensidade arquitetural: Flat Architecture, Structured Architecture e Hardened Architecture.
- Separar decisões de topologia da intensidade arquitetural para que monoliths, modular monoliths e microservices possam usar perfis internos distintos.
- Estabelecer padrões explícitos de módulo para cada faixa, incluindo profundidade estrutural permitida, formato de API pública de módulo, abstrações permitidas e restrições de shared kernel.
- Definir contratos de pattern packs para Hexagonal, Tactical DDD, CQRS, Event Sourcing e abordagens correlatas, incluindo componentes obrigatórios, elementos opcionais e anti-goals.
- Definir rules packs transversais para SOLID e princípios de design semelhantes, aplicáveis independentemente da topologia.
- Introduzir um **Software Product Complexity Score (SPCS)** mensurável, com dimensões ponderadas, thresholds e lógica de recomendação.
- Preparar saídas de perfil prontas para configuração e artefatos de decisão que depois possam ser representados em regras do gerador, YAML ou checklists de policy.

### Excluído

- Tratar solution architecture, enterprise architecture ou platform architecture como parte deste modelo.
- Tornar Hexagonal, DDD, Microservices, CQRS ou Event Sourcing defaults obrigatórios para todo produto ou módulo.
- Usar nomes de topologia como "microservice" ou "monolith" como proxy para definir o quão inflada ou sofisticada a arquitetura interna deve ser.

---

## Regras

| ID | Regra | Impacto |
|----|------|--------|
| RULE-001 | Este modelo governa apenas arquitetura de produto de software; ele não define solution architecture, platform architecture nem mapas de integração corporativa. | Evita diluição de escopo e mantém as recomendações focadas na estrutura interna da aplicação. |
| RULE-002 | A seleção de arquitetura deve usar dois eixos independentes: topologia do produto e intensidade arquitetural. | Evita equivalência falsa entre forma de deploy e complexidade de design interno. |
| RULE-003 | As faixas formais de intensidade são Flat Architecture, Structured Architecture e Hardened Architecture. | Cria um vocabulário estável para recomendação, governança e definição de perfis. |
| RULE-004 | Microservices, modular monoliths e sistemas com deploy único podem usar estruturas internas Flat, Structured ou Hardened conforme a necessidade medida. | Permite combinações como flat microservices, structured monoliths ou módulos hardened. |
| RULE-005 | Cada faixa de intensidade deve definir um padrão obrigatório de módulo cobrindo nomenclatura, profundidade de pacotes, fronteiras de API pública, restrições de shared kernel e orçamento de abstração. | Torna a estrutura modular explícita e comparável entre produtos e times. |
| RULE-006 | Pattern packs como Hexagonal, Tactical DDD, CQRS e Event Sourcing devem declarar componentes obrigatórios, componentes opcionais, contextos permitidos, gatilhos de promoção e anti-patterns. | Torna cada abordagem operacional em vez de retórica ou excessivamente aplicada. |
| RULE-007 | SOLID e princípios de design semelhantes são rules packs transversais, não escolhas de topologia nem faixas arquiteturais por si só. | Evita misturar disciplina de design com decisões de deploy ou camadas. |
| RULE-008 | O Software Product Complexity Score (SPCS) deve ser calculado a partir de fatores ponderados do produto, como densidade de regras de domínio, acoplamento de workflow, volatilidade de integração, pressão de compliance/auditoria, criticidade de runtime, assimetria de escala, autonomia de times, independência de deploy, divergência de canais e assimetria entre leitura e escrita. | Cria uma entrada mensurável para recomendações arquiteturais. |
| RULE-009 | A lógica de recomendação deve primeiro calcular a faixa de intensidade com base no SPCS e em condições eliminatórias, e só depois avaliar separadamente a topologia apropriada. | Produz orientação mais clara e evita inflar a arquitetura por causa de um único sinal isolado. |
| RULE-010 | Promoções arquiteturais, hardening e exceções devem ser documentados com sinais explícitos, thresholds e rationale. | Preserva rastreabilidade, capacidade de revisão e evolução disciplinada ao longo do tempo. |

---

## Histórias

Backlog preliminar para decomposição posterior em `x-create-feature`:

| # | Título | Responsável |
|---|-------|-------------|
| 1 | Definir a taxonomia formal das faixas de intensidade arquitetural Flat, Structured e Hardened | Arquiteto de Software |
| 2 | Separar escolhas de topologia do produto da intensidade arquitetural interna no motor de decisão | Líder Técnico |
| 3 | Definir o padrão de módulo para Flat Architecture, incluindo módulos rasos por capacidade e baixo orçamento de abstração | Desenvolvedor |
| 4 | Definir o padrão de módulo para Structured Architecture, incluindo APIs explícitas de módulo, ports seletivos e layering equilibrado | Arquiteto de Software |
| 5 | Definir o padrão de módulo para Hardened Architecture, incluindo fronteiras fortes, seams auditáveis e governança mais rígida | Arquiteto de Software |
| 6 | Criar um template de contrato de pattern pack para Hexagonal, Tactical DDD, CQRS, Event Sourcing e abordagens semelhantes | Engenheiro de Plataforma |
| 7 | Definir rules packs transversais para SOLID e restrições de design relacionadas | Líder Técnico |
| 8 | Implementar o Software Product Complexity Score com dimensões ponderadas, thresholds e exemplos de calibração | Arquiteto de Software |
| 9 | Gerar saídas de recomendação que sugiram independentemente a topologia e a faixa de intensidade | Engenheiro de Plataforma |
| 10 | Definir regras de promoção de Flat para Structured para Hardened e de deploy único para topologia modular ou distribuída | Líder Técnico |
| 11 | Documentar como exceções arquiteturais, desvios de módulo e pacotes especializados são registrados e revisados | Líder Técnico |

---

## DoR / DoD

### Definição de Pronto

- [ ] Topologia e intensidade arquitetural estão explicitamente separadas e nomeadas sem ambiguidade.
- [ ] Os nomes propostos das faixas de intensidade e suas semânticas foram aceitos pelos stakeholders.
- [ ] O template de padrão de módulo está definido tanto para as faixas arquiteturais quanto para os pattern packs.
- [ ] As dimensões, pesos, thresholds e método de calibração do SPCS estão explícitos o suficiente para revisão objetiva.
- [ ] Os operadores pretendidos do motor de recomendação e do fluxo de governança foram identificados.

### Definição de Concluído

- [ ] A feature distingue arquitetura de produto de software de solution architecture tanto na linguagem quanto na lógica de recomendação.
- [ ] O motor de recomendação produz separadamente topologia e intensidade arquitetural, com combinações válidas como flat microservices ou módulos hardened em monolith.
- [ ] Flat Architecture, Structured Architecture e Hardened Architecture possuem padrões explícitos de módulo, orçamentos de abstração e sinais de promoção.
- [ ] Pattern packs e rules packs estão definidos com elementos obrigatórios, contextos permitidos e limites de anti-patterns.
- [ ] O SPCS é mensurável, revisável e vinculado a thresholds explícitos e resultados de recomendação.

---

## Riscos

| Risco | Impacto | Mitigação |
|------|--------|------------|
| Times confundem topologia com intensidade arquitetural e assumem que todo microservice precisa ser fortemente sofisticado internamente. | Alto | Separar topologia e intensidade no modelo, nos exemplos e na saída de recomendação para que as combinações permaneçam explícitas. |
| Pattern packs e rules packs tornam-se catálogos burocráticos que reintroduzem inflação arquitetural. | Alto | Exigir que cada pack declare quando não deve ser usado, qual problema resolve e quais sinais justificam sua promoção. |
| O score de complexidade torna-se subjetivo ou fácil de manipular. | Médio | Usar dimensões ponderadas, exemplos de calibração, scoring baseado em evidência e critérios explícitos de revisão para cada threshold. |
