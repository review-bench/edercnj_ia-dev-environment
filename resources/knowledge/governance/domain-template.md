---
name: domain-template
description: Template structure for domain-specific rules — defines the 15-section RA9 structure for custom domain rules
requires-capabilities: []
---
# Rule — {DOMAIN_NAME} Domain

## Domain Overview

{DOMAIN_OVERVIEW}

## System Role

- **Receives:** {WHAT_IT_RECEIVES}
- **Processes:** {WHAT_IT_PROCESSES}
- **Returns:** {WHAT_IT_RETURNS}
- **Persists:** {WHAT_IT_PERSISTS}

## Domain Model

### Core Entities

{ENTITIES_TABLE}

### Value Objects

{VALUE_OBJECTS}

### Aggregates and Boundaries

{AGGREGATES}

## Business Rules

### {RULE_ID_1}: {RULE_NAME_1}

{RULE_1_DESCRIPTION}

### {RULE_ID_2}: {RULE_NAME_2}

{RULE_2_DESCRIPTION}

## Domain States and Transitions

{STATE_MACHINES}

## Communication Protocols

{PROTOCOLS}

## Sensitive Data

{SENSITIVE_DATA_TABLE}

### Data Handling Rules

{DATA_HANDLING_RULES}

## Domain-Specific Test Scenarios

### Unit Test Scenarios

{UNIT_TEST_SCENARIOS}

### Integration Test Scenarios

{INTEGRATION_TEST_SCENARIOS}

## Domain Anti-Patterns

{DOMAIN_ANTI_PATTERNS}

## Glossary

{GLOSSARY}
