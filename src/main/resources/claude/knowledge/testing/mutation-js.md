---
name: mutation-js
description: "Stryker mutation testing playbook for JavaScript/TypeScript projects"
requires-capabilities: [quality.mutation.stryker]
---

# Knowledge Pack: Mutation Testing — JavaScript / TypeScript (Stryker)

## Tool Matrix

| Runtime | Tool | Min Version | Container Image |
|---------|------|-------------|----------------|
| Node.js | Stryker | 8.0 | `node:20-slim` |

## Configuration File

`stryker.config.mjs` at project root:

```js
export default {
  packageManager: "npm",
  reporters: ["html", "clear-text", "json"],
  testRunner: "jest",  // or "mocha", "vitest"
  coverageAnalysis: "perTest",
  mutate: [
    "src/domain/**/*.ts",
    "src/application/**/*.ts",
    "!src/**/__tests__/**"
  ],
  thresholds: {
    high: 80,
    low: 60,
    break: 80
  },
  timeoutMS: 20 * 60 * 1000
};
```

## Run Command

```bash
timeout $((runtime_cap_min * 60)) npx stryker run \
  --mutate "src/domain/**/*.ts,src/application/**/*.ts" \
  --timeoutMS $((runtime_cap_min * 60 * 1000))
```

## Result Parsing

Report location: `reports/mutation/mutation.json`

```bash
SCORE=$(jq '.metrics.mutationScore' reports/mutation/mutation.json)
KILLED=$(jq '.metrics.killed' reports/mutation/mutation.json)
TOTAL=$(jq '(.metrics.killed + .metrics.survived + .metrics.timeout)' reports/mutation/mutation.json)
```

## Surviving Mutant Report

From `mutation.json`, find mutants with `status: "Survived"`:
- `fileName` → file path (sanitize to project-relative)
- `mutatorName` → mutation type (ArithmeticOperator, ConditionalExpression, etc.)
- `location.start.line` → line number
- `replacement` → what the mutant changed to

## Exit Code Mapping

| Stryker outcome | Skill exit code |
|-----------------|----------------|
| `mutationScore ≥ threshold` | 0 SUCCESS |
| `mutationScore < threshold` | 1 MUTATION_SCORE_BELOW_THRESHOLD |
| Timeout elapsed | 2 MUTATION_RUNTIME_CAP_EXCEEDED |
| `npx stryker` not found | 3 TOOL_NOT_FOUND |

## Default Exclusions

Patterns passed to `--mutate` exclusion list:
- `!src/**/__tests__/**` — test files
- `!src/**/*.generated.ts` — generated code
- `!src/**/infrastructure/adapter/**` — adapters

## Version Pinning

```yaml
quality:
  mutation:
    tool-versions:
      stryker: "8.2.6"
```
