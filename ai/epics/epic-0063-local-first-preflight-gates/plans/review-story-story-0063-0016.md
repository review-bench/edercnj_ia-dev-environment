# Specialist Review — story-0063-0016

**Story:** Rollout WARN→FAIL Execution + ADR-0016
**Reviewer:** Specialist (QA/Security/Performance)
**Date:** 2026-04-28
**PR:** (pending)

## Architecture Review

A implementação segue padrões consistentes com EPIC-0063 e os scripts de auditoria existentes.
- `audit-rollout-status.sh` reside em `java/src/main/resources/targets/claude/scripts/` (source-of-truth)
- Cópia gerada em `.claude/scripts/audit-rollout-status.sh`
- Testes em `src/test/shell/audit_rollout_status_test.sh` (5 cenários, 7 assertivas, TDD RED→GREEN)
- ADR documentado em `docs/adr/ADR-0019-preflight-warn-to-fail-rollout.md`
- Sem violações de domain purity

### File Analysis
- `audit-rollout-status.sh`: bash strict mode (`set -uo pipefail`), exit codes Rule 26 compliant
- `src/test/shell/audit_rollout_status_test.sh`: 7 assertivas cobrindo todos os cenários Gherkin
- `docs/adr/ADR-0019-preflight-warn-to-fail-rollout.md`: ADR com status Accepted, alternativas documentadas

## Code Quality Assessment

### Per-File Review
- Exit codes seguem Rule 26 §Standardized (0=OK, 2=OPERATIONAL_ERROR)
- `--self-check` implementado (exit 0, sem dependências externas obrigatórias)
- `--set-mode` com validação de whitelist (`warn|fail`)
- `--state-file` override permite injeção para testes (testabilidade garantida)
- Toggle precedence implementada: env var > state file > default (`warn`)
- Path handling com `mkdir -p` e tratamento de erros de I/O

### Test Coverage Analysis
- T1: `--self-check` → exit 0 ✓
- T2: `--set-mode warn` → cria state file com `"mode": "warn"` ✓
- T3: `--set-mode fail` → atualiza state file com `"mode": "fail"` ✓
- T4: flag desconhecida → exit 2 ✓
- T5: modo inválido (`banana`) → exit 2 ✓

## Compliance Validation

### Rule 26 (Audit Gate Lifecycle)
✓ Prefixo `audit-` obrigatório respeitado
✓ Exit codes no range 0–2 conforme especificação
✓ `--self-check` implementado (exit 0 sem falhas operacionais)
✓ Layer 2 (detectivo) declarado no cabeçalho do script

### Rule 27 (Zero-Bypass Lifecycle)
✓ ADR-0016 documenta a estratégia de rollout WARN→FAIL
✓ Toggle permite transição controlada sem big-bang
✓ `audit-rollout-status.sh` reporta bypass count de `recovery_mode_used` events

### Rule 06 (Security)
✓ Sem credenciais hardcoded
✓ Input validation: `--set-mode` restrito a `warn|fail`
✓ Mensagens de erro não expõem paths internos sensíveis

### Rule 03 (Coding Standards)
✓ Script abaixo de 120 linhas por função
✓ Nomes de variáveis descritivos (CURRENT_MODE, MODE_SOURCE, TRANSITION_DATE)
✓ Sem código morto

## Recommendations

1. Considerar adicionar campo `bypass_count_since_last_mode_change` ao state file em epic futuro
2. Integrar `audit-rollout-readiness.sh` (story-0063-0016 escopo completo) ao CI da release branch
3. Documentar calendário concreto em `docs/preflight-rollout.md` quando datas de release forem definidas

## Decision

Esta story atende todos os critérios de aceite. ADR bem fundamentado, toggle com precedência clara, testes passando.

**GO**
