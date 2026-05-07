---
name: coding-standards-rule
description: Full coding standards reference — hard limits, naming, SOLID, error handling, TDD, and language conventions
requires-capabilities: []
---
# Coding Standards — Full Reference

> **Always-loaded summary:** `.claude/rules/00-essentials.md §2 Hard Limits`
> **See also:** `.claude/knowledge/coding-standards.md` for language-specific idioms

## Hard Limits

| Constraint | Limit |
|-----------|-------|
| Method/function length | ≤ 25 lines |
| Class/module length | ≤ 250 lines |
| Parameters per function | ≤ 4 (use parameter object if more) |
| Line width | ≤ 120 characters |
| Train wreck depth | ≤ 2 levels across object boundaries |

## Naming

- Intent-revealing: `elapsedTimeInMs` not `d`
- Verbs for methods: `processTransaction()`, `extractAmount()`
- Nouns for types: `TransactionHandler`, `DecisionEngine`
- Extract ALL literals: named constants for numbers, enums for categorical values (colors, states, types)

## SOLID (one-liners)

- **SRP**: One class = one reason to change
- **OCP**: New behavior = new class, never modify existing handlers
- **LSP**: Every implementation must fulfill its interface contract
- **ISP**: Small focused interfaces; no empty method implementations
- **DIP**: Depend on abstractions (ports), not concrete implementations

## Error Handling

- NEVER return null — use Optional, empty collection, or Result type
- NEVER pass null as argument
- Exceptions MUST carry context (values that caused the error)
- Catch at the right level, not where convenient

## Forbidden

- Boolean flags as function parameters
- Comments that repeat what code says
- Mutable global state
- God classes / train wrecks (chained calls across objects)
- Wildcard imports
- `sleep()` for synchronization (use condition-based polling)
- `System.out` / `System.err` / `print()` in production code (use structured logging or propagate via return types)
- String concatenation with `+` in messages or content building (use language-idiomatic formatting)
- Fully qualified class names when an import suffices
- Dead code: unreachable branches, unused methods, test-only code in production source
- Mutable fields in immutable data carriers (records, data classes, frozen dataclasses)
- Duplicated utility methods across files (extract to shared helper immediately)

## Code Hygiene

- **DRY**: If a utility method exists in > 1 file, extract to a shared helper immediately
- **Single source of truth**: One canonical implementation per algorithm; local variants are forbidden

## TDD Practices

- **Red-Green-Refactor** is mandatory for all production code
  1. **Red**: Write a failing test that defines the expected behavior
  2. **Green**: Write the minimum code to make the test pass
  3. **Refactor**: Improve design without changing behavior
- Refactoring criteria: extract method when > 25 lines, eliminate duplication, improve naming
- Refactoring NEVER adds behavior — if behavior changes, write a new failing test first
- Test-first commits: test must appear in git history before or in the same commit as its implementation

> **Full TDD reference:** Read `.claude/knowledge/testing.md` for Double-Loop TDD, Transformation Priority Premise, and advanced TDD patterns.

## Language-Specific Conventions

> Read `.claude/knowledge/coding-standards.md` for language-specific idioms, constructor injection patterns, mapper conventions, and formatting rules.

## Cross-File Consistency

- Error handling pattern MUST be uniform across classes of the same role (e.g., all assemblers, all controllers)
- Constructor patterns, return types, and internal type definitions MUST follow the same shape within a module
- Inconsistency across files of the same role is a MEDIUM-severity violation
