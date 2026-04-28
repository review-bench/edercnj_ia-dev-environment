# Specialist Review — story-0063-0004

**Story:** PreToolUse Blocking Hook (enforce-preflight-gates.sh)
**Reviewer:** Specialist (QA/Security/Performance)
**Date:** 2026-04-28
**Branch:** feat/story-0063-0004-pretooluse-hook-v1

## Architecture Review

A implementação segue padrões hexagonais consistentes com o projeto.
- Hook source-of-truth em `java/src/main/resources/targets/claude/hooks/enforce-preflight-gates.sh`
- Output copy em `.claude/hooks/enforce-preflight-gates.sh` (chmod +x)
- Testes em `src/test/shell/enforce_preflight_gates_test.sh`
- Registrado em `.claude/settings.json` PreToolUse (antes de enforce-no-bypass-flags.sh conforme contrato)

### File Analysis

- `enforce-preflight-gates.sh`: bash strict mode (set -uo pipefail), exit 0/2 contrato Claude Code
- `enforce_preflight_gates_test.sh`: 9 testes TDD cobrindo todos os cenários Gherkin principais
- `settings.json`: hook registrado com timeout=300 (adequado para preflight.sh execution)

## Code Quality Assessment

### Per-File Review

- Exit codes corretos: 0 (allow) e 2 (BLOCKED) conforme contrato Claude Code PreToolUse
- `--no-verify`/`git commit -n` detectados e bloqueados ANTES do bypass check (incondicional)
- `gh pr merge --admin` detectado e bloqueado incondicionalmente
- `CLAUDE_RECOVERY_MODE=1` é o ÚNICO bypass (RULE-004, Rule 27)
- Malformed JSON input → exit 2 fail-CLOSED (RULE-005)
- Preflight ausente → exit 2 fail-CLOSED (RULE-005)
- Non-matched tool calls → exit 0 (no-op eficiente)

### Test Coverage Analysis

- T1: hook executável ✓
- T2: strict mode ✓
- T3: referencia CLAUDE_RECOVERY_MODE ✓
- T4: CLAUDE_RECOVERY_MODE=1 → exit 0 ✓
- T5: preflight ausente → exit 2 (fail-CLOSED) ✓
- T6: non-matched tool call → exit 0 ✓
- T7: git commit --no-verify → exit 2 ✓
- T8: Skill x-pr-create → exit 0 (preflight green) ✓
- T9: malformed stdin → exit 2 ✓

All 9 tests GREEN.

## Compliance Validation

### Rule 03 (Coding Standards)
✓ Bash functions focadas, single responsibility
✓ No train-wreck dependencies
✓ Named constants para mensagens de erro

### Rule 05 (Quality Gates)
✓ Tests precede implementation (TDD — RED phase verified at 0/9, GREEN phase at 9/9)
✓ Edge cases cobertos: recovery mode, preflight ausente, bypass commands, malformed JSON

### Rule 06 (Security)
✓ Sem hardcoded credentials
✓ Branch name validado via regex strict antes de uso (Rule 06 §Defensive Coding)
✓ Path operations: PROJECT_DIR → PREFLIGHT path derivado de forma segura
✓ Bypass vars whitelist: apenas CLAUDE_RECOVERY_MODE honrada; outras ignoradas

### Rule 24 (Execution Integrity)
✓ Camada 0 preventiva — bloqueia ANTES do tool call executar
✓ Evidence via telemetria NDJSON para recovery_mode_used events
✓ Exit codes named em stderr output

### Rule 26 (Audit Gate Lifecycle — Camada 0)
✓ Header block canonical presente (Layer, Trigger, Event, Exit codes, Latency, Telemetry)
✓ `enforce-` prefix para hook que bloqueia ativamente (Camada 0 naming convention)
✓ Latency target: < 500ms (não-interceptados em < 100ms com fast-path exit)

### Rule 27 (Zero-Bypass Lifecycle)
✓ RULE-004 implementado: CLAUDE_RECOVERY_MODE=1 é único bypass
✓ RULE-005 implementado: fail-CLOSED para gates (preflight ausente = block)
✓ Telemetry event para recovery_mode_used (auditoria)

## Recommendations

1. Story-0063-0013 (v2 matchers) deve estender este hook com 15 vetores adicionais
2. Story-0063-0016 (feature toggle CLAUDE_PREFLIGHT_PHASE) deve adicionar --warn mode
3. Story-0063-0018 (hook self-check) deve adicionar --self-check flag a este hook

## Decision

Story meets all acceptance criteria. Implementation is sound, TDD-compliant, and follows all relevant project conventions.

**GO**
