# Instruções para Revisões do GitHub Copilot

> Este arquivo é lido automaticamente pelo GitHub Copilot ao fazer code review
> em pull requests deste repositório (`ia-dev-environment`). Ele complementa o
> contexto carregado de `.claude/rules/` e `CLAUDE.md`.

## Idioma das Revisões — OBRIGATÓRIO

**Você DEVE responder exclusivamente em português brasileiro.**
Nunca use inglês em comentários, descrições de issues ou sugestões.
Identificadores de código (nomes de classes, métodos, variáveis), termos
técnicos consagrados (commit, merge, hash, branch), palavras-chave de
linguagem e nomes próprios permanecem em sua forma original.

Exemplo correto:
> "O método `processTransaction` ultrapassa o limite de 25 linhas (Rule 03).
> Considere extrair a validação para um método auxiliar."

Exemplo incorreto:
> "The method `processTransaction` exceeds the 25-line limit. Consider
> extracting validation into a helper."

Acentuação completa é obrigatória — nunca substitua caracteres acentuados
por equivalentes ASCII (`não`, não `nao`; `é`, não `e`).

## Identidade do Projeto

- **Repositório:** `ia-dev-environment` — CLI gerador (binário publicado como
  `ia-dev-env`) que produz o diretório `.claude/` e demais artefatos de
  governança a partir de um YAML de configuração.
- **Pacote Java:** `dev.iadev` — todo código-fonte vive sob
  `java/src/main/java/dev/iadev/**`.
- **Stack:** Java 21, Picocli 4.7, Maven.
- **Arquitetura:** Hexagonal (Ports & Adapters). Domínio puro, sem dependências
  externas além da biblioteca padrão.
- **Propósito:** Ler YAML de configuração e gerar o diretório `.claude/`
  (skills, rules, hooks, agents) para o projeto-alvo.
- **Nota sobre identidade:** o template do gerador ainda referencia o
  placeholder `my-java-cli` em `.claude/rules/01-project-identity.md` — esse
  nome é apenas marcador do template e NÃO se aplica a este repositório.
  Use sempre `ia-dev-environment` ao se referir ao projeto.

## Foco da Revisão (em ordem de prioridade)

1. **Segurança — Rule 06 + Rule 12.** Sinalize SEMPRE:
   - Concatenação de SQL com input do usuário (CWE-89).
   - `Math.random()` para tokens/IDs sensíveis (CWE-330).
   - Credenciais em código (CWE-798).
   - Deserialização sem `ObjectInputFilter` (CWE-502).
   - `TrustManager` que aceita qualquer certificado (CWE-295).
   - Uso de `new File(userInput)` sem normalização de path (CWE-22).
   - Mensagens de exceção expostas em respostas HTTP (CWE-209).
   - CORS com `allowedOrigins("*")` + `allowCredentials(true)` (CWE-942).

2. **Cobertura de testes — Rule 05 (Quality Gates).**
   Coverage é gate absoluto: ≥95% linha, ≥90% branch. Mesmo déficits
   pré-existentes precisam ser corrigidos no PR. Sinalize:
   - Classes/métodos novos sem teste correspondente.
   - Asserções fracas (`assertNotNull` sozinho não é suficiente).
   - Nomes de teste fora do padrão `[método]_[cenário]_[comportamento]`.
   - Uso de `Thread.sleep()` para sincronização (use polling com timeout).

3. **Padrões de código — Rule 03.** Limites rígidos:
   - Método/função ≤ 25 linhas.
   - Classe/módulo ≤ 250 linhas.
   - Parâmetros ≤ 4 (acima disso, exigir parameter object).
   - Largura de linha ≤ 120 caracteres.
   - Train wreck (cadeia de chamadas) ≤ 2 níveis entre objetos.
   - Nunca retornar `null` — usar `Optional`, coleção vazia ou tipo Result.
   - Sem flags booleanas como parâmetro de método.
   - Sem `System.out`/`System.err` em código de produção (use logging).
   - Sem fully qualified class names quando um import resolve.

4. **Arquitetura — Rule 04.** Direção de dependências:
   `adapter.inbound → application → domain ← adapter.outbound`. Sinalize:
   - Imports de framework ou biblioteca externa em `domain.*`.
   - Adapter inbound chamando direto domínio sem passar pela `application`.
   - Tipos de adapter referenciados em `domain.*`.
   - Anotações de framework em classes de `domain.*`.

5. **Conventional Commits + branch naming — Rules 08, 09.**
   - Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `perf`.
   - Quebra de contrato exige `feat!:` / `fix!:` ou `BREAKING CHANGE:`.
   - Branches: `feature/*`, `fix/*`, `chore/*`, `epic/*`, `release/*`,
     `hotfix/*`. Lowercase com hífen, sem underscore/camelCase.

