---
name: jpa-postgres
description: "JPA/Hibernate patterns specific to PostgreSQL: types, sequences, JSON columns, and Flyway integration."
requires-capabilities: [data.database.postgres]
fragment-slot: { slot: jpa-engine, fragment-id: postgres, fragment-order: 10 }
---

## JPA / PostgreSQL Integration

### Entity ID Strategy

PostgreSQL uses `BIGSERIAL` (identity column) — prefer `GenerationType.IDENTITY`:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

For high-throughput inserts, use `GenerationType.SEQUENCE` with allocationSize to reduce round trips:

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "merchant_seq")
@SequenceGenerator(name = "merchant_seq", sequenceName = "merchants_id_seq", allocationSize = 50)
private Long id;
```

### PostgreSQL-Specific Column Types

```java
@Column(columnDefinition = "jsonb")
private String metadata;  // Store as JSONB, query via @Query with CAST

@Column(columnDefinition = "uuid")
private UUID externalId;  // Store as native UUID (not VARCHAR)

@Column(columnDefinition = "text[]")  // Requires @Type(PostgreSQLEnumType.class) for arrays
private String[] tags;
```

### Enum Storage

```java
@Enumerated(EnumType.STRING)  // ALWAYS STRING, never ORDINAL
@Column(name = "status", nullable = false, length = 20)
private TransactionStatus status;
```

### Flyway Migration Naming (PostgreSQL)

```
db/migration/V1__create_merchants_table.sql
db/migration/V2__add_merchants_index.sql
db/migration/V3__add_terminals_table.sql
```

Key conventions:
- Use `BIGSERIAL PRIMARY KEY` (not `BIGINT GENERATED ALWAYS AS IDENTITY` for Flyway compatibility)
- Always use `CREATE INDEX CONCURRENTLY` for production indexes (use regular in migration, note for ops)
- Wrap DDL in transaction (PostgreSQL DDL is transactional — Flyway uses `BEGIN`/`COMMIT`)
- Never use `DROP INDEX` — use `DROP INDEX IF EXISTS`

### HikariCP Configuration (PostgreSQL)

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb?currentSchema=simulator
```

### PostgreSQL-Specific Pitfalls

- `LIKE` queries with `%prefix` pattern do not use B-tree indexes — use GIN index with `pg_trgm` extension for full-text
- `DISTINCT ON` is PostgreSQL-specific but more efficient than `GROUP BY` for latest-per-group queries
- `RETURNING` clause after INSERT/UPDATE is PostgreSQL-specific — use `@Query` with native query flag
- JSON operators (`->`, `->>`, `@>`) require native queries in Spring Data
