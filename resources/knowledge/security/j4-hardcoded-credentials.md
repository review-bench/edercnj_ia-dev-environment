---
name: kp-security-j4-hardcoded-credentials
description: "J4 — Password Hardcoded in String (CWE-798). Vulnerable + fixed Java examples with rationale."
requires-capabilities: ["language.java.*"]
---

# J4: Password Hardcoded in String

**CWE:** CWE-798 — Use of Hard-coded Credentials  
**Severity:** CRITICAL

## Vulnerable Code

```java
public class DatabaseConfig {
    private static final String DB_PASSWORD = "s3cr3t!";
    private static final String API_KEY =
            "ak_live_1234567890";
}
```

## Fixed Code

```java
public class DatabaseConfig {
    private final String dbPassword;
    private final String apiKey;

    DatabaseConfig(
            @Value("${db.password}") String dbPassword,
            @Value("${api.key}") String apiKey) {
        this.dbPassword = dbPassword;
        this.apiKey = apiKey;
    }
}
```

## Why it is dangerous

Hard-coded credentials are visible in source control history, compiled bytecode, and memory dumps. If the repository is leaked or decompiled, all environments using those credentials are immediately compromised. Credentials must come from a secrets manager or environment variables.
