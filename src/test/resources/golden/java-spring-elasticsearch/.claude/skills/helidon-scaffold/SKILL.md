---
name: helidon-scaffold
description: "Scaffolds a Helidon SE/MP service with routing, health, config, Dockerfile, and tests."
visibility: public
model: sonnet
requires-capabilities: [web.helidon.framework]
allowed-tools: [Read, Write, Edit, Bash, Skill]
context-budget: light
---

# Skill: Helidon Scaffold Generator

## Purpose

Scaffolds a complete Helidon service module following the hexagonal architecture pattern. Supports both Helidon SE (reactive, functional) and Helidon MP (MicroProfile, CDI + JAX-RS). Produces:

1. Main application entrypoint (SE: `WebServer` builder; MP: `Main.java`)
2. Resource/Service class (SE: routing; MP: `@Path` resource)
3. Health check implementations
4. Configuration setup
5. `Dockerfile` (multi-stage build)
6. Integration tests

## Triggers

- `/helidon-scaffold <ServiceName>` — scaffold Helidon MP service (default)
- `/helidon-scaffold <ServiceName> --se` — scaffold Helidon SE reactive service
- `/helidon-scaffold <ServiceName> --mp` — scaffold Helidon MP service explicitly

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ServiceName` | Yes | PascalCase service name (e.g., `PaymentService`, `InventoryService`) |
| `--se` | No | Use Helidon SE (reactive, functional routing) |
| `--mp` | No | Use Helidon MP (MicroProfile, CDI + JAX-RS) — default |
| `--port` | No | HTTP port (default: 8080) |

## Artifacts Generated

1. `src/main/java/.../Main.java` — Application entrypoint
2. `src/main/java/.../adapter/inbound/rest/{ServiceName}Resource.java` — HTTP endpoint
3. `src/main/java/.../adapter/inbound/health/AppLivenessCheck.java` — Liveness probe
4. `src/main/java/.../adapter/inbound/health/AppReadinessCheck.java` — Readiness probe
5. `src/main/resources/application.yaml` — Service configuration
6. `Dockerfile` — Multi-stage Docker build (JVM)
7. `src/test/java/.../adapter/inbound/rest/{ServiceName}ResourceTest.java` — Integration test

## Workflow

### Step 1 — Read Project Context

- `knowledge/stack-patterns/helidon/index.md` — Helidon patterns
- `knowledge/layer-templates.md` — Layer templates
- `knowledge/architecture-hexagonal.md` — Hexagonal conventions

### Step 2 — Generate Entrypoint

**Helidon MP:**
```java
public class Main {
    public static void main(String[] args) {
        io.helidon.microprofile.server.Main.main(args);
    }
}
```

**Helidon SE:**
```java
public class Main {
    public static void main(String[] args) {
        var server = WebServer.builder()
            .config(Config.global().get("server"))
            .routing(routing -> routing
                .register("/api/v1", new {ServiceName}Service())
                .get("/health/live", (req, res) -> res.send("UP"))
            )
            .build()
            .start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }
}
```

### Step 3 — Generate Resource (MP)

Follow `knowledge/stack-patterns/helidon/index.md` §1 JAX-RS pattern.

### Step 4 — Generate Health Checks

Follow `knowledge/stack-patterns/helidon/index.md` §4 MicroProfile Health pattern.

### Step 5 — Generate Dockerfile

Multi-stage build:
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

### Step 6 — Generate Tests

Integration test using `HelidonTest` annotation (MP) or `WebServer` programmatic start (SE).

## Output Checklist

- [ ] Non-root user in Dockerfile (`USER appuser`)
- [ ] Health endpoints `/health/live` and `/health/ready` implemented
- [ ] Config externalized via `application.yaml` (no hardcoded values)
- [ ] Graceful shutdown handled (SE: `shutdownHook`; MP: automatic)
- [ ] Tests do not use `Thread.sleep()` for async waiting

## Knowledge Pack References

- `knowledge/stack-patterns/helidon/index.md` — Helidon patterns
- `knowledge/layer-templates.md` — Layer templates
