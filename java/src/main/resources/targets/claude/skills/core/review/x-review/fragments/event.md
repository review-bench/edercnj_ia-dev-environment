---
name: x-review-fragment-event
description: Event specialist review fragment — active when a messaging capability is configured.
fragment-slot: { slot: review-specialist, fragment-id: event, fragment-order: 70 }
requires-any: [messaging.kafka.standard, messaging.rabbitmq.standard]
requires-capabilities: []
---

### Event Specialist (`/x-review-events`)

| Attribute | Value |
|-----------|-------|
| Max Score | /28 |
| Condition | Event-driven or event interfaces present |
| Skill | `x-review-events` |

Reviews: event schema versioning (Avro/JSON Schema registry), idempotent consumer pattern (deduplication key), dead-letter topic configuration, outbox pattern for transactional messaging, consumer group isolation, partition key strategy (cardinality, ordering guarantees), message retention policy, and producer acknowledgment settings (`acks=all`).
