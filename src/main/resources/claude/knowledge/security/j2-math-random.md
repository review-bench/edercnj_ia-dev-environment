---
name: kp-security-j2-math-random
description: "J2 — Math.random() for Security (CWE-330). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J2: Math.random() for Security

**CWE:** CWE-330 — Use of Insufficiently Random Values  
**Severity:** HIGH

## Vulnerable Code

```java
public String generateToken() {
    return String.valueOf(Math.random());
}
```

## Fixed Code

```java
public String generateToken() {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
}
```

## Why it is dangerous

`Math.random()` uses a linear congruential generator whose seed can be predicted after observing a few outputs. Session tokens, CSRF tokens, and password reset links generated with `Math.random()` can be forged by an attacker.
