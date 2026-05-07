---
name: x-execute-contract-tests
description: "Stack-aware contract breaking-change detection (openapi-diff, buf, schema registry)."
visibility: public
model: sonnet
allowed-tools: Read, Write, Edit, Bash, Grep, Glob
argument-hint: "[--story-id STORY-XXXX-YYYY] [--base-ref HEAD~1]"
context-budget: medium
requires-any:
  - quality.contract.openapi-diff
  - quality.contract.pact
  - quality.contract.buf
  - quality.contract.scc
requires-capabilities: []
---

## Global Output Policy

- **Language**: English ONLY.
- **Tone**: Technical, Direct, and Concise.
- **Efficiency**: Remove all conversational fillers and greetings to save tokens.

# Skill: Contract Breaking-Change Detection (`x-execute-contract-tests`)

## Purpose

Detect breaking changes in API contracts before they reach production. Stack-aware dispatch:

| Contract artifact | Tool | Activation |
|-------------------|------|-----------|
| `openapi.yaml` / `openapi.json` | openapi-diff | capability `quality.contract.openapi-diff` |
| `**/*.proto` | buf breaking | capability `quality.contract.buf` |
| Spring Boot + SCC stubs | Spring Cloud Contract | capability `quality.contract.scc` |
| `**/*.avsc` / schema registry | Schema registry compat | capability `quality.contract.scc` + `schema-registry-url` set |
| Consumer-driven contracts | Pact | capability `quality.contract.pact` (opt-in only) |

## Triggers

- `/x-execute-contract-tests` — auto-detect contracts, run all applicable tools
- `/x-execute-contract-tests --story-id story-XXXX-YYYY` — scope report to story
- `/x-execute-contract-tests --base-ref HEAD~1` — compare against specific base ref

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `--story-id` | String | — | Story ID for report path |
| `--base-ref` | String | `HEAD~1` | Base ref for comparison |

## Exit Codes

| Exit | Code | Condition |
|------|------|-----------|
| 0 | `SUCCESS` | No breaking changes, or breaking change documented in CHANGELOG |
| 1 | `CONTRACT_BREAKING_CHANGE` | Breaking change detected without CHANGELOG documentation |
| 2 | `OPERATIONAL_ERROR` | Tool not found, invalid config, file read failure |
| 3 | `CONTRACT_ARTIFACT_INVALID` | Contract file exists but is malformed |

## Step 1 — Detect Contract Artifacts

Scan project for contract artifacts:

```bash
# OpenAPI
find . -name "openapi.yaml" -o -name "openapi.json" | grep -v target/ | head -5

# gRPC proto
find . -name "*.proto" | grep -v target/ | head -10

# Avro schemas
find . -name "*.avsc" | grep -v target/ | head -5

# Spring Cloud Contract stubs
find . -path "*/contractTest/*.groovy" -o -path "*/contractTest/*.yml" | grep -v target/
```

When NO contract artifact found AND `quality.contract.enabled=true`:
- Emit `WARN: no contract artifact found — skipping`
- Exit 0 (non-blocking)

## Step 2 — Run Stack-Appropriate Tool

### REST (openapi-diff)

```bash
# Validate tool available
openapi-diff --version 2>&1 || { echo "OPERATIONAL_ERROR: openapi-diff not on PATH"; exit 2; }

# Compare base vs current
openapi-diff \
  --fail-on-incompatible \
  --output-markdown \
  <(git show ${BASE_REF}:openapi.yaml 2>/dev/null || echo "{}") \
  openapi.yaml \
  > /tmp/openapi-diff-result.md 2>&1
OPENAPI_EXIT=$?
```

Classify changes:
- `ADDED` field/path/method → NON_BREAKING
- `REMOVED` field/path/method → BREAKING
- `TYPE_CHANGED` → BREAKING
- `ENUM_VALUE_REMOVED` → BREAKING
- `NULLABLE_CHANGED` (required→optional = NON_BREAKING; optional→required = BREAKING)
- `DEFAULT_CHANGED` → NON_BREAKING (advisory warn)

