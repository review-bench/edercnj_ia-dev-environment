# gRPC Conventions

Proto3 style, service / method naming, error handling, and operational patterns for gRPC services.

## Proto File Layout

```
proto/
  example/
    order/
      v1/
        order_service.proto     # service definitions
        order.proto             # message definitions
        events.proto            # async events (if any)
```

- One directory per major version. Bumping versions creates `v2/` alongside `v1/`.
- Package matches directory: `package example.order.v1;`
- Java options to control generated code:
  ```proto
  option java_multiple_files = true;
  option java_package = "com.example.order.v1";
  option java_outer_classname = "OrderProto";
  option go_package = "github.com/example/order/v1;orderv1";
  ```

## Service and Method Naming

```proto
service OrderService {
  rpc GetOrder(GetOrderRequest) returns (Order);
  rpc ListOrders(ListOrdersRequest) returns (ListOrdersResponse);
  rpc CreateOrder(CreateOrderRequest) returns (Order);
  rpc UpdateOrder(UpdateOrderRequest) returns (Order);
  rpc DeleteOrder(DeleteOrderRequest) returns (google.protobuf.Empty);
  rpc CancelOrder(CancelOrderRequest) returns (Order);   // custom verb
}
```

| Convention | Rule |
|------------|------|
| Service name | PascalCase noun + `Service` suffix |
| Method name | PascalCase verb-noun (`GetOrder`, `ListOrders`, `CancelOrder`) |
| Standard verbs | `Get`, `List`, `Create`, `Update`, `Delete`, `Search`, `Batch{Verb}` |
| Custom verbs | Domain-specific actions: `CancelOrder`, `RefundPayment`, `Authorize{Resource}` |
| Request/Response | `{Method}Request` and `{Method}Response`; if the response is a single resource, use the resource type directly |

Avoid:
- `GetOrderById` — the request message carries the ID
- `Order` as a method name — methods are verbs
- Overly chatty methods (`GetOrderTotal` separate from `GetOrder` — return the field in `Order`)

## Message Design

### Naming and Types

```proto
message Order {
  string id = 1;                              // server-generated, prefixed: "ord_..."
  string customer_id = 2;
  google.protobuf.Timestamp created_at = 3;
  google.protobuf.Timestamp updated_at = 4;
  OrderStatus status = 5;
  Money total = 6;
  repeated OrderItem items = 7;
  string idempotency_key = 8;                 // for create flows
}

enum OrderStatus {
  ORDER_STATUS_UNSPECIFIED = 0;               // mandatory zero-default
  ORDER_STATUS_PENDING     = 1;
  ORDER_STATUS_CONFIRMED   = 2;
  ORDER_STATUS_SHIPPED     = 3;
  ORDER_STATUS_CANCELED    = 4;
}
```

| Convention | Rule |
|------------|------|
| Field name | `lower_snake_case`, single-word preferred |
| Field number | Stable forever — never reuse a number after removal |
| Reserved | Use `reserved 5, 7;` and `reserved "old_field";` for retired fields |
| Enum prefix | All values prefixed with enum name; `_UNSPECIFIED = 0` is mandatory |
| Timestamps | `google.protobuf.Timestamp`, never `int64` epoch |
| Money | Custom `Money` message with `amount_micros` (int64) and `currency_code` (string) |
| IDs | `string`, with deterministic prefix per type |

### Optional and Required

In proto3, all scalar fields have a zero default. Use `optional` (re-introduced in proto3.15+) when you must distinguish "absent" from "default":

```proto
message UpdateCustomerRequest {
  string customer_id = 1;
  optional string email = 2;        // null means "don't update"
  optional bool active = 3;
}
```

Avoid `optional` for collections (`repeated`) — `[]` already means absent.

### Field Numbers

| Range | Use |
|-------|-----|
| 1 – 15 | High-frequency fields (1-byte wire tag) |
| 16 – 2047 | Standard fields (2-byte wire tag) |
| 19000 – 19999 | Reserved by protobuf |
| 536870911 | Maximum |

Keep IDs, timestamps, and other always-present fields in 1-15.

## Error Handling

Use canonical `google.rpc.Code`:

