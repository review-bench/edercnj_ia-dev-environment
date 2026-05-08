---
name: anti-patterns-java
description: Java security anti-patterns index — CWE mapping, severity table, and links to individual anti-pattern KPs with vulnerable/fixed code examples
requires-capabilities: [lang.java.*]
---
# Java Security Anti-Patterns — Full Reference

> **Always-loaded summary:** `.claude/rules/00-essentials.md §4 Forbidden Top-Level`
> **Individual pattern KPs:** `.claude/knowledge/security/anti-patterns/j{N}-{name}.md`

## Index (CWE + Severity)

| ID | Pattern | CWE | Severity |
| :--- | :--- | :--- | :--- |
| J1 | SQL Concatenation with String | CWE-89 | CRITICAL |
| J2 | Math.random() for Security | CWE-330 | HIGH |
| J3 | ObjectInputStream Without Whitelist | CWE-502 | CRITICAL |
| J4 | Password Hardcoded in String | CWE-798 | CRITICAL |
| J5 | X509TrustManager Empty (Trust All) | CWE-295 | CRITICAL |
| J6 | new File(userInput) Without Normalization | CWE-22 | HIGH |
| J7 | Exception Message in HTTP Response | CWE-209 | MEDIUM |
| J8 | CORS allowedOrigins("*") | CWE-942 | HIGH |

## J1: SQL Concatenation (CWE-89 CRITICAL)

**Vulnerable:** Concatenating user input into SQL — allows `' OR 1=1 --` style injection.
**Fixed:** Use parameterized queries with `?` placeholders.

## J2: Math.random() for Security (CWE-330 HIGH)

**Vulnerable:** `Math.random()` uses a predictable linear congruential generator.
**Fixed:** Use `SecureRandom.getInstanceStrong()` for all security-sensitive random values.

## J3: ObjectInputStream Without Whitelist (CWE-502 CRITICAL)

**Vulnerable:** Deserializing any class from untrusted input allows gadget chain RCE.
**Fixed:** Use `ObjectInputFilter.Config.createFilter("com.example.dto.*;!*")` to restrict allowed classes.

## J4: Hardcoded Password (CWE-798 CRITICAL)

**Vulnerable:** Credentials embedded in source code are visible in git history and decompiled bytecode.
**Fixed:** Load from `@Value("${db.password}")` or secrets manager.

## J5: Trust-All TLS (CWE-295 CRITICAL)

**Vulnerable:** Empty `X509TrustManager` disables certificate validation — allows MITM attacks.
**Fixed:** Use `TrustManagerFactory.getDefaultAlgorithm()` with the system CA store.

## J6: Path Traversal (CWE-22 HIGH)

**Vulnerable:** `new File("/uploads/" + userInput)` allows `../../etc/passwd` traversal.
**Fixed:** Normalize with `Path.normalize()` and verify `startsWith(base)` before any I/O.

## J7: Exception Leakage (CWE-209 MEDIUM)

**Vulnerable:** Returning `e.getMessage()` to clients reveals stack traces and class names.
**Fixed:** Log the exception server-side; return generic `ErrorResponse("Internal server error", "ERR-500")`.

## J8: CORS Wildcard (CWE-942 HIGH)

**Vulnerable:** `allowedOrigins("*")` with `allowCredentials(true)` — allows any site to read authenticated responses.
**Fixed:** Restrict to known trusted domains: `allowedOrigins("https://app.example.com")`.

## Detailed Examples

Read individual KPs for full vulnerable/fixed code examples:
- `.claude/knowledge/security/anti-patterns/j1-sql-concatenation.md`
- `.claude/knowledge/security/anti-patterns/j2-math-random.md`
- `.claude/knowledge/security/anti-patterns/j3-deserialization.md`
- `.claude/knowledge/security/anti-patterns/j4-hardcoded-credentials.md`
- `.claude/knowledge/security/anti-patterns/j5-trust-all-tls.md`
- `.claude/knowledge/security/anti-patterns/j6-path-traversal.md`
- `.claude/knowledge/security/anti-patterns/j7-exception-leakage.md`
- `.claude/knowledge/security/anti-patterns/j8-cors-wildcard.md`