### gRPC/proto (buf breaking)

```bash
buf --version 2>&1 || { echo "OPERATIONAL_ERROR: buf not on PATH"; exit 2; }

buf breaking \
  --against ".git#branch=${BASE_REF},subdir=." \
  --config buf.yaml \
  2>&1 | tee /tmp/buf-result.txt
BUF_EXIT=$?
```

Classify:
- Field removed → BREAKING
- Field type changed → BREAKING
- Message removed → BREAKING
- Field renamed (proto3 wire-compat preserved) → NON_BREAKING (warn)

### Spring Cloud Contract (SCC)

```bash
# Skip when scc.stub-repository-url absent or unreachable
if [ -z "${SCC_STUB_REPO_URL}" ]; then
  echo "WARN: SCC stub repository not configured — skipping Spring Cloud Contract verification"
  SCC_EXIT=0
else
  mvn spring-cloud-contract:verify --no-transfer-progress -q 2>&1
  SCC_EXIT=$?
fi
```

### Pact (opt-in)

Runs only when `quality.contract.pact=true`:

```bash
if [ "${PACT_ENABLED}" = "true" ]; then
  mvn pact:verify --no-transfer-progress -q 2>&1
  PACT_EXIT=$?
fi
```

### Schema Registry (events)

```bash
if [ -n "${SCHEMA_REGISTRY_URL}" ]; then
  curl -u "${SCHEMA_REGISTRY_USER}:${SCHEMA_REGISTRY_PASSWORD}" \
    "${SCHEMA_REGISTRY_URL}/compatibility/subjects/${SUBJECT}/versions/latest" \
    -d @schema.avsc --fail-with-body 2>&1
  SCHEMA_EXIT=$?
  if [ $SCHEMA_EXIT -ne 0 ]; then
    echo "WARN: schema registry auth failed — skipping event contract check"
    SCHEMA_EXIT=0
  fi
fi
```

## Step 3 — CHANGELOG Integration

When breaking change detected, check if PR documents it:

```bash
# CHANGELOG detection regex — matches ## Breaking, ## Breaking Change(s), ## BREAKING
git diff ${BASE_REF} -- CHANGELOG.md 2>/dev/null \
  | grep -E '^\+##\s+(Breaking(\s+Changes?)?|BREAKING)\s*$' \
  | head -1
```

Breaking change + CHANGELOG entry → exit 0 with `WARN: breaking change detected — documented migration path accepted`
Breaking change + NO CHANGELOG entry → exit 1 `CONTRACT_BREAKING_CHANGE`

## Step 4 — Produce Report

Output: `ai/epics/epic-XXXX/reports/contract-report-${STORY_ID}.md`

```markdown
## Summary
- **Verdict:** PASS | FAIL | WARN
- **Tool(s):** openapi-diff 2.x | buf 1.30+ | Spring Cloud Contract | Pact
- **Execution time:** Xs

## Changes Detected

| Field / Path | Change Type | Severity | Location |
|--------------|-------------|----------|----------|
...

## CHANGELOG Integration
- **Breaking entry found:** yes / no
- **Accepted:** yes / no (reason)

## Recommendations
...
```

## Recovery

```
# When tool is not on PATH
OPERATIONAL_ERROR: openapi-diff not on PATH
→ Install: brew install openapitools/openapi-generator/openapi-diff (v2.x)
→ Or use Docker: docker run openapitools/openapi-diff:latest

# When SCC stub repo missing
WARN: SCC stub repository not configured — skipping Spring Cloud Contract verification
→ Set quality.contract.scc.stub-repository-url in project YAML to enable

# When schema registry auth fails
WARN: schema registry auth failed — skipping event contract check
→ Set SCHEMA_REGISTRY_USER and SCHEMA_REGISTRY_PASSWORD env vars
```
