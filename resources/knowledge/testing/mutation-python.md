---
name: mutation-python
description: "mutmut mutation testing playbook for Python projects"
requires-capabilities: [quality.mutation.mutmut]
---

# Knowledge Pack: Mutation Testing — Python (mutmut)

## Tool Matrix

| Runtime | Tool | Min Version | Container Image |
|---------|------|-------------|----------------|
| Python 3.12 | mutmut | 2.4 | `python:3.12-slim` |

## Installation

```bash
pip install mutmut==2.4.*
```

## Configuration File

`setup.cfg` or `pyproject.toml`:

```toml
[tool.mutmut]
paths_to_mutate = "domain/,application/"
tests_dir = "tests/"
runner = "python -m pytest -x --timeout=30"
```

## Run Command

```bash
timeout $((runtime_cap_min * 60)) mutmut run \
  --paths-to-mutate domain/,application/
```

## Result Parsing

```bash
# Get summary
mutmut results

# Parse stdout format: "Survived: N / Total: T"
OUTPUT=$(mutmut results 2>&1)
SURVIVED=$(echo "$OUTPUT" | grep -oP 'Survived: \K\d+')
TOTAL=$(echo "$OUTPUT" | grep -oP 'Total: \K\d+')
KILLED=$(( TOTAL - SURVIVED ))
SCORE=$(echo "scale=2; $KILLED * 100 / $TOTAL" | bc)
```

## Surviving Mutant Report

```bash
# List surviving mutant IDs
mutmut results --status survived

# Show individual mutant
mutmut show <mutant_id>
```

Output format:
```
--- domain/model.py
+++ domain/model.py mutant
@@ -42,7 +42,7 @@
-    if value > 0:
+    if value >= 0:
```

## Exit Code Mapping

| mutmut outcome | Skill exit code |
|----------------|----------------|
| All killed | 0 SUCCESS |
| Score < threshold | 1 MUTATION_SCORE_BELOW_THRESHOLD |
| Timeout | 2 MUTATION_RUNTIME_CAP_EXCEEDED |
| `mutmut` not on PATH | 3 TOOL_NOT_FOUND |

## Default Exclusions

Controlled via `paths_to_mutate` — only mutate `domain/` and `application/`.
Exclude generated files:

```toml
[tool.mutmut]
dict_synonyms = ""
# Explicitly exclude test and generated paths via runner args
```

## Version Pinning

```yaml
quality:
  mutation:
    tool-versions:
      mutmut: "2.4.3"
```
