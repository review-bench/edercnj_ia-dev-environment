# Event-Driven Conventions

Schema design, naming, ordering, idempotency, and operational patterns for asynchronous messaging — Kafka, RabbitMQ, SQS, Pub/Sub.

## Event Types

| Type | Purpose | Example |
|------|---------|---------|
| **Domain event** | Past-tense fact about something that happened | `OrderPlaced`, `PaymentAuthorized` |
| **Integration event** | Cross-service fact for other bounded contexts to consume | `order.placed.v1` |
| **Command** | Request to do something — has a single intended handler | `ReserveInventory`, `ChargeCard` |
| **Query** (rare) | Request for data via messaging — usually a sign you want RPC | — |

Domain events are internal to a bounded context. Integration events are the contract with other services and must be versioned.

## Naming

| Element | Convention | Example |
|---------|------------|---------|
| Event name | Past tense, subject-verb | `OrderPlaced`, `InventoryReserved` |
| Command name | Imperative, verb-subject | `PlaceOrder`, `ReserveInventory` |
| Topic / Queue name | `{domain}.{event}.v{N}` | `order.placed.v1`, `payment.authorized.v2` |
| Message ID | UUIDv4 or ULID | for dedup |
| Correlation ID | UUIDv4 | links events caused by one user action |
| Causation ID | UUIDv4 | the immediate parent event |

Topic per event type; do not multiplex unrelated events on one topic.

## Schema

### Required Envelope Fields

Every message carries an envelope plus the payload:

```json
{
  "specversion": "1.0",
  "type": "com.example.order.placed",
  "source": "/services/order-service",
  "id": "0193e4d2-...-...",
  "time": "2026-05-11T14:32:01.234Z",
  "datacontenttype": "application/json",
  "subject": "ord_123",
  "traceparent": "00-0af7651916cd43dd...01",

  "correlation_id": "0193e4d2-...-...",
  "causation_id": "0193e4d1-...-...",
  "schema_version": "1.2.0",

  "data": {
    "order_id": "ord_123",
    "customer_id": "cust_456",
    "total_cents": 15000,
    "currency": "USD",
    "items": [...]
  }
}
```

Following CloudEvents 1.0 spec covers the envelope. `data` is the domain payload.

### Schema Format

| Format | When |
|--------|------|
| **JSON Schema** | JSON payloads, dynamic ecosystems |
| **Avro** | Kafka with Schema Registry; binary efficiency |
| **Protobuf** | Strong typing, cross-language generation |

Schemas live in a schema registry (Confluent, Apicurio, AWS Glue). Producers and consumers fetch by topic + version.

### Forward / Backward Compatibility

- **Adding optional fields**: backward compatible (old consumers ignore them)
- **Removing required fields**: breaking — new version required
- **Renaming fields**: breaking
- **Changing semantics of a field**: breaking — add new field, deprecate old
- **Tightening constraints** (was nullable, now required): breaking
- **Loosening constraints** (was required, now optional): backward compatible

CI gate: schema registry rejects breaking changes (`x-execute-contract-tests`).

## Versioning

| Strategy | Pattern |
|----------|---------|
| Topic suffix | `order.placed.v1` → `order.placed.v2` |
| Header field | `schema_version: 1.2.0`, all on `order.placed` topic |

Topic suffix is more operationally clear (consumers explicitly subscribe to versions). Header versioning works when changes are reliably backward compatible.

Run two versions in parallel during migration; producers double-publish; consumers switch when ready; old topic retires after monitored quiet period.

## Idempotency

Consumers MUST be idempotent. Networks redeliver; brokers replay; restarts re-consume from offsets.

### Patterns

1. **Idempotent operation** — the operation itself is naturally idempotent (`UPSERT` on a unique key).
2. **Dedup table** — store `(message_id, processed_at)`; reject duplicates.
3. **Optimistic version** — write only if state matches expected version; conflicts become no-ops.

```java
@KafkaListener(topics = "order.placed.v1")
void handle(OrderPlacedEvent ev, Acknowledgment ack) {
    if (deduplicationStore.alreadyProcessed(ev.getEnvelope().getId())) {
        ack.acknowledge();
        return;
    }
    inventoryService.reserve(ev.getOrderId(), ev.getItems());
    deduplicationStore.markProcessed(ev.getEnvelope().getId());
    ack.acknowledge();
}
```