| Code | When | HTTP equiv |
|------|------|------------|
| `OK` (0) | Success | 200 |
| `INVALID_ARGUMENT` (3) | Malformed or invalid input | 400 |
| `UNAUTHENTICATED` (16) | Credentials missing/invalid | 401 |
| `PERMISSION_DENIED` (7) | Authn ok, authz denied | 403 |
| `NOT_FOUND` (5) | Resource missing | 404 |
| `ALREADY_EXISTS` (6) | Conflict on create | 409 |
| `FAILED_PRECONDITION` (9) | State precondition not met | 422 |
| `RESOURCE_EXHAUSTED` (8) | Quota / rate limit | 429 |
| `INTERNAL` (13) | Unhandled server error | 500 |
| `UNAVAILABLE` (14) | Transient downstream failure | 503 |
| `DEADLINE_EXCEEDED` (4) | Timeout exceeded | 504 |

Attach structured details using `google.rpc.Status` and `ErrorInfo` / `BadRequest` / `QuotaFailure`:

```proto
// Use grpc.Status.details to carry google.rpc.ErrorInfo
{
  code: INVALID_ARGUMENT
  message: "items[0].quantity must be positive"
  details: [
    google.rpc.BadRequest {
      field_violations: [
        { field: "items[0].quantity", description: "must be > 0" }
      ]
    }
  ]
}
```

Never leak stack traces or internal messages in the status (Rule 12 J7 applies to gRPC too).

## Streaming Patterns

| Stream type | Use case |
|-------------|----------|
| Unary | Most calls (request → response) |
| Server-streaming | Server pushes many responses (live feed, large list) |
| Client-streaming | Client uploads many requests (batch ingest) |
| Bidirectional | Real-time chat, telemetry, long-lived sessions |

For streaming:
- Send keep-alives on long-idle streams
- Server enforces max stream duration; clients reconnect
- Backpressure: if the consumer is slow, the producer must slow down or close — never buffer unbounded

## Deadlines and Timeouts

- **Clients always set a deadline.** No deadline means infinite wait.
- **Servers propagate deadlines.** When calling downstream, pass the remaining deadline.
- Default deadline per call: 5 seconds. Override per method based on workload.

```java
stub.withDeadlineAfter(2, TimeUnit.SECONDS)
    .getOrder(GetOrderRequest.newBuilder().setId(id).build());
```

A server processing a request whose deadline has passed should abort with `DEADLINE_EXCEEDED`.

## Authentication

| Mechanism | When |
|-----------|------|
| mTLS | Service-to-service inside the mesh |
| OAuth2 / JWT in metadata | Service-to-user |
| API key in metadata | Internal back-office tools (low security) |

Metadata key for tokens: `authorization: Bearer <token>` — matches HTTP convention.

## Health Checking

Implement [`grpc.health.v1.Health`](https://grpc.github.io/grpc/core/md_doc_health-checking.html):

```proto
package grpc.health.v1;
service Health {
  rpc Check(HealthCheckRequest) returns (HealthCheckResponse);
  rpc Watch(HealthCheckRequest) returns (stream HealthCheckResponse);
}
```

Load balancers and service meshes use this protocol. Implement it once per service.

## Reflection

Disable gRPC reflection in production deployments. It's useful for local testing but exposes the service surface (Rule 06 — minimize attack surface). Provide proto files via package registry or schema registry instead.

## Versioning

Major version goes in the package path: `example.order.v1`, `example.order.v2`. Both versions coexist as separate generated code; servers may implement both. Field-additive changes happen within a version (proto3 allows this for forward and backward compatibility).

Breaking changes (renumber field, change type, remove field) require a new version. Use `reserved` to retire field numbers and names:

```proto
message Order {
  reserved 4, 7;
  reserved "old_status";
}
```

## Observability

- Server interceptor emits the four golden signals per method (latency histogram, RPS counter, error rate, in-flight gauge)
- Client interceptor propagates `traceparent` via gRPC metadata
- Log entries include method name, deadline remaining, peer address, status code

## Cross-References

- `api-design-principles.md` (protocol-agnostic API design)
- `event-driven-conventions.md` (async messaging)
- `knowledge/testing/contract-grpc.md`
- `knowledge/performance-engineering/performance-grpc.md`
- Google API Design Guide — `https://cloud.google.com/apis/design`
- gRPC Status Codes — `https://grpc.github.io/grpc/core/md_doc_statuscodes.html`
