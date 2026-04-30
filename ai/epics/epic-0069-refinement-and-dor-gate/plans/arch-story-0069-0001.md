# Architecture Plan — story-0069-0001

**Story:** Capability `governance.refinement-gate` + Rule 29 + ADR-0022
**Architecture Style:** Content-layer additions (no new Java classes)

## Architectural Decisions

### 1. Capability is Universal (No Stack Dependency)
`governance.refinement-gate` declares `requires-capabilities: []` — no stack prerequisite.
Rationale (D-R5): refinement is a process, not a technology. Stack does not affect what a persona or a well-formed AC means.

### 2. Rule 29 is the Canonical Owner of the State Machine Extension
Status `Refinada` and its transitions are documented inside Rule 29, not Rule 22.
Rationale (D-R3-b): Rule 22 is "Skill Visibility" — unrelated to lifecycle status. Rule 29 defines what `Refinada` means; keeping them co-located achieves locality.

### 3. KP `refinement/dimensions.md` is Shared by stories 0002 and 0003
Rather than duplicating heuristics in both SKILL.md files, the knowledge pack centralizes acceptance/rejection rules per dimension. Stories 0002 and 0003 reference it via frontmatter `context: [refinement/dimensions]`.

### 4. ADR-0022 (not ADR-0018)
ADR-0018 was taken by `zero-bypass-amnesty`. Next free slot confirmed: ADR-0022.

## Dependency Direction
```
capabilities/ (pure YAML — no Java dependency)
    ↓
src/main/resources/targets/claude/rules/29-refinement-gate.md (content layer)
src/main/resources/targets/claude/knowledge/refinement/dimensions.md (content layer)
docs/adr/ADR-0022-refinement-gate.md (documentation)
```

No domain model changes in this story (deferred to story-0069-0004).
