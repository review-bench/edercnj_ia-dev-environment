---
name: mutation-go
description: "go-mutesting mutation testing playbook for Go projects"
requires-capabilities: [quality.mutation.go-mutesting]
---

# Knowledge Pack: Mutation Testing — Go (go-mutesting)

## Tool Matrix

| Runtime | Tool | Min Version | Container Image |
|---------|------|-------------|----------------|
| Go 1.22 | go-mutesting | 0.3 | `golang:1.22-alpine` |

## Installation

```bash
go install github.com/zimmski/go-mutesting/cmd/go-mutesting@v0.3.0
```

## Run Command

```bash
timeout $((runtime_cap_min * 60)) go-mutesting ./domain/... ./application/...
```

## Result Parsing

go-mutesting prints to stdout:

```
ok   github.com/example/project/domain/... [mutation testing summary]
PASS "... file.go:42:5" -- 1 mutations have been done
FAIL "... file.go:88:10" -- A slipped mutation was found
...
The mutation score is 0.821 (41 of 50 mutations have been killed)
```

```bash
# Parse: "X of Y mutations have been killed"
OUTPUT=$(go-mutesting ./domain/... ./application/... 2>&1)
KILLED=$(echo "$OUTPUT" | grep -oP '\K\d+(?= of \d+ mutations have been killed)')
TOTAL=$(echo "$OUTPUT" | grep -oP '\d+(?= mutations have been killed)' | tail -1)
SCORE=$(echo "scale=2; $KILLED * 100 / $TOTAL" | bc)
```

## Surviving Mutant Report

Lines starting with `FAIL` contain surviving mutants:

```
FAIL "... file.go:88:10" -- A slipped mutation was found
```

Format: `FAIL "<file>:<line>:<col>" -- <description>`

## Exit Code Mapping

| go-mutesting outcome | Skill exit code |
|----------------------|----------------|
| Score ≥ threshold | 0 SUCCESS |
| Score < threshold | 1 MUTATION_SCORE_BELOW_THRESHOLD |
| Timeout | 2 MUTATION_RUNTIME_CAP_EXCEEDED |
| `go-mutesting` not on PATH | 3 TOOL_NOT_FOUND |

## Default Exclusions

Scope `./domain/... ./application/...` already excludes adapters and tests.
Additional exclusion via build tags:

```go
//go:build !mutationtesting
```

## Version Pinning

```yaml
quality:
  mutation:
    tool-versions:
      go-mutesting: "0.3.0"
```

## Notes

- go-mutesting runs tests inline — ensure `go test ./...` passes before running
- Large codebases: add `-timeout` flag to `go test` via `--test.timeout`
- Container run: `docker run --rm -v "$PWD":/workspace golang:1.22-alpine sh -c "cd /workspace && go-mutesting ./domain/... ./application/..."`
