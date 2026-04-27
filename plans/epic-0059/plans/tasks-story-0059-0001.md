# Task Breakdown — story-0059-0001

**Story:** Estender audit-execution-integrity.sh para os 6 Artefatos de Fase 1
**Epic:** EPIC-0059
**Date:** 2026-04-27

## Tasks

### TASK-0059-0001-001
- **Title:** Adicionar REQUIRED_PHASE_1_ARTIFACTS ao audit-execution-integrity.sh
- **Layer:** Adapter (CI script)
- **Size:** M
- **Dependencies:** —
- **Branch:** `feat/task-0059-0001-001-extend-audit-phase1`
- **Files:** `scripts/audit-execution-integrity.sh`
- **DoD:** Array com 6 entries, loop de verificação, exit 1 quando ausente

### TASK-0059-0001-002
- **Title:** Adicionar flag --scope=fase1 e estender --self-check
- **Layer:** Adapter (CI script)
- **Size:** S
- **Dependencies:** TASK-0059-0001-001
- **Branch:** `feat/task-0059-0001-002-scope-selfcheck`
- **Files:** `scripts/audit-execution-integrity.sh`
- **DoD:** `--scope=fase1` funcional, `--self-check` conta 10 artifacts, exit 4 se != 10

### TASK-0059-0001-003
- **Title:** Popular audits/execution-integrity-baseline.txt (EPIC-0054–0057)
- **Layer:** Config
- **Size:** M
- **Dependencies:** TASK-0059-0001-001
- **Branch:** `feat/task-0059-0001-003-amnesty-baseline`
- **Files:** `audits/execution-integrity-baseline.txt`
- **DoD:** Todas as stories de EPIC-0054-0057 listadas com razão `# amnesty EPIC-0059`
