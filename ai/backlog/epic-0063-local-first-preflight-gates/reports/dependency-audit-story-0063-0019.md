# Dependency Audit — story-0063-0019

**Story:** NDJSON Integrity Hash Chain
**Epic:** EPIC-0063 (Local-First Pre-Flight Gates)
**Date:** 2026-04-28

## Runtime Dependencies

| Tool | Purpose | Availability | Risk |
|---|---|---|---|
| `sha256sum` | SHA-256 hash computation (Linux) | Standard on GNU coreutils | LOW |
| `shasum -a 256` | SHA-256 hash computation (macOS) | Built into macOS | LOW |
| `jq` | JSON parsing of anchor file (optional) | Optional — falls back to grep | LOW |
| `awk` | Extract hash from sha256sum output | Standard POSIX | LOW |
| `grep` | Fallback JSON parsing without jq | Standard POSIX | LOW |
| `sed` | Fallback JSON value extraction | Standard POSIX | LOW |
| `date` | ISO8601 timestamp generation | Standard POSIX | LOW |

## Security Assessment

- No network dependencies
- No third-party libraries
- All tools are standard system utilities
- Script produces no side effects beyond writing the anchor JSON file

## Compatibility Matrix

| OS | sha256 Tool | jq Available | Status |
|---|---|---|---|
| Linux (Ubuntu/Debian) | sha256sum | Usually yes | SUPPORTED |
| Linux (Alpine) | sha256sum | Optional | SUPPORTED (falls back to grep) |
| macOS | shasum -a 256 | Usually via brew | SUPPORTED |

## Verdict

No dependency risks. All tools are standard POSIX utilities or well-known coreutils.
The optional jq fallback ensures functionality without jq installed.
