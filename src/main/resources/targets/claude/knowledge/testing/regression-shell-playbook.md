---
name: regression-shell-playbook
description: Guidance for regression shell quality gate — self vs service mode, drift troubleshooting, golden regeneration policy, scenarios.yaml schema.
visibility: internal
user-invocable: false
requires-capabilities:
  - quality.regression.self
  - quality.regression.service
---

> 🔒 **KNOWLEDGE PACK** — Referenced internally by `x-test-regression-shell`. Not user-invocable.

# Regression Shell Playbook

## Self Mode vs. Service Mode

| Aspect | Self Mode (`quality.regression.self`) | Service Mode (`quality.regression.service`) |
| :--- | :--- | :--- |
| **Target** | Generator's own output — `.claude/` directory | Client project's running services |
| **Tool** | `ia-dev-env generate` + `diff -r` | `curl` / `grpcurl` / `wscat` / native shell |
| **Baseline** | `governance/baselines/regression-self-golden/` (golden snapshot) | `governance/baselines/regression-baseline.json` (response hashes) |
| **Use case** | CI gate on generator changes | CI gate on client service changes |
| **Typical duration** | ~30 seconds | Variable (depends on service startup) |

Self and service are **mutually exclusive** per project (declared via `quality.regression.self` / `quality.regression.service` capability). A single project cannot use both simultaneously.

## Self Mode — Golden Regeneration Policy

The golden snapshot at `governance/baselines/regression-self-golden/` is the reference state for the generator's output. Regenerate it when:

1. A deliberate structural change to `.claude/` output is made (new skill, rule update, hook change).
2. A `scenarios.yaml.template` update changes the generated file content.

**Regeneration procedure:**

```bash
# 1. Generate to temp dir with reference config
ia-dev-env generate --config governance/baselines/regression-self-reference.yaml --output /tmp/regression-golden-new/

# 2. Review the diff
diff -r governance/baselines/regression-self-golden/ /tmp/regression-golden-new/

# 3. If expected, update golden
cp -r /tmp/regression-golden-new/ governance/baselines/regression-self-golden/

# 4. Run baseline update to record the change
/x-test-regression-shell <STORY-ID> --mode self --update-baseline
```

The `governance/baselines/regression-self-baseline.txt` file lists paths exempt from the
golden check (e.g., `*.json` files with timestamps, lock files). Add new exemptions with
a PR comment explaining why.

## Service Mode — `scenarios.yaml` Schema

```yaml
# tests/regression/scenarios.yaml
version: "1.0"
scenarios:
  - id: "health-check"
    type: rest                    # rest | grpc | websocket | cli
    description: "GET /health returns 200"
    command: "curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/health"
    expected:
      exit_code: 0
      stdout_contains: "200"     # optional substring match
      stdout_hash: "abc123..."   # optional SHA-256 of full stdout (strict mode)

  - id: "grpc-hello"
    type: grpc
    description: "HelloService.SayHello returns greeting"
    command: "grpcurl -plaintext localhost:9090 hello.HelloService/SayHello"
    expected:
      exit_code: 0
      stdout_contains: "Hello"

  - id: "cli-generate"
    type: cli
    description: "Generate command exits 0"
    command: "ia-dev-env generate --config test-config.yaml --output /tmp/test-out/ --dry-run"
    expected:
      exit_code: 0
```

**Field reference:**

| Field | Required | Description |
| :--- | :--- | :--- |
| `id` | yes | Unique scenario identifier (kebab-case). Referenced in baseline JSON. |
| `type` | yes | Protocol: `rest`, `grpc`, `websocket`, `cli`. |
| `description` | yes | Human-readable description. |
| `command` | yes | Shell command to execute. Supports env var expansion. |
| `expected.exit_code` | yes | Expected process exit code. |
| `expected.stdout_contains` | no | Substring that must appear in stdout. |
| `expected.stdout_hash` | no | SHA-256 of full stdout for strict comparison. |
| `timeout_seconds` | no | Per-scenario timeout (default 30). |

**Ordering:** Scenarios execute sequentially in file order. Use `depends_on: [id]` to express dependencies (not yet implemented — reserved field).

## Drift Troubleshooting

### Symptom: Unexpected diff in self mode

Possible causes:

1. **Generator template changed** — check `git log src/main/resources/` for recent changes.
2. **YAML profile changed** — check `governance/baselines/regression-self-reference.yaml`.
3. **Non-deterministic output** — check for timestamps, UUIDs, or random seeds in templates.
4. **Tool version change** — check `ia-dev-env --version` matches the version used to create the golden.

Resolution: Determine if the diff is expected (→ regenerate golden) or a regression (→ fix the generator).

### Symptom: Scenario FAIL in service mode

1. **Service not started** — verify the target service is running before executing regression.
2. **Port mismatch** — check `REGRESSION_BASE_URL` env var or hardcoded port in `scenarios.yaml`.
3. **Response body changed** — inspect `stdout_contains` vs actual response.
4. **Exit code mismatch** — service may be returning an error. Check service logs.

## Tooling Minimum Versions

| Tool | Minimum Version | Detection Command |
| :--- | :--- | :--- |
| `curl` | 7.68 | `curl --version \| head -1` |
| `grpcurl` | 1.8 | `grpcurl --version` |
| `wscat` | 5.0 | `wscat --version` |

When a required tool is absent: exit 2 `OPERATIONAL_ERROR` with message `REGRESSION_TOOL_MISSING: <tool> required for <type> scenarios`.
