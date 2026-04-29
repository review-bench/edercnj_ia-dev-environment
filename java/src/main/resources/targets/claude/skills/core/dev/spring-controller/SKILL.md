---
name: spring-controller
description: "Generate a Spring Boot @RestController with matching DTOs, mappers, @ControllerAdvice handler, and unit tests following hexagonal architecture."
visibility: public
model: sonnet
requires-capabilities: [web.spring.boot]
allowed-tools: [Read, Write, Edit, Bash, Skill]
---

# Skill: Spring Controller Generator

## Purpose

Generates a complete Spring Boot inbound adapter (controller) layer following the hexagonal architecture pattern established by this project. Produces:

1. `@RestController` class with CRUD or custom endpoints
2. Request/Response DTOs (`record` types with `@RegisterReflectionForBinding`)
3. Static mapper class (`DtoMapper`)
4. `@RestControllerAdvice` exception handler (or extends existing)
5. Unit tests for controller and mapper

## Triggers

- `/spring-controller <ResourceName> [--operations GET,POST,DELETE]` — generate full controller for a resource
- `/spring-controller <ResourceName> --crud` — generate standard CRUD (GET list, GET by id, POST, PUT, DELETE)
- `/spring-controller <ResourceName> --read-only` — generate GET endpoints only

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `ResourceName` | Yes | PascalCase resource name (e.g., `Merchant`, `Transaction`) |
| `--operations` | No | Comma-separated HTTP methods to generate (default: all) |
| `--crud` | No | Shorthand for GET list, GET by id, POST, PUT, DELETE |
| `--read-only` | No | Shorthand for GET list and GET by id only |
| `--path` | No | Override API path (default: `/api/v1/{resource-name-plural}`) |
| `--port` | No | Port interface name (default: `{ResourceName}ManagementPort`) |

## Workflow

### Step 1 — Read Project Context

Read existing conventions:
- `knowledge/stack-patterns/spring/index.md` — Spring Boot patterns
- `knowledge/layer-templates.md` — Layer template patterns
- `knowledge/architecture-hexagonal.md` — Hexagonal architecture conventions
- Existing controller in `src/main/java/**/adapter/inbound/` for naming conventions

### Step 2 — Generate DTOs

Create request and response records following Spring patterns:

```java
// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Request.java
@RegisterReflectionForBinding
public record {ResourceName}Request(
    @NotBlank @Size(max = N) String field1,
    @NotNull Type field2
) {}

// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Response.java
@RegisterReflectionForBinding
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

### Step 4 — Generate Controller

Follow `knowledge/stack-patterns/spring/index.md` §4 RestController pattern:

```java
// src/main/java/{package}/adapter/inbound/rest/{ResourceName}Controller.java
@RestController
@RequestMapping("{path}")
public class {ResourceName}Controller {
    // Constructor injection of port
    // Endpoints per --operations
}
```

### Step 5 — Generate/Update Exception Handler

Check if `GlobalExceptionHandler` exists in `adapter/inbound/rest/`:
- If exists: add missing `@ExceptionHandler` methods for domain exceptions
- If not exists: generate new `GlobalExceptionHandler` following §5 pattern

### Step 6 — Generate Tests

```java
// src/test/java/{package}/adapter/inbound/rest/{ResourceName}ControllerTest.java
@WebMvcTest({ResourceName}Controller.class)
class {ResourceName}ControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean {ResourceName}ManagementPort port;

    // One test per endpoint per scenario (happy path + error paths)
}
```

## Output Checklist

- [ ] DTOs are records with `@RegisterReflectionForBinding`
- [ ] Validation annotations on all request fields (`@NotBlank`, `@NotNull`, `@Size`, etc.)
- [ ] Mapper is a final class with private constructor (static methods only)
- [ ] Controller uses constructor injection (no `@Autowired` on fields)
- [ ] HTTP status codes correct: 200 GET, 201 POST, 204 DELETE/PUT with no body
- [ ] `@ExceptionHandler` present for each domain exception type
- [ ] Tests cover: success path, not found (404), conflict (409), validation failure (400)
- [ ] Tests use `MockMvc` with `@WebMvcTest` (not `@SpringBootTest`)

## Knowledge Pack References

- `knowledge/stack-patterns/spring/index.md` — Spring Boot patterns (§4 RestController, §5 ControllerAdvice)
- `knowledge/layer-templates.md` — Layer template patterns
- `knowledge/architecture-hexagonal.md` — Hexagonal architecture
- `knowledge/coding-standards/coding-conventions.md` — Naming conventions