## Ordering

Different brokers give different guarantees. Know yours.

| Broker | Ordering guarantee |
|--------|--------------------|
| Kafka | Per partition, FIFO |
| RabbitMQ | Per queue, FIFO (without competing consumers) |
| SQS Standard | None |
| SQS FIFO | Per message group |
| Pub/Sub | None (Pub/Sub Lite: per partition) |

For ordered processing of related events (e.g., all events for `order_id=ord_123`):
- Partition by an ordering key (`order_id`) so they hit the same partition / queue group
- Consumer for that partition processes serially

Cross-entity ordering (`OrderPlaced` then `PaymentAuthorized`) requires explicit causation tracking, not partition order.

## Outbox / Inbox Pattern

To publish events reliably alongside a database transaction:

```
BEGIN;
  INSERT INTO orders (...)         -- business state
  INSERT INTO outbox (id, topic, payload, created_at)
COMMIT;
```

A separate worker reads `outbox` and publishes to the broker. Marks rows as sent. This avoids the dual-write problem (DB committed, broker publish failed → ghost orders OR DB rolled back, event already published → phantom events).

Inbox is the consumer side: store incoming events before processing to enable replay and idempotency.

See `knowledge/patterns-outbox.md` for full implementation details.

## Dead-Letter Queues (DLQ)

Every consumer has a DLQ. Messages move to DLQ after N retry failures.

| Setting | Default |
|---------|---------|
| Max retry attempts | 3 |
| Backoff | Exponential — 1s, 5s, 30s |
| DLQ topic | `{original}.dlq` |
| DLQ retention | Long (7+ days) — humans investigate |
| Alert on DLQ | Yes — DLQ rate > 0 pages |

DLQ messages carry the original payload PLUS:
- Last failure reason
- Retry count
- First-seen and last-attempted timestamps
- Consumer instance ID

Replay tooling exists: operators read DLQ, fix the issue, re-publish to original topic. Build this; don't improvise it at 3 AM.

## Poison Messages

When a message will never succeed (bad schema, missing reference, business rule permanently rejecting it):
- Skip after N retries → DLQ
- Do not block downstream messages (head-of-line blocking)
- Alert on DLQ ingestion rate

For Kafka: a poison message that crashes the consumer halts the partition. Wrap deserialization to send malformed payloads directly to DLQ.

## Backpressure

| Mechanism | Description |
|-----------|-------------|
| Consumer lag | Monitor and alert; auto-scale consumers |
| Bounded queues | Reject incoming when over watermark |
| Rate limits | Producers throttle on broker quota signals |
| Circuit breaker | Stop consuming temporarily when downstream is down |

Unbounded buffers eat memory and crash. Never `consumer.poll()` into an unbounded executor.

## At-Least-Once vs Exactly-Once

| Guarantee | When |
|-----------|------|
| **At-least-once** | Default; pair with idempotent consumers |
| **At-most-once** | Acceptable for fire-and-forget telemetry; not for financial events |
| **Exactly-once** | Kafka's transactional producer + idempotent consumer; complex, choose deliberately |

Most systems should be at-least-once + idempotent. "Exactly-once" is usually marketing for "at-least-once with cooperation".

## Schema Registry as a Gate

CI step (`x-execute-contract-tests`):
- Validates new schemas against registered baseline
- Fails on breaking changes unless explicitly tagged
- Publishes accepted schemas to registry on merge

Without registry: skew between producer and consumer expectations causes silent data loss.

## Observability

| Metric | Pillar |
|--------|--------|
| `messaging_consumer_lag` | Gauge per consumer group + partition |
| `messaging_consumer_processed_total` | Counter |
| `messaging_consumer_failures_total` | Counter labeled by error type |
| `messaging_dlq_received_total` | Counter — should be near zero |
| `messaging_processing_duration_seconds` | Histogram |

Logs: every consumed event logs `event_id`, `event_type`, `correlation_id`, `partition`, `offset`, `result`.

Traces: span starts from consumer poll; restored from `traceparent` in envelope.

## Cross-References

- `api-design-principles.md` (sync API counterpart)
- `grpc-conventions.md`
- `knowledge/patterns-outbox.md`
- `knowledge/testing/contract-events.md`
- CloudEvents 1.0 specification
- Confluent / Apicurio schema registry documentation
