---
name: claude-md-fragment-messaging
description: Messaging-specific CLAUDE.md section — active when a messaging capability is configured.
fragment-slot: { slot: domain-specific, fragment-id: messaging, fragment-order: 20 }
requires-any: [messaging.kafka.standard, messaging.rabbitmq.standard]
requires-capabilities: []
---

## Messaging Patterns

This project uses an event-driven messaging layer.

- **Outbox pattern:** Publish events via an `OutboxEvent` table committed in the same transaction as the domain change; a scheduler polls and dispatches. Never publish events directly in service methods.
- **Idempotent consumers:** Every consumer must be idempotent using a deduplication key stored in a processed-events table; replay is safe and expected.
- **Dead-letter topics:** Every consumer topic must have a corresponding DLT (`{topic}.DLT`); failed messages are routed there after `maxAttempts`.
- **Schema versioning:** Use a schema registry (Confluent or AWS Glue) for Avro schemas; always evolve backward-compatible (add optional fields only).
- **Consumer groups:** One consumer group per logical consumer; never share a consumer group across different business contexts.
