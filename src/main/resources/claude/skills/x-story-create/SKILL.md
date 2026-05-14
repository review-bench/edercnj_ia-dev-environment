---
name: x-story-create
model: sonnet
description: "Creates focused Story artifacts from a Feature markdown with epic linkage and RNFs."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "--from-feature <feature-file|feature-id> --epic-id <EPIC-NNNN|NNNN> [--capability-file <path>] [--product-file <path>] [--output-dir <path>] [--dry-run]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English only for code identifiers and established technical terms.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Avoid repeating Product-First rationale already captured in parent artifacts.

# Skill: x-story-create

## Purpose

Generate 1-N Story markdown artifacts from an existing Feature artifact, linked to a target epic via `--epic-id`. This focused Product-First wrapper materializes only the Feature → Story step, preserving `sourceFeature`, carrying inherited RNFs, and enriching each story with the refinement-critical context required by `x-refine-story`.

> **EPIC-0077 semantic reintroduction:** `x-story-create` was hard-cut in EPIC-0065 when generic story generation moved behind `x-internal-create-story`. It is reintroduced here with a narrower responsibility: **Feature-derived story generation only**. It is not a legacy alias.

## When to Use

- You already have a Feature artifact and need the derived Stories only
- You want to bind generated stories to an existing epic via `--epic-id`
- You need inherited RNFs rendered explicitly into the story markdown before implementation starts

## Prerequisites

Read before starting:

- `.claude/templates/_TEMPLATE-STORY.md`
- `.claude/skills/planning-standards-kp/SKILL.md` — mandatory RA9 source of truth
- `.claude/skills/x-internal-create-story/SKILL.md`

If the feature source cannot be resolved, stop with a validation error.

## Workflow

1. Resolve `--from-feature`:
   - if it is a file path, use it directly;
   - otherwise, resolve it by feature id under `ai/**`.
2. Normalize `--epic-id` (`EPIC-NNNN` or `NNNN` → `NNNN`).
3. Resolve optional `--capability-file` and `--product-file` for RNF inheritance.
4. Decompose the feature into 1-N stories using its use cases as story seeds.
5. Create `ai/epics/epic-<ID>-<slug>/story-<ID>-<NNNN>.md`.
6. Populate:
    - `Epic ID`
    - `Source Feature`
    - `Source Feature Link`
    - inherited RNFs in the story body without replacing mandatory template sections
    - `## 1` through `## 9`
7. Before writing each story, validate the refinement-critical sections:
   - `## 1` and `## 2` contain a concrete persona and scenario
   - `## 3` contains measurable value and at least 1 metric with unit and target
   - `## 4` contains Gherkin ACs covering happy-path, error/boundary, performance/SLA, and security
   - `## 5` contains typed contracts
   - `## 8` contains at least 1 alternative/decision rationale
8. Derive evidence for those sections in this order:
   - primary actor, flow, input/output, and success signal from the Feature use case
   - cross-cutting constraints and measurable value from the linked Epic
   - complementary context from `--capability-file` and `--product-file`
   - inherited RNFs as constraints, never as a replacement for persona, AC, contracts, or rationale
9. If the source feature and linked epic do not provide enough evidence to fill those sections concretely, stop with a validation error instead of generating a story that will fail refinement.

## Parameters

| Parameter | Required | Description |
| :--- | :--- | :--- |
| `--from-feature <feature-file\|feature-id>` | Yes | Feature markdown source |
| `--epic-id <EPIC-NNNN\|NNNN>` | Yes | Epic identifier to link generated stories |
| `--capability-file <path>` | No | Capability markdown used to enrich inherited RNFs |
| `--product-file <path>` | No | Product markdown used to extend inherited RNFs |
| `--output-dir <path>` | No | Base output dir (default `ai/epics`) |
| `--dry-run` | No | Validate inputs without writing files |

## Integration Notes

- `x-create-feature` remains the full orchestrator for Feature → Epic + Stories + Map.
- `x-story-create` is the focused public wrapper for Feature → Story only.
- `x-internal-create-story` remains the reusable internal implementation surface for orchestrators and focused wrappers.
