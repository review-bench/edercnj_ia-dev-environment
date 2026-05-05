---
name: x-epic-create
model: sonnet
description: "Create a focused Epic artifact from an existing Feature markdown, preserving sourceFeature lineage and inherited RNFs without generating stories or implementation map."
user-invocable: true
allowed-tools: Read, Write, Edit, Bash, Grep, Glob, Skill
argument-hint: "--from-feature <feature-file|feature-id> [--capability-file <path>] [--product-file <path>] [--epic-id <NNNN>] [--output-dir <path>] [--dry-run]"
requires-capabilities: []
---

## Output Policy

- **Language**: Portuguese (pt-BR) for all content. English for code identifiers and established technical terms.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove fillers and avoid re-explaining repository conventions.

# Skill: x-epic-create

## Purpose

Generate a single Epic markdown artifact from an existing Feature artifact. This focused skill is the Product-First complement to `x-create-feature`: it materializes only the Feature → Epic step, preserving `Source Feature`, inheriting RNFs from the Product / Capability / Feature chain, and intentionally omitting sections `2`, `4`, and `8` of the generic epic template because that context is already owned upstream in the chain.

> **EPIC-0077 semantic reintroduction:** `x-epic-create` was hard-cut in EPIC-0065 when the generic public epic generator moved behind `x-internal-create-epic`. It is reintroduced here with a narrower responsibility: **Feature-derived epic generation only**. It is not a legacy alias.

## When to Use

- You already have a Feature artifact and need the derived Epic only
- You want Product-First traceability without generating stories/map in the same step
- You need a lightweight wrapper around `x-internal-create-epic` for Feature → Epic

## Prerequisites

Read before starting:

- `.claude/templates/_TEMPLATE-EPIC.md`
- `.claude/skills/planning-standards-kp/SKILL.md` — mandatory RA9 source of truth
- `.claude/skills/x-internal-create-epic/SKILL.md`

If the feature source cannot be resolved, stop with a validation error.

## Workflow

1. Resolve `--from-feature`:
   - if it is a file path, use it directly;
   - otherwise, resolve it by feature id under `ai/**`.
2. Resolve optional `--capability-file` and `--product-file` to enrich RNF inheritance.
3. Validate epic id (`4 digits`, default `0001`).
4. Create `ai/epics/epic-<ID>-<slug>/epic-<ID>.md`.
5. Populate:
   - `Source Feature`
   - `Source Feature Link`
   - `## 0.6 Inherited RNFs`
   - `## 1`, `## 3`, `## 5`, `## 6`, `## 7`, `## 9`
6. **Do not render** sections `## 2`, `## 4`, `## 8` for Feature-derived epics.

## Parameters

| Parameter | Required | Description |
| :--- | :--- | :--- |
| `--from-feature <feature-file\|feature-id>` | Yes | Feature markdown source |
| `--capability-file <path>` | No | Capability markdown used to enrich inherited RNFs |
| `--product-file <path>` | No | Product markdown used to extend inherited RNFs |
| `--epic-id <NNNN>` | No | Epic identifier (default `0001`) |
| `--output-dir <path>` | No | Base output dir (default `ai/epics`) |
| `--dry-run` | No | Validate inputs without writing files |

## Integration Notes

- `x-create-feature` remains the full orchestrator for Feature → Epic + Stories + Map.
- `x-epic-create` is the focused public wrapper for Feature → Epic only.
- The generation logic may delegate to `x-internal-create-epic` when orchestrated by another planning skill.
