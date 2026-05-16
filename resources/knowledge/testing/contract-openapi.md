---
name: contract-openapi
description: "openapi-diff playbook for REST API contract breaking-change detection"
requires-capabilities: [quality.contract.openapi-diff]
---

# Knowledge Pack: Contract Testing — REST / OpenAPI (openapi-diff)

## Tool Matrix

| Runtime | Tool | Min Version | Container Image |
|---------|------|-------------|----------------|
| JVM / any | openapi-diff | 2.0.0 | `openapitools/openapi-diff:latest` |

## Installation

```bash
# Homebrew
brew install openapitools/openapi-generator/openapi-diff

# Docker
docker pull openapitools/openapi-diff:latest

# NPX
npx @openapitools/openapi-diff@latest --version
```

## Run Command

```bash
openapi-diff \
  --fail-on-incompatible \
  --output-markdown /tmp/openapi-diff-result.md \
  <(git show ${BASE_REF}:openapi.yaml 2>/dev/null || echo "{}") \
  openapi.yaml
```

## Breaking vs Non-Breaking Classification

| Change | Classification | Wire Impact |
|--------|---------------|-------------|
| Field removed from request/response | BREAKING | Consumers break |
| Required field added to request | BREAKING | Existing callers fail |
| Type changed (`string` → `integer`) | BREAKING | Serialization break |
| Enum value removed | BREAKING | Known values rejected |
| Optional → Required (response field) | BREAKING | Consumer assumptions break |
| Path/method removed | BREAKING | 404/405 for callers |
| Field added to request/response | NON_BREAKING | Additive — safe |
| Required → Optional | NON_BREAKING | Less strict |
| New path/method added | NON_BREAKING | Backward compat |
| Default value changed | NON_BREAKING (advisory) | Behavioral change, warn |
| Description/title changed | NON_BREAKING | Doc only |

## Output Parsing

```bash
# Exit code: 0=compatible, 1=incompatible, 2=error
RESULT=$(openapi-diff --output-json /tmp/result.json base.yaml current.yaml)
EXIT=$?

BREAKING=$(jq '.incompatible | length' /tmp/result.json)
NON_BREAKING=$(jq '.compatible | length' /tmp/result.json)
```

## CHANGELOG Integration

When breaking change detected, check PR CHANGELOG:

```bash
git diff ${BASE_REF} -- CHANGELOG.md \
  | grep -E '^\+##\s+(Breaking(\s+Changes?)?|BREAKING)\s*$'
```

Match → exit 0 with WARN "documented migration path accepted"
No match → exit 1 `CONTRACT_BREAKING_CHANGE`

## Exit Code Mapping

| openapi-diff outcome | Skill exit code |
|----------------------|----------------|
| Compatible | 0 SUCCESS |
| Incompatible (no CHANGELOG) | 1 CONTRACT_BREAKING_CHANGE |
| Incompatible + CHANGELOG entry | 0 SUCCESS (WARN) |
| Tool not on PATH | 2 OPERATIONAL_ERROR |
| Malformed YAML | 3 CONTRACT_ARTIFACT_INVALID |

## Version Pinning

```yaml
quality:
  contract:
    openapi-diff:
      min-version: "2.0.0"
```

## Notes

- openapi-diff compares semantic structure, not YAML byte-for-byte
- Ensure `openapi.yaml` is at project root or configure path in skill args
- For multi-file OpenAPI specs, merge with `swagger-merger` before running
- Container run: `docker run --rm -v "$PWD":/workspace openapitools/openapi-diff:latest --fail-on-incompatible /workspace/base.yaml /workspace/current.yaml`
