---
name: micronaut-scaffold
description: "Scaffolds a Micronaut service with @Controller, DI, health, Dockerfile, and tests."
visibility: public
model: sonnet
requires-capabilities: [web.micronaut.framework]
allowed-tools: [Read, Write, Edit, Bash, Skill]
---

# Skill: Micronaut Scaffold Generator

## Purpose

Scaffolds a complete Micronaut service module following the hexagonal architecture pattern. Produces:

1. `Application.java` entrypoint
2. `@Controller` class with endpoints
3. `@Singleton` service and port wiring
4. `HealthIndicator` implementation
5. Configuration (`application.yml`)
6. `Dockerfile` (multi-stage build)
7. Integration tests (`@MicronautTest`)

## Triggers

- `/micronaut-scaffold <ServiceName>` — scaffold Micronaut service
- `/micronaut-scaffold <ServiceName> --graalvm` — include GraalVM native hints

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ServiceName` | Yes | PascalCase service name |
| `--graalvm` | No | Add `@ReflectiveAccess` and `reflect-config.json` for native image |
| `--port` | No | HTTP port (default: 8080) |

## Artifacts Generated

1. `src/main/java/.../Application.java` — Application entrypoint
2. `src/main/java/.../adapter/inbound/rest/{ServiceName}Controller.java` — HTTP controller
3. `src/main/java/.../adapter/inbound/health/{ServiceName}HealthIndicator.java` — Health indicator
4. `src/main/java/.../config/AppConfig.java` — Configuration binding
5. `src/main/resources/application.yml` — Service configuration
6. `Dockerfile` — Multi-stage Docker build
7. `src/test/java/.../adapter/inbound/rest/{ServiceName}ControllerTest.java` — Integration test

## Workflow

### Step 1 — Read Project Context

- `knowledge/stack-patterns/micronaut/index.md` — Micronaut patterns
- `knowledge/layer-templates.md` — Layer templates
- `knowledge/architecture-hexagonal.md` — Hexagonal conventions

### Step 2 — Generate Application Entrypoint

```java
public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
```

### Step 3 — Generate Controller

Follow `knowledge/stack-patterns/micronaut/index.md` §1 Controller pattern:

```java
@Controller("/api/v1/{resource}")
public class {ServiceName}Controller {
    private final {Port} port;

    public {ServiceName}Controller({Port} port) {
        this.port = port;
    }

    @Get
    public List<{Response}> list() { ... }

    @Post
    @Status(HttpStatus.CREATED)
    public {Response} create(@Valid @Body {Request} request) { ... }
}
```

### Step 4 — Generate Health Indicator

Follow `knowledge/stack-patterns/micronaut/index.md` §6 Health Checks pattern.

### Step 5 — Generate Configuration

```yaml
# src/main/resources/application.yml
micronaut:
  application:
    name: {service-name}
  server:
    port: 8080
app:
  feature:
    enabled: true
    max-items: 100
```

### Step 6 — Generate Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 7 — Generate Tests

```java
@MicronautTest
class {ServiceName}ControllerTest {

    @Inject
    {ServiceName}Client client;  // Declarative HTTP client injected

    @MockBean({Port}.class)
    {Port} port() { return mock({Port}.class); }

    @Test
    void list_returnsOk() {
        var result = client.list();
        assertThat(result).isNotNull();
    }
}
```

## Output Checklist

- [ ] Compile-time DI — no reflection-based injection
- [ ] No `@Autowired` (Spring annotation — forbidden)
- [ ] Constructor injection without extra annotations (`@Inject` not needed in Micronaut)
- [ ] `@Valid` on controller request parameters
- [ ] Health endpoint at `/health/liveness` and `/health/readiness`
- [ ] Config via `application.yml` (no hardcoded values)
- [ ] Dockerfile uses non-root user

## Knowledge Pack References

- `knowledge/stack-patterns/micronaut/index.md` — Micronaut patterns
- `knowledge/layer-templates.md` — Layer templates
