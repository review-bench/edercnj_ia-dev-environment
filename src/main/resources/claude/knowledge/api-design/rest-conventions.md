# REST Conventions

HTTP-specific conventions that complement the protocol-agnostic principles in `api-design-principles.md`.

## HTTP Methods

| Method | Semantics | Idempotent | Cacheable | Body |
|--------|-----------|------------|-----------|------|
| `GET` | Retrieve | Yes | Yes | None (use query params) |
| `HEAD` | Retrieve metadata only | Yes | Yes | None |
| `POST` | Create or non-idempotent action | No | No | Required for create |
| `PUT` | Full replacement | Yes | No | Required, complete representation |
| `PATCH` | Partial update | No (unless using JSON Patch with ETag) | No | Required |
| `DELETE` | Remove | Yes | No | Usually none |
| `OPTIONS` | Discover allowed methods / CORS preflight | Yes | No | None |

Use `PATCH` with [JSON Merge Patch (RFC 7396)](https://www.rfc-editor.org/rfc/rfc7396) for simple partial updates, or [JSON Patch (RFC 6902)](https://www.rfc-editor.org/rfc/rfc6902) when atomicity and ordering matter.

## Status Codes

### Success (2xx)

| Code | When |
|------|------|
| `200 OK` | Successful GET, PUT, PATCH, DELETE with response body |
| `201 Created` | Successful POST that created a resource; include `Location` header pointing to the new resource |
| `202 Accepted` | Request acknowledged but not yet processed (async) |
| `204 No Content` | Successful DELETE or PUT with no response body |

### Client Errors (4xx)

| Code | When |
|------|------|
| `400 Bad Request` | Malformed syntax (invalid JSON, missing required field at parse time) |
| `401 Unauthorized` | No or invalid credentials |
| `403 Forbidden` | Valid credentials, insufficient permission |
| `404 Not Found` | Resource does not exist OR access is denied without revealing existence |
| `405 Method Not Allowed` | Method not supported on this resource; include `Allow` header |
| `409 Conflict` | State conflict (idempotency key mismatch, optimistic-lock failure) |
| `410 Gone` | Resource removed permanently |
| `412 Precondition Failed` | `If-Match` / `If-None-Match` evaluation failed |
| `415 Unsupported Media Type` | Body Content-Type not accepted |
| `422 Unprocessable Entity` | Syntactically valid but semantically rejected (validation, business rule) |
| `429 Too Many Requests` | Rate limit exceeded; include `Retry-After` |

`422` vs `400`: use `400` for "I cannot parse this JSON". Use `422` for "I parsed it, but the customer doesn't exist" or "the dates overlap with an existing booking".

### Server Errors (5xx)

| Code | When |
|------|------|
| `500 Internal Server Error` | Unhandled exception; log details, return generic `problem+json` |
| `502 Bad Gateway` | Upstream service returned a non-recoverable error |
| `503 Service Unavailable` | Service is overloaded or in maintenance; include `Retry-After` |
| `504 Gateway Timeout` | Upstream service did not respond in time |

Never leak 5xx details to clients (Rule 12 J7).

## Headers

### Required Request Headers

| Header | Value | Notes |
|--------|-------|-------|
| `Authorization` | `Bearer <token>` | For authenticated calls |
| `Content-Type` | `application/json; charset=utf-8` | For bodies |
| `Accept` | `application/json` | What client can parse |
| `traceparent` | W3C trace context | For distributed tracing |
| `Idempotency-Key` | UUIDv4 or ULID | For POST / PATCH mutations |

### Standard Response Headers

| Header | Value |
|--------|-------|
| `Content-Type` | `application/json` for success, `application/problem+json` for errors |
| `Location` | URI of created resource (for `201`) |
| `ETag` | Entity tag for optimistic concurrency |
| `Last-Modified` | Resource modification timestamp |
| `Cache-Control` | Caching policy |
| `X-RateLimit-*`, `Retry-After` | See `api-design-principles.md` |

### Forbidden Response Headers

- `Server`, `X-Powered-By` — fingerprinting risk; strip them
- Custom headers with internal hostnames, version codes, or queue lengths

## Resource Naming

| Rule | Example |
|------|---------|
| Plural nouns for collections | `/orders`, not `/order` |
| Lower case, hyphens for multi-word | `/payment-methods`, not `/PaymentMethods` |
| Resource IDs in path | `/orders/{id}`, not `/orders?id={id}` |
| Sub-resources express containment | `/orders/{id}/items` |
| Filtering via query params | `/orders?status=pending&customer_id=cust_1` |
| Sorting via query params | `/orders?sort=-created_at` (prefix `-` for descending) |
| Sparse fieldsets | `/orders?fields=id,total,status` |

Path segments are case-sensitive per RFC 3986; pick lowercase and enforce it.

## Content Negotiation

```
Accept: application/json
Accept: application/vnd.example.v2+json     # versioned media type
Accept-Encoding: gzip, br
Accept-Language: en, pt-BR;q=0.9
```

Server picks the best match; respond `406 Not Acceptable` if no match. Default to JSON when `Accept` is `*/*`.

## Conditional Requests

For caching and optimistic concurrency:

```
# Read with cache validation
GET /orders/123
If-None-Match: "abc123"

# Update with concurrency check
PATCH /orders/123
If-Match: "abc123"
```

Server responds `304 Not Modified` (read) or `412 Precondition Failed` (write).

## CORS

| Header | Purpose |
|--------|---------|
| `Access-Control-Allow-Origin` | Specific origin; never `*` if credentials are involved |
| `Access-Control-Allow-Methods` | Methods supported |
| `Access-Control-Allow-Headers` | Headers the client may send |
| `Access-Control-Allow-Credentials` | `true` only when needed; pairs with specific origin |
| `Access-Control-Max-Age` | Preflight cache time |

See Rule 12 J8 — wildcard CORS with credentials is forbidden.

## Long-Running Operations

For operations that take > 1 second, prefer async patterns:

```
POST /imports
202 Accepted
Location: /imports/imp_456
{ "id": "imp_456", "status": "pending" }

GET /imports/imp_456
200 OK
{ "id": "imp_456", "status": "completed", "result_uri": "..." }
```

Avoid long-polling without server-side timeout caps. Avoid keeping HTTP connections open for minutes; use job/poll or webhooks.

## Bulk Operations

Two patterns:

1. **Array body** for atomic-ish bulk create:
   ```
   POST /orders/bulk
   { "orders": [ ... ] }
   ```
   Response includes per-item status. Whole request is one transaction or none — make it explicit.

2. **Job-based** for long bulk imports:
   ```
   POST /imports          # returns job id
   GET  /imports/{id}     # poll status
   ```

## API Documentation

- OpenAPI 3.1 specification is the source of truth; checked into the repo
- Spec drift detection runs in CI (`x-detect-spec-drift`)
- Each endpoint has: summary, description, request/response examples, error responses, authentication scopes
- Server URLs include environment templating (`{stage}.api.example.com`)

## Cross-References

- `api-design-principles.md` (protocol-agnostic principles)
- `api-deprecation-checklist.md` (sunset workflow)
- RFC 7231 — HTTP/1.1 Semantics
- RFC 7807 — Problem Details for HTTP APIs
- RFC 5988 — Web Linking (for HATEOAS-style pagination/relations)
- Rule 06 — `security-baseline.md`
- Rule 12 J7 (exception leakage), J8 (CORS wildcard)
