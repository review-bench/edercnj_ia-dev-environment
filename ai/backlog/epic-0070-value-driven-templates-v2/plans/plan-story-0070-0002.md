# Implementation Plan — story-0070-0002

**Story:** Reescrever `_TEMPLATE-EPIC.md` v2 (foco valor)
**Epic:** EPIC-0070

## Implementation Steps

### Step 1: Add frontmatter v3.0 to _TEMPLATE-EPIC.md

**File:** `src/main/resources/shared/templates/_TEMPLATE-EPIC.md`

Add at the very top of the file (before `# Épico:`):

```yaml
---
requires-capabilities: [governance.value-driven-templates]
template-version: "2.0"
---
```

### Step 2: Rewrite header and metadata block

Replace the current header section with the v2 header keeping the same metadata fields but updating the Status enum to include `Refinada`.

### Step 3: Replace sections 1-9 with v2 value-driven sections

Nine new sections replacing the v1 RA9 technical sections:

1. **Visão & Problema** — narrative problem statement (observable pain + evidence)
2. **Persona & Stakeholders** — specific affected personas
3. **Hipótese & OKRs** — If/then/because form + ≥1 OKR with unit
4. **Alternativas Consideradas** — ≥2 alternatives with rejection rationale
5. **Escopo** — explicit in-scope + out-of-scope lists
6. **Riscos** — product risks + technical risks
7. **Índice de Histórias** — story index table
8. **Quality Gates** — DoR/DoD (adapted from v1 section 5)
9. **Origem & Referências** — sources, ADRs, related epics

### Step 4: Retain 0.5 Cross-Epic Dependencies and Refinement Verdict

These sections are structural and must be preserved unchanged.

### Step 5: Sync .claude/templates/_TEMPLATE-EPIC.md

The `.claude/` directory is generated output but for this project the source template at `src/main/resources/shared/templates/` is the source-of-truth. `PlanTemplatesAssembler` copies it to `.claude/templates/`. We update both directly to keep the working tree consistent.

### Step 6: Validate with 2 synthetic dogfood epics

Use EPIC-0070 and EPIC-0064 as reference epics to validate the template structure produces good narrative output.

## Implementation Notes

- Do NOT use `mvn` to regenerate goldens — delegated to story-0070-0008
- TemplatesAssembler.java: no changes needed — it copies the template verbatim
- The `{{PLACEHOLDER}}` tokens in sections are resolved at runtime by LLM, not during generation
