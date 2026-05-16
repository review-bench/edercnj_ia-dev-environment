---
requires-capabilities: ["lang.java.*", "framework.picocli"]
---
# Rule 10 — Anti-Patterns (Java + Picocli)

> CLI-specific anti-patterns with incorrect and correct code examples.
> Each entry references the rule or knowledge pack it violates.

## Anti-Patterns

### ANTI-001: Command Injection via Runtime.exec() (CRITICAL)
**Category:** SECURITY
**Rule violated:** `12-security-anti-patterns.java.md` (CWE-78)

**Incorrect code:**
```java
// User input concatenated into shell command
@Command(name = "run")
public class RunCommand implements Callable<Integer> {
    @Parameters String script;

    @Override
    public Integer call() throws Exception {
        Runtime.getRuntime().exec("bash -c " + script);
        return 0;
    }
}
```

**Correct code:**
```java
// Command and args passed as separate tokens — no shell interpolation
@Command(name = "run")
public class RunCommand implements Callable<Integer> {
    @Parameters String script;

    @Override
    public Integer call() throws Exception {
        new ProcessBuilder("bash", script)
                .inheritIO()
                .start()
                .waitFor();
        return 0;
    }
}
```

### ANTI-002: Credentials Passed as CLI Arguments (HIGH)
**Category:** SECURITY
**Rule violated:** `12-security-anti-patterns.java.md` (CWE-214)

**Incorrect code:**
```java
// Password visible in ps output and shell history
@Command(name = "connect")
public class ConnectCommand implements Callable<Integer> {
    @Option(names = "--password") String password;
    @Option(names = "--api-key")  String apiKey;
}
```

**Correct code:**
```java
// Read sensitive values from env vars or prompted interactively
@Command(name = "connect")
public class ConnectCommand implements Callable<Integer> {
    @Option(names = "--password-env",
            description = "Env var holding the password",
            defaultValue = "APP_PASSWORD")
    String passwordEnv;

    @Override
    public Integer call() {
        String password = System.getenv(passwordEnv);
        if (password == null) {
            char[] pwd = System.console()
                    .readPassword("Password: ");
            password = new String(pwd);
        }
        // use password
        return 0;
    }
}
```

### ANTI-003: Exit Code Always Zero on Failure (HIGH)
**Category:** CORRECTNESS
**Rule violated:** `03-coding-standards.md` (Unix conventions)

**Incorrect code:**
```java
// Swallows errors and always exits 0 — breaks scripting
@Override
public Integer call() {
    try {
        doWork();
    } catch (Exception e) {
        System.err.println("Failed: " + e.getMessage());
    }
    return 0; // caller cannot detect failure
}
```

**Correct code:**
```java
// Distinct exit codes allow callers to detect failure
@Override
public Integer call() {
    try {
        doWork();
        return 0;
    } catch (NotFoundException e) {
        System.err.println("Not found: " + e.getMessage());
        return 2;
    } catch (Exception e) {
        System.err.println("Error: " + e.getMessage());
        return 1;
    }
}
```

### ANTI-004: God Command with Multiple Responsibilities (HIGH)
**Category:** SERVICE_LAYER
**Rule violated:** `03-coding-standards.md#solid` (SRP)

**Incorrect code:**
```java
// Single command doing fetch, transform, and report — violates SRP
@Command(name = "process")
public class ProcessCommand implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        List<Record> data = httpClient.fetch(url);
        List<Result> results = transform(data);
        writeReport(results, outputPath);
        sendEmail(results, recipient);
        return 0;
    }
}
```

**Correct code:**
```java
// Command delegates to focused use cases
@Command(name = "process")
public class ProcessCommand implements Callable<Integer> {
    private final FetchUseCase fetch;
    private final TransformUseCase transform;
    private final ReportUseCase report;

    @Override
    public Integer call() throws Exception {
        var data    = fetch.execute(url);
        var results = transform.execute(data);
        report.execute(results, outputPath);
        return 0;
    }
}
```

### ANTI-005: Printing Stack Trace to stdout/stderr (MEDIUM)
**Category:** UX / SECURITY
**Rule violated:** `12-security-anti-patterns.java.md` (CWE-209)

**Incorrect code:**
```java
// Stack trace leaks implementation details to user
@Override
public Integer call() {
    try {
        doWork();
    } catch (Exception e) {
        e.printStackTrace(); // leaks internals, bad UX
    }
    return 1;
}
```

**Correct code:**
```java
// Friendly message to user; full detail behind --verbose flag
@Override
public Integer call() {
    try {
        doWork();
        return 0;
    } catch (Exception e) {
        log.debug("Unexpected error", e);
        System.err.println("Error: " + e.getMessage()
                + " (use --verbose for details)");
        return 1;
    }
}
```

### ANTI-006: Mutable Static State in Command (MEDIUM)
**Category:** CORRECTNESS / TESTING
**Rule violated:** `03-coding-standards.md#solid` (DIP)

**Incorrect code:**
```java
// Static mutable state breaks testability and thread safety
@Command(name = "generate")
public class GenerateCommand implements Callable<Integer> {
    private static int fileCount = 0; // shared across calls

    @Override
    public Integer call() {
        fileCount = 0;
        copyFiles();
        System.out.println("Copied: " + fileCount);
        return 0;
    }
}
```

**Correct code:**
```java
// Instance state scoped to a single invocation
@Command(name = "generate")
public class GenerateCommand implements Callable<Integer> {
    private int fileCount = 0;

    @Override
    public Integer call() {
        copyFiles();
        System.out.println("Copied: " + fileCount);
        return 0;
    }
}
```
