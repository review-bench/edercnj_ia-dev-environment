# Stack-Specific Content Inventory — EPIC-0064 story-0064-0401

Generated: 2026-04-29  
Tool: bash + grep scan of `java/src/main/resources/targets/claude/`

---

## Summary

| Stack | Files with stack-specific content | Recommendation |
|-------|-----------------------------------|----------------|
| Spring Boot | 9 | fragment / move-to-subdir |
| Quarkus | 10 | fragment / move-to-subdir |
| Picocli (CLI) | 2 | move-to-subdir |
| Helidon | 1 | new-subdir |
| Micronaut | 1 | new-subdir |
| Universal | ~80 | keep-universal |

---

## Detailed Inventory

| Path | Stack | Content-Identified | Recommendation |
|------|-------|-------------------|----------------|
| `knowledge/stack-patterns/spring-patterns/index.md` | spring | @SpringBootApplication, @Service, @ConfigurationProperties, Spring DI, Health checks | move-to-subdir → `stack-patterns/spring/index.md` + fix requires-capabilities |
| `knowledge/stack-patterns/quarkus-patterns/index.md` | quarkus | @ApplicationScoped, @ConfigMapping, Panache, CDI, @Inject, RESTEasy | move-to-subdir → `stack-patterns/quarkus/index.md` + fix requires-capabilities |
| `knowledge/database-patterns/connection-pool-tuning.md` | quarkus+spring+helidon | HikariCP (Spring), Agroal (Quarkus), Helidon dbclient | fragment → separate engine fragments |
| `knowledge/database-patterns/index.md` | spring | JPA sections reference Spring Data annotations | fragment → JPA/R2DBC fragments |
| `knowledge/layer-templates.md` | spring+quarkus | @RestController (Spring), @Inject (CDI/Quarkus) | fragment → stack-specific adapter layer templates |
| `rules/conditional/anti-patterns/10-anti-patterns.java-spring-boot.md` | spring | Spring Boot anti-patterns | keep-conditional (already conditional) |
| `rules/conditional/anti-patterns/10-anti-patterns.java-quarkus.md` | quarkus | Quarkus anti-patterns | keep-conditional (already conditional) |
| `agents/core/performance-engineer.md` | quarkus | @ApplicationScoped CDI reference | fragment → make conditional on quarkus |
| `agents/developers/java-developer.md` | quarkus | References Quarkus CDI patterns | fragment |
| `knowledge/architecture-hexagonal.md` | quarkus | CDI @ApplicationScoped example | minor reference, keep-universal |
| `skills/conditional/test/x-test-e2e/SKILL.md` | spring+quarkus | @SpringBootTest, @QuarkusTest references | keep-conditional (already conditional) |
| `skills/core/dev/x-ci-generate/SKILL.md` | spring+quarkus | Framework-specific CI config | fragment |
| `skills/core/dev/x-story-implement/references/openapi-generator.md` | spring+quarkus | Spring/Quarkus OpenAPI generator config | fragment |

---

## Action Plan for Phase 4

### story-0064-0402 — Reorganize stack-patterns/
1. `spring-patterns/` → `spring/` with `requires-capabilities: [web.spring.boot]`
2. `quarkus-patterns/` → `quarkus/` with `requires-capabilities: [web.quarkus.framework]`
3. Create `picocli/` with `requires-capabilities: [cli.picocli.framework]`
4. Create `helidon/` with `requires-capabilities: [web.helidon.framework]`
5. Create `micronaut/` with `requires-capabilities: [web.micronaut.framework]`

### story-0064-0403 — db-jpa fragments
- Create `knowledge/database-patterns/jpa/` with 4 engine fragments (postgres, mysql, mariadb, h2)

### story-0064-0404 — db-r2dbc KP
- Create `knowledge/database-patterns/r2dbc/` with reactive engine fragments + mutex with jpa

### story-0064-0405 to 0409 — Framework-specific skills
- `skills/core/dev/spring-controller/` — Spring @RestController pattern skill
- `skills/core/dev/quarkus-resource/` — Quarkus RESTEasy Resource pattern skill
- `skills/core/dev/picocli-command/` — Picocli @Command pattern skill
- `skills/core/dev/helidon-scaffold/` — Helidon declarative scaffold (5 artifacts)
- `skills/core/dev/micronaut-scaffold/` — Micronaut declarative scaffold (5 artifacts)
