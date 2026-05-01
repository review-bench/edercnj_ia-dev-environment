---
name: contract-grpc
description: "buf breaking playbook for gRPC/proto3 contract breaking-change detection"
requires-capabilities: [quality.contract.buf]
---

# Knowledge Pack: Contract Testing — gRPC / proto3 (buf breaking)

## Tool Matrix

| Runtime | Tool | Min Version | Container Image |
|---------|------|-------------|----------------|
| any | buf | 1.30.0 | `bufbuild/buf:latest` |

## Installation

```bash
# Homebrew
brew install bufbuild/buf/buf

# Direct install
curl -sSL https://github.com/bufbuild/buf/releases/latest/download/buf-$(uname -s)-$(uname -m) -o /usr/local/bin/buf
chmod +x /usr/local/bin/buf

# Verify
buf --version
```

## Configuration File

`buf.yaml` at project root:

```yaml
version: v1
lint:
  use:
    - DEFAULT
breaking:
  use:
    - FILE
  except:
    - FIELD_SAME_DEFAULT
```

## Run Command

```bash
buf breaking \
  --against ".git#branch=$develop,subdir=." \
  --config buf.yaml
```

Or compare against a specific ref:

```bash
buf breaking \
  --against ".git#tag=${BASE_TAG}" \
  --error-format text \
  2>&1 | tee /tmp/buf-result.txt
```

## Breaking vs Non-Breaking Classification

| Change | Classification | Wire Impact |
|--------|---------------|-------------|
| Field removed | BREAKING | Old field tag number removed — deserializes as unknown |
| Field type changed | BREAKING | Wire format mismatch |
| Message removed | BREAKING | 404 for callers |
| RPC removed | BREAKING | gRPC method gone |
| Service removed | BREAKING | Entire service gone |
| Oneof variant removed | BREAKING | Callers using variant fail |
| Enum value removed | BREAKING | Known values rejected |
| Field renamed (tag preserved) | NON_BREAKING | Wire compat preserved (proto3) |
| New field added | NON_BREAKING | Unknown fields ignored |
| New RPC added | NON_BREAKING | Additive |
| Comment/option changed | NON_BREAKING | Metadata only |

## Output Parsing

```bash
buf breaking \
  --against ".git#branch=main" \
  --error-format json 2>/dev/null | jq '.'

# Count breaking violations
VIOLATIONS=$(buf breaking --against ".git#branch=main" 2>&1 | wc -l)
```

## Exit Code Mapping

| buf outcome | Skill exit code |
|-------------|----------------|
| No violations | 0 SUCCESS |
| Breaking violations (no CHANGELOG) | 1 CONTRACT_BREAKING_CHANGE |
| Breaking violations + CHANGELOG entry | 0 SUCCESS (WARN) |
| buf not on PATH | 2 OPERATIONAL_ERROR |
| Malformed proto file | 3 CONTRACT_ARTIFACT_INVALID |

## Version Pinning

```yaml
quality:
  contract:
    buf:
      min-version: "1.30.0"
```

## Notes

- `buf breaking --against` compares current workspace against git ref
- `FILE` rule set is recommended for production (stricter than `PACKAGE` or `WIRE`)
- Field numbers are the contract — renaming fields is safe in proto3 (wire compat preserved)
- For multi-module repos, run `buf breaking` from each module directory
- Container run: `docker run --rm -v "$PWD":/workspace bufbuild/buf:latest breaking --against ".git#branch=main"`
- `FIELD_SAME_DEFAULT` is typically excluded (proto3 default is zero value — changing explicit default is low risk)
