# Test Plan — story-0070-0002

**Story:** Reescrever `_TEMPLATE-EPIC.md` v2
**Epic:** EPIC-0070

## Test Strategy

SIMPLE scope — template markdown artifact. Tests are shell/grep-based AC verification.

## AC Test Scenarios

### TC-001 — Happy: 9 v2 sections present

```bash
# Verify exactly 9 expected sections exist
for section in \
  "## 1. Visão & Problema" \
  "## 2. Persona & Stakeholders" \
  "## 3. Hipótese & OKRs" \
  "## 4. Alternativas Consideradas" \
  "## 5. Escopo" \
  "## 6. Riscos" \
  "## 7. Índice de Histórias" \
  "## 8. Quality Gates" \
  "## 9. Origem & Referências"; do
  grep -qF "$section" src/main/resources/shared/templates/_TEMPLATE-EPIC.md || echo "MISSING: $section"
done
# Expected: no output (all sections present)
```

### TC-002 — Degenerate: v1 technical sections absent

```bash
# Verify v1 sections are gone
count=$(grep -cE "^## (2\. Packages|3\. Contratos|4\. Materializa|6\. Segurança|7\. Observabilidade)" \
  src/main/resources/shared/templates/_TEMPLATE-EPIC.md)
echo "v1 technical sections found: $count"
# Expected: 0
```

### TC-003 — Error: frontmatter v3.0 present

```bash
grep -A2 "^---" src/main/resources/shared/templates/_TEMPLATE-EPIC.md | head -5
# Expected: requires-capabilities: [governance.value-driven-templates]
# Expected: template-version: "2.0"
```

### TC-004 — Boundary: dogfood validation

Manual review: EPIC-0070 and EPIC-0064 written against the v2 template both produce
- ≥ 1 paragraph of verifiable hypothesis
- ≥ 2 rejected alternatives with rationale
- Specific personas (not "sistema")

## Regression Tests

- `.claude/templates/_TEMPLATE-EPIC.md` in sync with source-of-truth
- `## 0.5 Cross-Epic Dependencies` section retained
- `## Refinement Verdict` slot retained
