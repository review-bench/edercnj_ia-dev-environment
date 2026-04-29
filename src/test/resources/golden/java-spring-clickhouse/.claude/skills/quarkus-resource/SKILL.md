---
name: quarkus-resource
description: "Generate a Quarkus RESTEasy Reactive @Path resource with DTOs, @RegisterForReflection, ExceptionMapper, and @QuarkusTest unit tests."
visibility: public
model: sonnet
requires-capabilities: [web.quarkus.framework]
allowed-tools: [Read, Write, Edit, Bash, Skill]
context-budget: light
---

# Skill: Quarkus Resource Generator

## Purpose

Generates a complete Quarkus inbound adapter (resource) layer following the hexagonal architecture pattern. Produces:

1. JAX-RS `@Path` resource class (RESTEasy Reactive)
2. Request/Response DTOs (`record` types with `@RegisterForReflection`)
3. Static mapper class
4. `ExceptionMapper<RuntimeException>` (or extends existing)
5. `@QuarkusTest` unit tests

## Triggers

- `/quarkus-resource <ResourceName> [--operations GET,POST,DELETE]` — generate full resource for a resource
- `/quarkus-resource <ResourceName> --crud` — generate standard CRUD
- `/quarkus-resource <ResourceName> --read-only` — generate GET endpoints only

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ResourceName` | Yes | PascalCase resource name (e.g., `Merchant`, `Transaction`) |
| `--operations` | No | Comma-separated HTTP methods (default: all) |
| `--crud` | No | GET list, GET by id, POST, PUT, DELETE |
| `--read-only` | No | GET list and GET by id only |
| `--path` | No | Override API path (default: `/api/v1/{resource-name-plural}`) |
| `--port` | No | Port interface name (default: `{ResourceName}ManagementPort`) |

## Workflow

### Step 1 — Read Project Context

Read existing conventions:
- `knowledge/stack-patterns/quarkus/index.md` — Quarkus patterns
- `knowledge/layer-templates.md` — Layer template patterns
- `knowledge/architecture-hexagonal.md` — Hexagonal architecture conventions
- Existing resources in `src/main/java/**/adapter/inbound/` for naming conventions

### Step 2 — Generate DTOs

```java
// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Request.java
@RegisterForReflection
public record {ResourceName}Request(
    @NotBlank @Size(max = N) String field1,
    @NotNull Type field2
) {}

// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Response.java
@RegisterForReflection
public record {ResourceName}Response(Long id, String field1, Type field2) {}
```

### Step 3 — Generate Mapper

```java
// src/main/java/{package}/adapter/inbound/rest/{ResourceName}DtoMapper.java
public final class {ResourceName}DtoMapper {
    private {ResourceName}DtoMapper() {}

    public static {DomainType} toDomain({ResourceName}Request request) { ... }
    public static {ResourceName}Response toResponse({DomainType} domain) { ... }
}
```

### Step 4 — Generate Resource

Follow `knowledge/stack-patterns/quarkus/index.md` §4 RESTEasy Reactive pattern:

```java
// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Resource.java
@Path("{path}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class {ResourceName}Resource {
    // @Inject constructor injection of port
    // Endpoints per --operations with @Transactional on writes
}
```

### Step 5 — Generate/Update ExceptionMapper

Check if `SimulatorExceptionMapper` (or similar) exists in `adapter/inbound/rest/`:
- If exists: add missing switch cases for domain exceptions
- If not exists: generate new `@Provider ExceptionMapper<RuntimeException>` following §4 pattern

### Step 6 — Generate Tests

```java
// src/test/java/{package}/adapter/inbound/rest/{ResourceName}ResourceTest.java
@QuarkusTest
class {ResourceName}ResourceTest {
    @InjectMock {ResourceName}ManagementPort port;

    // RestAssured-based tests per endpoint per scenario
}
```

## Output Checklist

- [ ] DTOs are records with `@RegisterForReflection`
- [ ] Validation annotations on request fields (`@NotBlank`, `@NotNull`, `@Size`)
- [ ] `@Transactional` on POST/PUT/DELETE endpoints
- [ ] Constructor injection with `@Inject` (not field injection)
- [ ] HTTP status codes correct: 200 GET, 201 POST with `Response.status(201)`, 204 DELETE
- [ ] Switch expression in ExceptionMapper covers all domain exceptions + default
- [ ] Tests use `@QuarkusTest` + RestAssured (not `@SpringBootTest`)
- [ ] Tests cover: success path, not found (404), conflict (409), validation failure (400)

## Knowledge Pack References

- `knowledge/stack-patterns/quarkus/index.md` — Quarkus patterns (§4 RESTEasy, exception mapper)
- `knowledge/layer-templates.md` — Layer template patterns
- `knowledge/architecture-hexagonal.md` — Hexagonal architecture
