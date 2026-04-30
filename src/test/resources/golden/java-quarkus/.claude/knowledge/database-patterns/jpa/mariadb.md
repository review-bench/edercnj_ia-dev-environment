---
name: jpa-mariadb
description: "JPA/Hibernate patterns specific to MariaDB 11: sequences, JSON columns, temporal tables, and Flyway integration."
requires-capabilities: [data.database.mariadb]
fragment-slot: { slot: jpa-engine, fragment-id: mariadb, fragment-order: 30 }
---

## JPA / MariaDB 11 Integration

### Entity ID Strategy

MariaDB 10.3+ supports true `SEQUENCE` objects. Use `GenerationType.IDENTITY` for simplicity or `SEQUENCE` for batch performance:

```java
// Simplest — AUTO_INCREMENT
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// High-throughput — true SEQUENCE (MariaDB 10.3+)
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "merchant_seq")
@SequenceGenerator(name = "merchant_seq", sequenceName = "merchants_id_seq", allocationSize = 50)
private Long id;
```

### MariaDB-Specific Features

```java
// JSON column (MariaDB 10.2+)
@Column(name = "metadata", columnDefinition = "JSON")
private String metadata;

// UUID stored as CHAR(36) or BINARY(16) for space efficiency
@Column(name = "external_id", columnDefinition = "BINARY(16)")
private UUID externalId;
```

### System-Versioned (Temporal) Tables

MariaDB supports SQL:2011 temporal tables natively — use for audit-without-triggers:

```sql
CREATE TABLE merchants (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    mid VARCHAR(15) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL
) WITH SYSTEM VERSIONING;
```

Query history:
```sql
SELECT * FROM merchants FOR SYSTEM_TIME AS OF '2026-01-01 00:00:00';
SELECT * FROM merchants FOR SYSTEM_TIME ALL WHERE id = 42;
```

### Flyway Migration Naming (MariaDB)

```
db/migration/V1__create_merchants_table.sql
db/migration/V2__add_temporal_versioning.sql
```

Key conventions:
- MariaDB DDL is **not transactional** (like MySQL) — keep migrations atomic
- Use `BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY` (compatible with MariaDB Connector/J)
- Specify `ENGINE=InnoDB CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
- Sequences: `CREATE SEQUENCE merchants_id_seq START WITH 1 INCREMENT BY 50`

### HikariCP Configuration (MariaDB)

```properties
spring.datasource.url=jdbc:mariadb://localhost:3306/mydb
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-test-query=SELECT 1
```

### MariaDB vs MySQL Differences

| Feature | MariaDB | MySQL |
|---------|---------|-------|
| True SEQUENCE | Yes (10.3+) | No (emulated) |
| Temporal tables | Yes (10.2+) | No native |
| Window functions | Yes (10.2+) | Yes (8.0+) |
| JSON path operators | Partial | Full (5.7+) |
| `RETURNING` clause | Yes (10.5+) | No |

### MariaDB-Specific Pitfalls

- MariaDB and MySQL JDBC URLs differ — use `mariadb://` scheme with MariaDB Connector/J
- `utf8mb4` required (not `utf8`) for full Unicode support including emoji
- MariaDB's `AUTO_INCREMENT` resets on server restart for InnoDB in older versions — use sequences for strict monotonicity
- `JSON` type in MariaDB is an alias for `LONGTEXT` (not a native JSON type until 10.5.2 with JSON_TABLE)
