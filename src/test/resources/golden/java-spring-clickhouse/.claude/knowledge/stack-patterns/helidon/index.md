---
name: helidon
description: "Helidon SE/MP patterns: routing, health checks, config, CDI (MP), and reactive I/O (SE)."
requires-capabilities: [web.helidon.framework]
---

# Pattern: Helidon Patterns

## Purpose

Provides Helidon-specific implementation patterns for web services. Agents reference this pack when generating code for a Java 21 + Helidon project.

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with Helidon-specific conventions.

---

## 1. Helidon MP (MicroProfile) — CDI + JAX-RS

Helidon MP follows the MicroProfile specification. Use `@ApplicationScoped` (CDI) and `@Path` (JAX-RS).

```java
@Path("/api/v1/merchants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class MerchantResource {

    private final MerchantManagementPort service;

    @Inject
    public MerchantResource(MerchantManagementPort service) {
        this.service = service;
    }

    @GET
    public List<MerchantResponse> list() {
        return service.listAll().stream()
            .map(MerchantDtoMapper::toResponse)
            .toList();
    }

    @POST
    public Response create(@Valid CreateMerchantRequest request) {
        var created = service.create(MerchantDtoMapper.toDomain(request));
        return Response.status(201).entity(MerchantDtoMapper.toResponse(created)).build();
    }
}
```

---

## 2. Helidon SE — Reactive Routing

Helidon SE is a functional, reactive API (no CDI).

```java
public class MerchantService {

    public static WebServer startServer(MerchantManagementPort port) {
        var routing = HttpRouting.builder()
            .get("/api/v1/merchants", (req, res) -> {
                var merchants = port.listAll();
                res.send(merchants.stream().map(MerchantDtoMapper::toResponse).toList());
            })
            .post("/api/v1/merchants", (req, res) -> {
                req.content().as(CreateMerchantRequest.class).thenAccept(request -> {
                    var created = port.create(MerchantDtoMapper.toDomain(request));
                    res.status(201).send(MerchantDtoMapper.toResponse(created));
                });
            })
            .build();

        return WebServer.builder()
            .routing(routing)
            .build()
            .start();
    }
}
```

---

## 3. Configuration (MicroProfile Config)

```java
@ApplicationScoped
public class AppConfig {

    @Inject
    @ConfigProperty(name = "app.feature.enabled", defaultValue = "true")
    private boolean featureEnabled;

    @Inject
    @ConfigProperty(name = "app.feature.max-items", defaultValue = "100")
    private int maxItems;
}
```

---

## 4. Health Checks (MicroProfile Health)

```java
@Liveness
@ApplicationScoped
public class AppLivenessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.up("alive");
    }
}

@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {
    @Override
    public HealthCheckResponse call() {
        // Check database connectivity
        return HealthCheckResponse.up("database");
    }
}
```

### Endpoints

| Path | Purpose |
|------|---------|
| `/health/live` | Liveness |
| `/health/ready` | Readiness |
| `/health` | Combined |

---

## Anti-Patterns (Helidon-Specific)

- Blocking the reactive event loop in Helidon SE (use async APIs)
- Mixing SE and MP patterns in the same module
- Missing `@ApplicationScoped` on CDI beans in MP
- Ignoring backpressure in reactive streams (SE)
