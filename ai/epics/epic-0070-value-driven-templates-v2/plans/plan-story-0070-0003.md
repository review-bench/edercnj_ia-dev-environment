# Implementation Plan — story-0070-0003

Rewrite `src/main/resources/shared/templates/_TEMPLATE-STORY.md` to v2.

## Steps

1. Add frontmatter v3.0 (requires-capabilities + template-version)
2. Replace sections 1-9 with value-driven v2 (keeping Decision Rationale content semantics)
3. Section 4 (AC Gherkin): include 4 mandatory category skeletons (degenerate, happy, error/boundary, perf/SLA, security)
4. Section 5 (Contratos): keep typed request/response tables but remove SOLID/Coding Constraints
5. Section 9 (Refinement Verdict): slot with contract comment for EPIC-0069
6. Remove: Packages (Hexagonal), Materialização SOLID, 4.2/4.3 SOLID+Coding Constraints, Segurança section, Observabilidade section
7. Sync to `.claude/templates/_TEMPLATE-STORY.md`
