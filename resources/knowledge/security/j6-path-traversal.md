---
name: kp-security-j6-path-traversal
description: "J6 — new File(userInput) Without Normalization (CWE-22). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J6: new File(userInput) Without Normalization

**CWE:** CWE-22 — Improper Limitation of a Pathname to a Restricted Directory  
**Severity:** HIGH

## Vulnerable Code

```java
public byte[] readFile(String filename) {
    File file = new File("/uploads/" + filename);
    return Files.readAllBytes(file.toPath());
}
```

## Fixed Code

```java
public byte[] readFile(String filename) {
    Path base = Path.of("/uploads").toAbsolutePath();
    Path resolved = base.resolve(filename)
            .normalize().toAbsolutePath();
    if (!resolved.startsWith(base)) {
        throw new SecurityException(
                "Path traversal attempt: " + filename);
    }
    return Files.readAllBytes(resolved);
}
```

## Why it is dangerous

An attacker can use `../` sequences (e.g., `../../etc/passwd`) to escape the intended directory and read or overwrite arbitrary files on the server. Path normalization and prefix validation are both required to prevent directory traversal.
