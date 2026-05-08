# Tech-Lead Review — story-0063-0004

**Story:** PreToolUse Blocking Hook (enforce-preflight-gates.sh)
**Reviewer:** Tech-Lead (45-point holistic review)
**Date:** 2026-04-28
**Branch:** feat/story-0063-0004-pretooluse-hook-v1

## Overview

Story story-0063-0004 implementa o hook PreToolUse `enforce-preflight-gates.sh` — peça central da Camada 0 preventiva do EPIC-0063. A implementação foi avaliada contra o checklist de 45 pontos.

## 45-Point Checklist

### Clean Code (10 pontos)
✓ Naming convention consistente (enforce-* prefix para hooks bloqueadores)
✓ Functions focadas: `derive_ids_from_branch`, `block_with_message`, `emit_recovery_event`, `resolve_ndjson_path`
✓ No dead code — todas as funções utilizadas no fluxo principal
✓ Comentários expressam "porquê", não "o quê"
✓ Variable names self-documenting: SHOULD_INTERCEPT, INTERCEPT_CONTEXT, DERIVED_SCOPE
✓ Single responsibility: hook decide blocked/allowed, preflight executa gates
✓ DRY: funções reutilizadas para múltiplos patterns
✓ Error messages claros com refs: Rule 27 §RULE-059-07, story-0063-0004
✓ Imports/sources explícitos
✓ Indentation e estilo consistente com enforce-no-bypass-flags.sh

### SOLID Principles (5 pontos)
✓ SRP: hook intercepta e decide; preflight.sh executa; separação clara
✓ OCP: novos patterns = nova entrada no bloco de pattern matching (extensível)
✓ LSP: N/A (sem herança em bash)
✓ ISP: interface mínima — stdin JSON, exit 0/2
✓ DIP: depende de jq via PATH, preflight via $PROJECT_DIR (configurável)

### Architecture (5 pontos)
✓ Source-of-truth em java/src/main/resources (geração determinística)
✓ Output copy em .claude/hooks (regenerável)
✓ Registrado corretamente em settings.json (antes de enforce-no-bypass-flags.sh)
✓ Latency target respeitado (fast-path exit para não-interceptados)
✓ Camada 0 posicionada corretamente no modelo de 5 camadas (Rule 26)

### Framework Conventions (5 pontos)
✓ Claude Code PreToolUse exit code contract: 0=allow, 2=block
✓ Rule 26 §Camada 0 naming: enforce-*.sh prefix
✓ Rule 26 §Camada 0 header block presente
✓ Rule 27 RULE-004 (único bypass) implementado fielmente
✓ Rule 27 RULE-005 (fail-CLOSED) implementado: ausência de preflight = block

### Tests (5 pontos)
✓ Tests precede implementation (TDD verified)
✓ 9 testes cobrindo edge cases: recovery, preflight ausente, bypass commands, malformed JSON
✓ No mocking domain — testes usam executables reais (fake scripts)
✓ Test names descrevem comportamento esperado
✓ Todos os cenários Gherkin críticos verificados (5.2 AC)

### TDD Process (5 pontos)
✓ RED phase verificada: 0/9 passando antes da implementação
✓ GREEN phase verificada: 9/9 passando após implementação
✓ Atomic commits por fase TDD
✓ Conventional Commits format
✓ Test file criado antes do hook file

### Security (5 pontos)
✓ No hardcoded credentials
✓ Branch name validado via regex strict — nenhum path traversal possível
✓ Input validation via jq parse → fail-CLOSED em JSON inválido
✓ Bypass vars whitelist explícita — CLAUDE_SKIP_AUDIT etc. IGNORADOS
✓ Error messages: paths e context revelados apenas em stderr (não stdout)

### Cross-file Consistency (5 pontos)
✓ Exit code pattern uniforme com enforce-no-bypass-flags.sh e enforce-phase-sequence.sh
✓ Bash strict mode uniforme: `set -uo pipefail`
✓ PROJECT_DIR resolution uniforme: `${CLAUDE_PROJECT_DIR:-$(git ...)}`
✓ jq dependency check pattern uniforme
✓ stderr output format consistente com outros enforce-*.sh hooks

## Risk Assessment

**Low Risk.** Implementação é incremental e backward-compatible. Hook é fail-CLOSED (RULE-005) — casos edge (branch não-matched, JSON malformado) são tratados conservadoramente. CLAUDE_RECOVERY_MODE=1 permite bypass auditado para cenários de recovery. O mecanismo de bloqueio é físico (exit code 2 interpretado por Claude Code) — não depende de disciplina do LLM.

**Risco residual:** Hook intercepta via regex matching no command string — um comando muito complexo com git push embutido em subshell poderia não ser interceptado. Aceito: coberto por Camada 3 CI audit.

## Decision

All 45 checkpoints passed. Story is production-ready.

**GO**
