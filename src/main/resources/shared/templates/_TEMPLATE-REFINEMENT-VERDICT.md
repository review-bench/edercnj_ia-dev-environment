## Refinement Verdict

> **Gerado por:** `/x-{{REFINE_SKILL}}` em `{{CHECKED_AT}}`
> **Scope:** {{SCOPE}} | **Status:** {{STATUS_BADGE}}

| Dimensão | Persona | Resultado | Blocker |
| :--- | :--- | :--- | :--- |
{{#each dimensions}}
| {{@key}} | {{persona}} | {{#if checked}}✅ OK{{else}}❌ NO-GO{{/if}} | {{blocker}} |
{{/each}}

### Blockers

{{#if blockers}}
{{#each blockers}}
- {{this}}
{{/each}}
{{else}}
_Nenhum blocker — refinamento aprovado._
{{/if}}

---

> **verdictHash:** `{{VERDICT_HASH}}`
> Editores humanos: não edite este bloco manualmente — `audit-refinement-gate.sh` detecta divergência via hash (Rule 29 §verdictHash).
