---
name: jpa-mysql
description: "JPA/Hibernate patterns specific to MySQL 8: AUTO_INCREMENT, strict mode, JSON columns, and Flyway integration."
requires-capabilities: [data.database.mysql]
fragment-slot: { slot: jpa-engine, fragment-id: mysql, fragment-order: 20 }
---

## JPA / MySQL 8 Integration

### Entity ID Strategy

MySQL uses `AUTO_INCREMENT` — use `GenerationType.IDENTITY`:

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

Avoid `GenerationType.SEQUENCE` — MySQL does not support true sequences (emulated via table, slow under contention).

### MySQL-Specific Column Types

```java
@Column(name = "metadata", columnDefinition = "JSON")
private String metadata;  // MySQL 5.7+ JSON column (native JSON type)

@Column(name = "status", columnDefinition = "ENUM('PENDING','APPROVED','DENIED')")
private String status;  // MySQL ENUM (prefer VARCHAR for JPA portability)
```

### String Column Length

MySQL `VARCHAR` defaults to `utf8mb4` (4 bytes per char). Indexed columns have a 767-byte limit (or 3072 bytes with `innodb_large_prefix`):

```java
@Column(name = "mid", nullable = false, unique = true, length = 15)
private String mid;  // OK — 15 * 4 = 60 bytes

@Column(name = "description", length = 191)  // Safe limit for indexed utf8mb4 VARCHAR without large_prefix
private String description;
```

### Strict Mode (Required)

Ensure `sql_mode` includes `STRICT_TRANS_TABLES` and `NO_ZERO_IN_DATE` to prevent silent data truncation:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?sessionVariables=sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
```

### Flyway Migration Naming (MySQL)

```
db/migration/V1__create_merchants_table.sql
db/migration/V2__add_merchants_index.sql
```

Key conventions:
- Use `BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY` (not `BIGSERIAL`)
- MySQL DDL is **not transactional** — Flyway cannot roll back failed DDL migrations; keep migrations atomic
- Always specify `ENGINE=InnoDB` (default, but explicit for clarity)
- Use `utf8mb4` for all text columns (not `utf8`): `CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`

### HikariCP Configuration (MySQL)

```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.connection-test-query=SELECT 1
```

### MySQL-Specific Pitfalls

- `GROUP BY` in MySQL 5.7+ strict mode: all non-aggregate columns in SELECT must appear in GROUP BY
- Case-insensitive collation by default — use `BINARY` prefix or `utf8mb4_bin` for case-sensitive comparisons
- No partial indexes — use prefix indexes: `CREATE INDEX idx_email ON users (email(191))`
- `DATETIME` vs `TIMESTAMP`: `TIMESTAMP` is stored in UTC and auto-converted; `DATETIME` is stored as-is
- No `RETURNING` clause — use `getGeneratedKeys()` or `save()` return value
