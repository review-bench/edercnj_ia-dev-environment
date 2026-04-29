---
name: claude-md-fragment-database
description: Database-specific CLAUDE.md section — active when any database capability is configured.
fragment-slot: { slot: domain-specific, fragment-id: database, fragment-order: 10 }
requires-capabilities: [data.database.*]
---

## Database Patterns

This project uses `{{ capabilities.data.database.name }}` as its primary database.

- **Migrations:** Use Flyway or Liquibase; all migrations are in `src/main/resources/db/migration/` with versioned names (`V__description.sql`). Never alter an applied migration.
- **Transactions:** Annotate service-layer methods with `@Transactional`; never apply it to repository or domain classes.
- **Connection pool:** Configured via `HikariCP`; tune `maximumPoolSize` based on `(core_count * 2) + effective_spindle_count`.
- **Query generation:** Prefer Spring Data JPA derived queries and JPQL for complex queries; raw SQL only for performance-critical paths with a comment explaining why.
- **Index strategy:** All queried columns must have an index; composite index column order follows cardinality (high → low).
- **Soft delete:** Use `@SQLDelete`/`@Where` in JPA; never expose deleted records through normal repository queries.
