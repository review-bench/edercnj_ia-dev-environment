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

- **Nome:** `my-java-cli` (CLI gerador de ambiente de desenvolvimento Claude Code).
- **Stack:** Java 21, Picocli 4.7, Maven.
- **Arquitetura:** Hexagonal (Ports & Adapters). Domínio puro, sem dependências
  externas além da biblioteca padrão.
- **Propósito:** Ler YAML de configuração e gerar o diretório `.claude/`
  (skills, rules, hooks, agents) para o projeto-alvo.

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
  ou `audits/baselines/*.txt`** — são gerados/imutáveis por contrato.
- **Não bloqueie PRs por nits.** Reserve "request changes" para violações
  de Rules 03–06, 12 ou bugs lógicos reais.

## Caminhos com Tratamento Especial

- **`domain/**`:** pureza absoluta — zero imports de framework/serialização.
  Se o domínio precisa de I/O ou serialização, exigir port interface.
- **`adapter/inbound/**`:** validação de entrada + tratamento de erro
  uniforme; nunca retornar mensagem de exceção crua ao cliente. Deve chamar
  `application/`, nunca orquestrar domínio diretamente.
- **`adapter/outbound/**`:** queries parametrizadas, timeouts explícitos,
  cleanup de recursos via `try-with-resources` ou `finally`.
- **`src/test/**`:** nomes no padrão `[método]_[cenário]_[comportamento]`,
  asserções específicas (`assertThat(x).isEqualTo(y)`, não `isNotNull()`),
  arquivos > 250 linhas exigem inner classes / agrupamento.
- **`scripts/audit-*.sh`:** prefixo obrigatório `audit-`, exit codes
  padronizados (0/1/2/3) conforme Rule 26, suporte obrigatório a `--self-check`.
- **`.claude/`, `src/test/resources/golden/`:** **gerados** — não comentar
  conteúdo, apenas o gerador (`*Assembler.java`).

## Quando uma Sugestão Sua Conflitar com uma Regra do Projeto

A regra do projeto (`.claude/rules/NN-*.md`) **sempre vence**. Se você
identificar conflito entre uma "best practice" genérica e uma regra deste
projeto, mencione a regra explicitamente e proponha alinhamento à regra,
não o contrário.

## Versão

`v1.0` — alinhado às rules 01–28 vigentes em 2026-04. Atualizações futuras
devem manter a estrutura de seções para preservar referências em PRs antigos.
