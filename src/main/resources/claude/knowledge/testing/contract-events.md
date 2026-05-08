---
name: contract-events
description: "Schema registry compatibility and SCC event contract playbook for event-driven contract testing"
requires-capabilities: [quality.contract.scc]
---

# Knowledge Pack: Contract Testing — Events / Schema Registry (SCC + Avro compat)

## Tool Matrix

| Runtime | Tool | Min Version | Purpose |
|---------|------|-------------|---------|
| JVM | Spring Cloud Contract | 4.0 | Consumer-driven stubs (Spring Boot) |
| any | Schema Registry CLI | depends | Avro/JSON-Schema/Protobuf compat check |
| any | `curl` / `kafka-schema-registry-client` | — | REST API compat check |

## Spring Cloud Contract (SCC) Stubs

### Run Command

```bash
mvn spring-cloud-contract:verify \
  --no-transfer-progress \
  -Dspring.cloud.contract.stubs.repository.uri=${SCC_STUB_REPO_URL} \
  -q 2>&1
```

### Stub Repository Guard

When `quality.contract.scc.stub-repository-url` absent or unreachable:

```bash
if [ -z "${SCC_STUB_REPO_URL}" ]; then
  echo "WARN: SCC stub repository not configured — skipping Spring Cloud Contract verification"
  exit 0
fi
curl -s --head "${SCC_STUB_REPO_URL}" | head -1 | grep "200" || {
  echo "WARN: SCC stub repository unreachable — skipping"
  exit 0
}
```

## Schema Registry Compatibility Check

### BACKWARD Compatibility (default — safe for Avro consumers)

New schema MUST be able to read data written by the old schema:

```bash
# Check compatibility via Confluent Schema Registry REST API
curl -X POST \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -u "${SCHEMA_REGISTRY_USER}:${SCHEMA_REGISTRY_PASSWORD}" \
  "${SCHEMA_REGISTRY_URL}/compatibility/subjects/${SUBJECT}-value/versions/latest" \
  -d "{\"schema\": $(cat schema.avsc | jq -Rs .)}" \
  2>&1
```

### Breaking vs Non-Breaking (Avro)

| Change | BACKWARD compat | FORWARD compat | Recommendation |
|--------|----------------|----------------|---------------|
| Field added with default | Compatible | Compatible | Safe — always add defaults |
| Field added without default | Incompatible (BACKWARD) | Compatible | Add default or use FORWARD mode |
| Field removed | Compatible (BACKWARD) | Incompatible | Use tombstone / deprecation flow |
| Field type changed | Incompatible | Incompatible | Always BREAKING — avoid |
| Enum value added | Compatible | Incompatible | Additive, warn consumers |
| Enum value removed | Incompatible | Compatible | BREAKING — never remove |
| Default value changed | Compatible | Compatible | Non-breaking |

### Auth Failure Guard

```bash
COMPAT_RESULT=$(curl -s --fail -u "${SCHEMA_REGISTRY_USER}:${SCHEMA_REGISTRY_PASSWORD}" \
  "${SCHEMA_REGISTRY_URL}/compatibility/..." 2>&1)
CURL_EXIT=$?
if [ $CURL_EXIT -ne 0 ]; then
  echo "WARN: schema registry auth failed — skipping event contract check"
  echo "auth_skipped" >> contract-report.md
  exit 0  # fail-open — do not block merge
fi
```

## Exit Code Mapping

| Outcome | Skill exit code |
|---------|----------------|
| All compatible | 0 SUCCESS |
| Breaking (no CHANGELOG) | 1 CONTRACT_BREAKING_CHANGE |
| Breaking + CHANGELOG entry | 0 SUCCESS (WARN) |
| Auth failure / registry unreachable | 0 SUCCESS (WARN: auth_skipped) |
| SCC stub repo missing | 0 SUCCESS (WARN: scc_skipped) |
| Malformed schema file | 3 CONTRACT_ARTIFACT_INVALID |

## Security Notes

- **NEVER** log `SCHEMA_REGISTRY_USER` or `SCHEMA_REGISTRY_PASSWORD` in reports
- Auth failures → `auth_skipped` in report, no credential details
- Use environment variables only, never hardcode in YAML/code

## Version Pinning

```yaml
quality:
  contract:
    scc:
      stub-repository-url: "${SCC_STUB_REPO_URL}"
    events:
      schema-registry-url: "${SCHEMA_REGISTRY_URL}"
      subject-name-strategy: "TopicNameStrategy"
```

## Notes

- Subject naming: `TopicNameStrategy` (default) = topic name + `-value` / `-key` suffix
- Schema evolution best practices: always use `BACKWARD` compat for consumer safety
- Never remove enum values; deprecate by convention and remove after all consumers migrated
- SCC stubs use WireMock under the hood; Pact is a separate, broker-based alternative
