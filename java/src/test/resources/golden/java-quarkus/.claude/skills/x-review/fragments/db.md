---
name: x-review-fragment-db
description: Database specialist review fragment — active when any database capability is configured.
fragment-slot: { slot: review-specialist, fragment-id: db, fragment-order: 50 }
requires-capabilities: [data.database.*]
---

### Database Specialist (`/x-review-db`)

| Attribute | Value |
|-----------|-------|
| Max Score | /40 |
| Condition | `database != none` |
| Skill | `x-review-db` |

Reviews: migration idempotency (Flyway/Liquibase), index strategy (covering indexes, composite index order), N+1 query patterns (Hibernate `@EntityGraph`, `JOIN FETCH`), transaction boundary placement (`@Transactional` on service layer), connection pool sizing (`HikariCP`), schema naming conventions (snake_case, plural tables), and soft-delete vs hard-delete patterns.