6. **TDD — Rule 03 + Rule 05.** Procure no histórico de commits do PR a
   sequência Red → Green → Refactor. Sinalize PRs onde a implementação
   aparece antes do teste correspondente no `git log`.

## Como Comentar

- **Seja específico:** aponte arquivo e linha.
- **Cite a regra:** "Rule 03", "Rule 06", "CWE-89" — facilita rastreabilidade.
- **Sugira correção concreta:** mostre o snippet ajustado quando possível.
- **Severidade implícita:** segurança e arquitetura = crítico; estilo = sugestão.
- **Tom direto, técnico, sem rodeios.** Nada de saudações ou agradecimentos.

## O Que NÃO Fazer

- **Não comente preferências estilísticas** já cobertas pelo formatador/linter
  (espaçamento, vírgulas, ordenação de imports).
- **Não duplique feedback do CI** (testes, lint, build) — assuma que o
  pipeline já reporta isso.
- **Não sugira refatorações fora do diff** do PR. Mantenha o escopo do que
  mudou.
- **Não peça mudanças em arquivos `.claude/`, `src/test/resources/golden/`
  ou `governance/baselines/*.txt`** — são gerados/imutáveis por contrato.
- **Não bloqueie PRs por nits.** Reserve "request changes" para violações
  de Rules 03–06, 12 ou bugs lógicos reais.

## Caminhos com Tratamento Especial

Todo código-fonte Java vive sob `java/src/main/java/dev/iadev/**`. Os
caminhos abaixo usam o pacote real do repositório — não há diretório
`adapter/` separado: os concretos ficam sob `infrastructure/` (port impls)
e `cli/` (entrada CLI).

- **`java/src/main/java/dev/iadev/domain/**`** (`model/`, `port/`,
  `service/`, `stack/`): pureza absoluta — zero imports de framework ou
  biblioteca de serialização. Se o domínio precisa de I/O, exigir port
  interface declarada em `domain/port/`.
- **`java/src/main/java/dev/iadev/cli/**`** (CLI inbound — Picocli):
  validação de entrada e tratamento de erro uniforme; nunca expor mensagem
  de exceção crua ao usuário. Deve chamar `application/`, nunca orquestrar
  `domain/` diretamente.
- **`java/src/main/java/dev/iadev/infrastructure/**`** (adapters outbound
  — implementações de port): I/O com timeouts explícitos, cleanup de
  recursos via `try-with-resources` ou `finally`, sem lógica de negócio.
- **`java/src/main/java/dev/iadev/application/**`**: orquestração de casos
  de uso. Pode depender de `domain/` (incluindo ports), nunca de
  `infrastructure/` ou `cli/` diretamente.
- **`java/src/test/**`**: nomes no padrão
  `[método]_[cenário]_[comportamento]`, asserções específicas
  (`assertThat(x).isEqualTo(y)`, não `isNotNull()`), arquivos > 250 linhas
  exigem `@Nested` / agrupamento por `@DisplayName`.
- **Audit scripts:**
  - **Templates fonte:** `java/src/main/resources/targets/claude/scripts/<stack>/*.sh.tpl`
    (ex.: `java-maven/`, `node/`, `go/`, `python/`, `_default/`). Mudanças
    em audits começam aqui.
  - **Saída gerada:** `.claude/scripts/audit-*.sh` — produzida pelo
    `ScriptsAssembler` durante `ia-dev-env generate`. Não editar diretamente.
  - **Contrato (Rule 26):** prefixo obrigatório `audit-`, exit codes
    padronizados (0/1/2/3), suporte obrigatório a `--self-check`.
  - **Violação de Rule 007:** qualquer arquivo `audit-*.sh` em `scripts/`
    na raiz do repositório é proibido — `CiPipelineLeanSmokeIT` falha o
    build. Sinalize esse caso explicitamente.
- **`.claude/`, `src/test/resources/golden/`:** **gerados** — não comentar
  conteúdo, apenas o gerador (`*Assembler.java` em
  `java/src/main/java/dev/iadev/application/assembler/`).

## Quando uma Sugestão Sua Conflitar com uma Regra do Projeto

A regra do projeto (`.claude/rules/NN-*.md`) **sempre vence**. Se você
identificar conflito entre uma "best practice" genérica e uma regra deste
projeto, mencione a regra explicitamente e proponha alinhamento à regra,
não o contrário.

## Versão

`v1.0` — alinhado às rules 01–28 vigentes em 2026-04. Atualizações futuras
devem manter a estrutura de seções para preservar referências em PRs antigos.
