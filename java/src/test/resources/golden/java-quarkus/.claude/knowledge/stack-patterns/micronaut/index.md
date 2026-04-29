---
name: micronaut
description: "Micronaut patterns: @Controller, @Singleton, @ConfigurationProperties, compile-time DI, HTTP client, and health indicators."
requires-capabilities: [web.micronaut.framework]
---

# Pattern: Micronaut Patterns

## Purpose

Provides Micronaut-specific implementation patterns for web services. Agents reference this pack when generating code for a Java 21 + Micronaut project.

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with Micronaut-specific conventions.

---

## 1. Controller Pattern

Micronaut uses compile-time DI — no reflection at runtime.

```java
@Controller("/api/v1/merchants")
public class MerchantController {

    private final MerchantManagementPort service;

    public MerchantController(MerchantManagementPort service) {
        this.service = service;
    }

    @Get
    public List<MerchantResponse> list() {
        return service.listAll().stream()
            .map(MerchantDtoMapper::toResponse)
            .toList();
    }

    @Post
    @Status(HttpStatus.CREATED)
    public MerchantResponse create(@Valid @Body CreateMerchantRequest request) {
        var created = service.create(MerchantDtoMapper.toDomain(request));
        return MerchantDtoMapper.toResponse(created);
    }

    @Get("/{id}")
    public Optional<MerchantResponse> findById(Long id) {
        return service.findById(id).map(MerchantDtoMapper::toResponse);
    }

    @Delete("/{id}")
    @Status(HttpStatus.NO_CONTENT)
    public void delete(Long id) {
        service.deactivate(id);
    }
}
```

---

## 2. Bean Scopes

| Annotation | Scope | Use |
|------------|-------|-----|
| `@Singleton` | Single instance | Stateless services (default) |
| `@Prototype` | New per injection | Stateful helpers |
| `@RequestScope` | Per HTTP request | Request-scoped state |

```java
@Singleton
public class MerchantService implements MerchantManagementPort {
    // Stateless — safe as singleton
}
```

---

## 3. Configuration

```java
@ConfigurationProperties("app.feature")
public class FeatureConfig {

    private boolean enabled = true;
    private int maxItems = 100;
    private int timeoutSeconds = 30;

    // Getters and setters (required for @ConfigurationProperties)
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }
}
```

---

## 4. Error Handling

```java
@Produces
@Singleton
public class MerchantExceptionHandler implements ExceptionHandler<MerchantNotFoundException, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, MerchantNotFoundException exception) {
        return HttpResponse.notFound(Map.of("error", exception.getMessage()));
    }
}
```

---

## 5. HTTP Client (Declarative)

```java
@Client("/api/v1")
public interface MerchantClient {

    @Get("/merchants")
    List<MerchantResponse> listAll();

    @Post("/merchants")
    @Status(HttpStatus.CREATED)
    MerchantResponse create(@Body CreateMerchantRequest request);
}
```

---

## 6. Health Checks

```java
@Singleton
public class DatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Publisher<HealthResult> getResult() {
        // Check database connectivity
        return Mono.just(HealthResult.healthy("database"));
    }
}
```

### Endpoints

| Path | Purpose |
|------|---------|
| `/health/liveness` | Liveness |
| `/health/readiness` | Readiness |
| `/health` | Combined |

---

## Anti-Patterns (Micronaut-Specific)

- Reflection-based DI (Micronaut uses AOT compile-time DI — reflection breaks GraalVM native)
- `@Autowired` (Spring annotation — use constructor injection without annotations)
- Mutable state in `@Singleton` beans
- Missing `@Valid` on controller method parameters (validation is opt-in)
- Using `Optional` as controller return type without configuring 404 behavior explicitly
