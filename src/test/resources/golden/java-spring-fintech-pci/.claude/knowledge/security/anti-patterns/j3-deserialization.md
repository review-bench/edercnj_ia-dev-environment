---
name: kp-security-j3-deserialization
description: "J3 — ObjectInputStream Without Whitelist (CWE-502). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J3: ObjectInputStream Without Whitelist

**CWE:** CWE-502 — Deserialization of Untrusted Data  
**Severity:** CRITICAL

## Vulnerable Code

```java
public Object deserialize(byte[] data) {
    try (ObjectInputStream ois =
            new ObjectInputStream(
                    new ByteArrayInputStream(data))) {
        return ois.readObject();
    }
}
```

## Fixed Code

```java
public Object deserialize(byte[] data) {
    try (ObjectInputStream ois =
            new ObjectInputStream(
                    new ByteArrayInputStream(data))) {
        ois.setObjectInputFilter(
                ObjectInputFilter.Config.createFilter(
                        "com.example.dto.*;!*"));
        return ois.readObject();
    }
}
```

## Why it is dangerous

Java deserialization can instantiate arbitrary classes and trigger gadget chains (e.g., Commons Collections, Spring beans) leading to remote code execution. An attacker sending a crafted byte stream can execute arbitrary commands on the server.
