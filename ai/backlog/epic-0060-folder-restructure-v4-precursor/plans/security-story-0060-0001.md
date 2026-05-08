# Security Assessment — story-0060-0001: PathResolver helper + schema v4

**Story:** story-0060-0001
**Epic:** EPIC-0060
**Date:** 2026-04-27
**Overall Risk:** LOW

---

## 1. Threat Surface

`PathResolver` is a utility class that constructs `java.nio.file.Path` values from
user-supplied `epicId` strings and a constructor-injected `basePath`. It performs
filesystem probe via `Files.newDirectoryStream`. No network I/O; no database; no
serialization of user input beyond path construction.

`ExecutionState` is a domain model serialized/deserialized with Jackson.
The only new field is `flowVersion` (string constant).

---

## 2. Risk Analysis

### R-1: Path Traversal (CWE-22) — CRITICAL concern, mitigated

| Field | Detail |
| :--- | :--- |
| **Threat** | Caller passes `epicId = "../secrets"` to escape `basePath`. |
| **Attack vector** | Direct API call from a skill or test with a crafted `epicId`. |
| **Mitigation** | `epicId` validated against `^\d{4}$` before any filesystem access. Non-matching input throws `IllegalArgumentException` immediately — no path is constructed. |
| **Residual risk** | None — regex allows only `[0-9]{4}`, making traversal sequences impossible. |
| **Rule ref** | Rule 06, Rule 12 §J6 (CWE-22) |

```java
// Correct guard (required in TASK-001 implementation)
private static void validateEpicId(String epicId) {
    if (!epicId.matches("^\\d{4}$")) {
        throw new IllegalArgumentException(
            "Invalid epicId format: must be 4-digit string (e.g., \"0060\")");
    }
}
```

### R-2: Symlink Following — LOW concern, mitigated by default

| Field | Detail |
| :--- | :--- |
| **Threat** | Glob probe follows a symlink to a directory outside `basePath`. |
| **Attack vector** | Attacker plants a symlink named `epic-0060-malicious` inside `ai/epics/`. |
| **Mitigation** | `Files.newDirectoryStream` with a glob does **not** follow symlinks by default on all major JVM implementations (JDK 11+). The returned `Path` is the symlink entry, not its target; callers that subsequently call `Files.readAllBytes` on child paths are outside the scope of this story. |
| **Residual risk** | NEGLIGIBLE — `PathResolver` returns `Path` objects only; it does not perform read/write I/O on the returned paths. |
| **Rule ref** | Rule 06 (path operations — symlinks) |

### R-3: Sensitive Data in Paths — N/A

| Field | Detail |
| :--- | :--- |
| **Threat** | Paths expose PII or credentials. |
| **Assessment** | Paths contain only epic IDs (4-digit integers) and slugs derived from epic titles. No user PII, tokens, or secrets appear in path segments. |
| **Residual risk** | None. |
| **Rule ref** | Rule 07 (structured logging — no PII) |

### R-4: IOException Leak via Error Message — LOW concern, mitigated

| Field | Detail |
| :--- | :--- |
| **Threat** | `IOException` from `Files.newDirectoryStream` exposes internal filesystem paths in logs. |
| **Mitigation** | Catch block logs at `WARN` with structured fields (`epic_id`, `error_message`, `fallback_to`) — no stack trace exposed to callers. Exception is swallowed; probe returns `Optional.empty()`. |
| **Residual risk** | NEGLIGIBLE — no exception propagated to external callers. |
| **Rule ref** | Rule 12 §J7 (CWE-209), Rule 07 (structured logging) |

### R-5: Jackson Deserialization of ExecutionState — LOW concern, inherently safe

| Field | Detail |
| :--- | :--- |
| **Threat** | Unsafe deserialization of `flowVersion` field (CWE-502). |
| **Assessment** | `flowVersion` is a `String` field. Jackson deserializes strings without class loading, gadget chains, or reflection on arbitrary types. No `@JsonTypeInfo` with `NONE`/`OBJECT` polymorphism. |
| **Residual risk** | None. |
| **Rule ref** | Rule 12 §J3 (CWE-502 — not applicable here; documented for completeness) |

---

## 3. OWASP / ASVS Mapping

| ASVS ID | Requirement | Applicable? | Status |
| :--- | :--- | :--- | :--- |
| V5.2.2 | Path traversal prevention | Yes | PASS — regex guard |
| V5.2.3 | Symlink escape prevention | Yes | PASS — `newDirectoryStream` default |
| V7.4.1 | Error messages do not expose internals | Yes | PASS — structured WARN log |
| V13.3.1 | Deserialization without gadget risk | Yes | PASS — plain String field |

---

## 4. Security Checklist

- [x] Input validation: `epicId` regex `^\d{4}$` enforced before filesystem access
- [x] Path normalization: all constructed paths resolved against `basePath.toAbsolutePath()`
- [x] No symlink following in probe
- [x] No sensitive data in constructed paths
- [x] Exception messages do not expose stack traces to callers
- [x] No use of `Math.random()` or insecure RNG (not applicable)
- [x] No hardcoded credentials (not applicable)
- [x] No `ObjectInputStream` (not applicable)

---

## 5. Conclusion

Overall risk: **LOW**. The primary CWE-22 risk is fully mitigated by the `epicId` regex
guard applied before any path construction. No additional security controls are required
for this story beyond what is specified in the implementation plan.
