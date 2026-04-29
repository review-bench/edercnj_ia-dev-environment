# Plano de Arquitetura — story-0063-0007

**Story:** Local Coverage Gate (audit-coverage-local.sh)

## Componentes

- `java/src/main/resources/targets/claude/scripts/audit-coverage-local.sh` (NEW source-of-truth)
- `.claude/scripts/audit-coverage-local.sh` (REGENERATED)
- `src/test/shell/audit_coverage_local_test.sh` (NEW)

## Parsing Strategy

JaCoCo CSV preferido (target/site/jacoco/jacoco.csv); fallback HTML.

Coverage calc: `lc/(lm+lc)*100` para line; `bc/(bm+bc)*100` para branch.

## Exit Codes (Rule 26)

| Exit | Code |
| :--- | :--- |
| 0 | OK |
| 1 | COVERAGE_BELOW_THRESHOLD |
| 2 | OPERATIONAL_ERROR |
| 3 | INVALID_ARGS |
