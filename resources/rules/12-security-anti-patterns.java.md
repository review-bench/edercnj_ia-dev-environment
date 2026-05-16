---
requires-capabilities: ["lang.java.*"]
---
# Rule 12 — Security Anti-Patterns (Java)

> Language-specific security anti-patterns with vulnerable and fixed code examples.
> Each entry references its CWE identifier and severity level.

## Security Anti-Patterns

### J2: Math.random() for Security
**CWE:** CWE-330 — Use of Insufficiently Random Values
**Severity:** HIGH

#### Vulnerable Code
```java
// Math.random() is predictable and not cryptographic
public String generateToken() {
    return String.valueOf(Math.random());
}
```

#### Fixed Code
```java
// SecureRandom provides cryptographically strong values
public String generateToken() {
    byte[] bytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
}
```

#### Why it is dangerous
`Math.random()` uses a linear congruential generator whose seed can be predicted after observing a few outputs. Tokens and identifiers generated with `Math.random()` can be forged by an attacker.

### J3: ObjectInputStream Without Whitelist
**CWE:** CWE-502 — Deserialization of Untrusted Data
**Severity:** CRITICAL

#### Vulnerable Code
```java
// Deserializes any class from untrusted input
public Object deserialize(byte[] data) {
    try (ObjectInputStream ois =
            new ObjectInputStream(
                    new ByteArrayInputStream(data))) {
        return ois.readObject();
    }
}
```

#### Fixed Code
```java
// ObjectInputFilter restricts allowed classes
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

#### Why it is dangerous
Java deserialization can instantiate arbitrary classes and trigger gadget chains leading to remote code execution. An attacker sending a crafted byte stream can execute arbitrary commands on the server.

### J4: Credentials Hardcoded in Source
**CWE:** CWE-798 — Use of Hard-coded Credentials
**Severity:** CRITICAL

#### Vulnerable Code
```java
// Credentials embedded in source code
public class Config {
    private static final String API_KEY =
            "ak_live_1234567890";
    private static final String SECRET =
            "s3cret!";
}
```

#### Fixed Code
```java
// Credentials loaded from environment variables
public class Config {
    private final String apiKey;
    private final String secret;

    Config() {
        this.apiKey = requireEnv("API_KEY");
        this.secret = requireEnv("APP_SECRET");
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Required env var not set: " + name);
        }
        return value;
    }
}
```

#### Why it is dangerous
Hard-coded credentials are visible in source control history, compiled bytecode, and memory dumps. If the repository is leaked or decompiled, all environments using those credentials are immediately compromised. Credentials must come from environment variables or a secrets manager.

### J6: Path Traversal via User Input
**CWE:** CWE-22 — Improper Limitation of a Pathname to a Restricted Directory
**Severity:** HIGH

#### Vulnerable Code
```java
// User input used directly as file path
public byte[] readFile(String filename) {
    File file = new File("/workspace/" + filename);
    return Files.readAllBytes(file.toPath());
}
```

#### Fixed Code
```java
// Normalize path and verify it stays within base dir
public byte[] readFile(String filename) {
    Path base = Path.of("/workspace").toAbsolutePath();
    Path resolved = base.resolve(filename)
            .normalize().toAbsolutePath();
    if (!resolved.startsWith(base)) {
        throw new SecurityException(
                "Path traversal attempt: "
                        + filename);
    }
    return Files.readAllBytes(resolved);
}
```

#### Why it is dangerous
An attacker can use `../` sequences (e.g., `../../etc/passwd`) to escape the intended directory and read or overwrite arbitrary files on the filesystem. Path normalization and prefix validation are both required to prevent directory traversal.

### J7: Stack Trace Leaked to CLI Output
**CWE:** CWE-209 — Generation of Error Message Containing Sensitive Information
**Severity:** MEDIUM

#### Vulnerable Code
```java
// Full stack trace exposed to user
@Command(name = "run")
public class RunCommand implements Callable<Integer> {
    @Override
    public Integer call() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace(); // leaks internals
        }
        return 1;
    }
}
```

#### Fixed Code
```java
// User-friendly message; details logged separately
@Command(name = "run")
public class RunCommand implements Callable<Integer> {
    private static final Logger log =
            LoggerFactory.getLogger(RunCommand.class);

    @Override
    public Integer call() {
        try {
            doWork();
        } catch (Exception e) {
            log.debug("Unexpected error", e);
            System.err.println("Error: " + e.getMessage()
                    + " (run with --verbose for details)");
        }
        return 1;
    }
}
```

#### Why it is dangerous
Stack traces expose internal class names, file paths, library versions, and application structure. Attackers use this information to craft targeted exploits against known vulnerabilities in the exposed dependencies.
