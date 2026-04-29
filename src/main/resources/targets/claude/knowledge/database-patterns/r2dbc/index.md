---
name: r2dbc
description: "R2DBC reactive database access patterns: repository, entity mapping, transactions, and connection pool configuration."
requires-capabilities: [data.database.postgres]
excludes-capabilities: []
---

# Pattern: R2DBC Reactive Database Access

> **Mutex with JPA:** R2DBC and JPA/Hibernate are mutually exclusive in the same application module. Choose R2DBC for reactive/non-blocking services, JPA for traditional blocking services.

## Purpose

R2DBC provides non-blocking, reactive access to relational databases. Use R2DBC when the application must not block threads while waiting for database I/O — typically in Quarkus reactive or Spring WebFlux projects.

---

## 1. Repository Pattern (R2DBC)

```java
@Repository
public interface MerchantR2dbcRepository extends ReactiveCrudRepository<MerchantRow, Long> {

    Mono<MerchantRow> findByMid(String mid);

    Flux<MerchantRow> findByStatus(String status);

    @Query("SELECT * FROM merchants WHERE status = :status ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<MerchantRow> findByStatusPaged(String status, int limit, int offset);

    Mono<Boolean> existsByMid(String mid);
}
```

---

## 2. Entity / Row Mapping

R2DBC uses `@Table` and `@Column` from `spring-data-relational` (NOT JPA):

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("merchants")
public record MerchantRow(
    @Id Long id,
    @Column("mid") String mid,
    @Column("name") String name,
    @Column("status") String status,
    @Column("created_at") OffsetDateTime createdAt,
    @Column("updated_at") OffsetDateTime updatedAt
) {}
```

No `@Entity`, no `@GeneratedValue`, no JPA annotations.

---

## 3. Reactive Transaction Management

```java
@Service
public class MerchantService {

    private final MerchantR2dbcRepository repository;
    private final TransactionalOperator txOperator;

    public MerchantService(MerchantR2dbcRepository repository,
                           TransactionalOperator txOperator) {
        this.repository = repository;
        this.txOperator = txOperator;
    }

    public Mono<MerchantRow> create(MerchantRow merchant) {
        return repository.save(merchant)
            .as(txOperator::transactional);  // Wrap in reactive transaction
    }

    // Or use @Transactional (reactive context-aware in Spring 5.3+)
    @Transactional
    public Mono<MerchantRow> update(Long id, String name) {
        return repository.findById(id)
            .flatMap(existing -> repository.save(new MerchantRow(
                existing.id(), existing.mid(), name,
                existing.status(), existing.createdAt(), OffsetDateTime.now()
            )));
    }
}
```

---

## 4. Connection Pool (r2dbc-pool)

```properties
# PostgreSQL R2DBC
spring.r2dbc.url=r2dbc:postgresql://localhost:5432/mydb
spring.r2dbc.username=app
spring.r2dbc.password=${DB_PASSWORD}
spring.r2dbc.pool.initial-size=5
spring.r2dbc.pool.max-size=20
spring.r2dbc.pool.max-idle-time=10m
spring.r2dbc.pool.max-life-time=30m
spring.r2dbc.pool.validation-query=SELECT 1
```

---

## 5. Database Migration (Flyway with R2DBC)

R2DBC does not run Flyway directly (Flyway is JDBC-based). Configure a JDBC datasource for Flyway only:

```java
@Configuration
@ConditionalOnProperty("spring.flyway.enabled")
public class FlywayConfig {

    @Bean
    public Flyway flyway(@Value("${spring.datasource.url}") String jdbcUrl,
                         @Value("${spring.r2dbc.username}") String user,
                         @Value("${spring.r2dbc.password}") String password) {
        return Flyway.configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .load();
    }
}
```

```properties
# Separate JDBC URL for Flyway (same database, JDBC scheme)
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
```

---

## 6. R2DBC vs JPA Decision Matrix

| Factor | Use R2DBC | Use JPA |
|--------|-----------|---------|
| Concurrency model | Reactive / non-blocking (WebFlux, Quarkus Reactive) | Blocking (Spring MVC, Quarkus Synchronous) |
| ORM features needed | No (manual mapping) | Yes (lazy loading, L2 cache, criteria API) |
| Connection efficiency | High (fewer threads needed) | Moderate (1 thread per connection) |
| Learning curve | Higher | Lower |
| Native query support | Full | Full |
| Relationship support | Manual joins only | `@OneToMany`, `@ManyToMany` |

---

## Anti-Patterns

- Mixing R2DBC and JPA (`EntityManager`) in the same Spring application context
- Blocking calls (JDBC, `Thread.sleep()`) inside reactive chains — blocks Netty I/O threads
- Missing `@Transactional` or `TransactionalOperator` on write operations
- Large `Flux` collections without pagination (unbounded reactive stream)
- Using `block()` in production code (defeats non-blocking design)
