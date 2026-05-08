---
requires-capabilities: [governance.bug-lifecycle]
template-version: "1.0"
template-type: bug-implementation-map
generated-by: x-internal-map-bug
generated-at: {{GENERATED_AT}}
source-bug: bug-{{BUG_ID}}
---

# Implementation Map — bug-{{BUG_ID}}

**Bug:** bug-{{BUG_ID}}  
**Total Stories:** {{STORY_COUNT}}  
**Critical Path Length:** {{CRITICAL_PATH_LENGTH}}  
**Generated:** {{GENERATED_AT}}

---

## 1. Dependency Matrix

> Blocked-By relationships between all stories in this bug.

| Story | Blocked By | Blocks |
| :---- | :--------- | :----- |
{{DEPENDENCY_MATRIX_ROWS}}

---

## 2. Phase Diagram (ASCII)

> Execution phases based on topological sort. Stories in the same phase can run in parallel.

```
{{ASCII_PHASE_DIAGRAM}}
```

---

## 3. Critical Path

> Longest dependency chain — the minimum time to resolve this bug.

```
{{CRITICAL_PATH_TRACE}}
```

**Critical path length:** {{CRITICAL_PATH_LENGTH}} stories

---

## 4. Mermaid Dependency Graph

> Visual representation of all story dependencies.

```mermaid
graph TD
{{MERMAID_GRAPH_NODES}}
{{MERMAID_GRAPH_EDGES}}
```

---

## 5. Phase Summary Tables

> Per-phase story listing with kind and estimated complexity.

{{PHASE_SUMMARY_TABLES}}

---

## 6. Generation Metadata

| Field | Value |
| :---- | :---- |
| Generator Skill | x-internal-map-bug |
| Generated At | {{GENERATED_AT}} |
| Source Bug | `ai/bugs/bug-{{BUG_ID}}/bug.md` |
| Source Stories | {{SOURCE_STORIES_LIST}} |
| Story Count | {{STORY_COUNT}} |
| Severity | {{BUG_SEVERITY}} |
| Scope | {{BUG_SCOPE}} |
