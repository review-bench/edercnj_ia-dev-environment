# Test Plan — story-0070-0003

## AC Scenarios

### TC-001 — Happy: 9 v2 sections present
Verify sections: Visão, Persona & Cenário, Entrega de Valor, AC (Gherkin), Contratos, Tasks, Dependências, Decision Rationale, Refinement Verdict

### TC-002 — Degenerate: v1 technical sections absent
grep -E "^## (Packages|SOLID|Coding Constraints|Observabilidade|Segurança)" → 0 matches

### TC-003 — Performance/SLA: template ≤ 200 lines
wc -l _TEMPLATE-STORY.md → reasonable size

### TC-004 — Security: placeholder sanitization
Documented in template (HTML comment notes placeholder escaping concern for rendering layer)

### TC-005 — Frontmatter v3.0 present
head of file shows requires-capabilities and template-version fields

### TC-006 — Gherkin 4 categories present
Template includes degenerate, happy, error/boundary, performance/SLA, security scenario skeletons
