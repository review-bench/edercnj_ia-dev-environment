# Implementation Plan — story-0059-0009

**Story:** GitHub Branch Protection + CODEOWNERS
**Scope:** SIMPLE
**Planning Mode:** INLINE

## Tasks

### TASK-0059-0009-001: Create scripts/setup-branch-protection.sh
- **Branch:** `feat/task-0059-0009-001-branch-protection-script`
- **Files:** `scripts/setup-branch-protection.sh`, `audits/required-checks.txt`
- **Layer:** Ops/Script

### TASK-0059-0009-002: Create .github/CODEOWNERS and SETUP-PROTECTION.md
- **Branch:** `feat/task-0059-0009-002-codeowners-docs`
- **Files:** `.github/CODEOWNERS`, `.github/SETUP-PROTECTION.md`
- **Dependencies:** TASK-0059-0009-001
- **Layer:** Doc/Config

## Acceptance Criteria Coverage
- setup-branch-protection.sh idempotent, --dry-run flag, configures required checks
- CODEOWNERS protects 6 critical paths
- SETUP-PROTECTION.md documents full configuration procedure
