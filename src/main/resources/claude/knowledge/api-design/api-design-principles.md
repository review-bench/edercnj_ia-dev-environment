# API Design Principles

Principles that apply to any synchronous API contract — REST, gRPC, GraphQL — before getting into protocol-specific conventions.

## Resources, Not Actions

Model your domain as resources (nouns) with a small, uniform set of operations.

```
GET    /orders            # list
POST   /orders            # create
GET    /orders/{id}       # read
PATCH  /orders/{id}       # partial update
DELETE /orders/{id}       # delete
```

Avoid action endpoints like `/createOrder`, `/getOrderById`, `/cancelOrderNow`. Reserve verb-style sub-resources for operations that genuinely cannot map to CRUD:

```
POST /orders/{id}:cancel        # state transition
POST /orders/{id}:refund        # idempotent state action
```

The Google AIP-136 "custom methods" convention uses `:verb` to make these visually distinct from resource paths.

## Idempotency

Mutating operations must be safe to retry. The client provides an idempotency key; the server stores it and returns the original result for repeat requests.

```
POST /payments
Idempotency-Key: 0193e4d2-..-..
```

- Keys are unique per logical operation, generated client-side
- Server retains keys for at least 24 hours (longer for high-value operations)
- Repeated keys with **different** bodies return `409 Conflict` (RFC 7807 problem)
- Repeated keys with **same** body return the original `2xx` response, not a new one

`GET`, `PUT`, `DELETE` are idempotent by HTTP definition; do not require idempotency keys on them. `POST` and `PATCH` need keys.

## Versioning

Pick one strategy per API surface and stick to it.

| Strategy | Example | When |
|----------|---------|------|
| URL path | `/v1/orders`, `/v2/orders` | Public APIs with long-lived clients |
| Custom header | `Api-Version: 2026-05-01` | Internal APIs; date-based works well |
| Media type | `Accept: application/vnd.example.v2+json` | Theoretical purity; few teams adopt |

The major version reflects breaking changes. Adding optional fields, new endpoints, or new enum values is **not** breaking — those go into the existing version.

### Breaking vs Non-Breaking

| Non-breaking (same version) | Breaking (new version) |
|-----------------------------|------------------------|
| Adding new endpoint | Removing endpoint |
| Adding optional field to request | Adding required field to request |
| Adding field to response | Removing field from response |
| Adding new enum value (consumers must accept unknown) | Renaming field |
| Relaxing validation | Changing field type |
| | Changing error code or response shape |
| | Changing default behavior |

If a change is breaking and the API has external consumers, follow the expand/contract pattern across at least two versions (Rule 09 mirrors this for DB migrations).

## Pagination

Cursor-based for any collection that can grow without bound.

```
GET /orders?limit=50&cursor=eyJsYXN0SWQiOiJvcmRfMTIzIn0=

200 OK
{
  "data": [...],
  "next_cursor": "eyJsYXN0SWQiOiJvcmRfMTczIn0=",
  "has_more": true
}
```

Avoid offset-based pagination for large datasets — page drift, slow performance, no stable view.

For small bounded sets (`< 1000`), offset/limit is acceptable but cap `limit` server-side.

## Error Responses — RFC 7807

All error responses follow Problem Details for HTTP APIs:

```json
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json

{
  "type": "https://api.example.com/problems/insufficient-inventory",
  "title": "Insufficient Inventory",
  "status": 422,
  "detail": "Cannot reserve 5 of SKU-A; only 2 available.",
  "instance": "/orders/ord_123",
  "trace_id": "0af7651916cd43dd8448eb211c80319c",
  "errors": [
    { "field": "items[0].quantity", "code": "INVENTORY_INSUFFICIENT", "available": 2 }
  ]
}
```

- `type` is a stable URI documenting the error class — clients dispatch on this, not on `title`
- `status` matches the HTTP status line
- `trace_id` enables support to correlate with logs
- `errors` array for field-level validation failures

Never return internal stack traces, SQL fragments, or implementation details in `detail` (Rule 12 J7).

## Authentication and Authorization

- **Authentication** at the edge (gateway/sidecar or framework filter), not per-controller.
- **Authorization** in the use-case layer, scoped to the resource (Rule 06).
- Bearer tokens (OAuth2/OIDC JWT) for service-to-user; mTLS for service-to-service.
- `401` for missing/invalid credentials; `403` for valid credentials without permission. Never the other way around.

## Rate Limiting

Public APIs MUST publish rate limit headers (Rule 06):

```
X-RateLimit-Limit:        1000
X-RateLimit-Remaining:    994
X-RateLimit-Reset:        1715620800
Retry-After:              60                    # only when 429
```

Different buckets for read vs write; tighter limits on auth endpoints.

## Consistency Across Endpoints

Within one API:
- All timestamps in UTC ISO-8601 with timezone suffix (`2026-05-11T14:30:00Z`)
- All money as integer minor units (`amount_cents: 1500`, `currency: "USD"`), never floats
- IDs as strings (`"ord_123"`), even when underlying storage is numeric — leaves room for prefixes/migration
- Field names: `snake_case` for JSON (or `camelCase` — pick one per API, never mix), `lower_snake_case` for gRPC/protobuf
- Boolean fields use positive phrasing (`is_active: true`, not `is_inactive: false`)

## Backward Compatibility for Clients

- New fields in responses must have a sensible default value; clients must ignore unknown fields
- Enum values: clients must accept unknown values gracefully (treat as the "default" or pass through)
- Don't change field semantics; add a new field instead and deprecate the old one

## Discoverability

- OpenAPI/AsyncAPI/protobuf schemas are the source of truth for the contract
- Schemas live in version control; CI fails when production code diverges from spec (`x-detect-spec-drift`)
- Generated clients (when offered) are versioned alongside the spec

## Observability Contract

Every API request/response cycle:
- Carries `traceparent` header (W3C Trace Context)
- Logs include `trace_id`, `request_id`, `route`, `status`, `duration_ms`
- Metrics expose the four golden signals per route (see `observability-principles.md`)

## Cross-References

- `rest-conventions.md` (HTTP-specific details)
- `knowledge/protocols/grpc-conventions.md` (gRPC specifics)
- `knowledge/api-design/api-deprecation-checklist.md`
- Rule 06 — `security-baseline.md` (rate limits, headers)
- RFC 7807 — Problem Details for HTTP APIs
- Google AIP — API Improvement Proposals
