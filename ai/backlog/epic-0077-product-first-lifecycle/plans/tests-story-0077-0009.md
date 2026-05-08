# Test Plan — story-0077-0009

## Strategy

- Unit: argument parsing, domain transformation, idempotency hash calculation
- Integration: orchestration use case end-to-end with file I/O
- E2E: CLI invocation via smoke script

## Key Scenarios

1. Valid ideation → product created with 12+ RNFs Root + C1 stub
2. Idempotent rerun (same ideation) → same product, no duplicate write
3. Invalid ideation (missing section) → exit 1 with clear error
4. `--dry-run` → validates but writes nothing
