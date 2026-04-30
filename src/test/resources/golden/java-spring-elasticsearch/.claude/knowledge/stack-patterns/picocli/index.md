---
name: picocli
description: "Picocli patterns: @Command, @Option, @Parameters, subcommands, type converters, and output formatting for CLI tools."
requires-capabilities: [cli.picocli.framework]
---

# Pattern: Picocli CLI Patterns

## Purpose

Provides Picocli-specific implementation patterns for building command-line tools. Agents reference this pack when generating code for a Java 21 + Picocli project.

## Supplements

Supplements `architecture` and `layer-templates` knowledge packs with Picocli-specific conventions.

---

## 1. Root Command Structure

```java
@Command(
    name = "mytool",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "My CLI tool description.",
    subcommands = {
        GenerateCommand.class,
        ValidateCommand.class
    }
)
public class MyToolCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        // Root command logic (or show usage)
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new MyToolCommand()).execute(args);
        System.exit(exitCode);
    }
}
```

---

## 2. Subcommand Pattern

```java
@Command(
    name = "generate",
    description = "Generate output from configuration.",
    mixinStandardHelpOptions = true
)
public class GenerateCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true,
            description = "Path to configuration YAML file.")
    private Path configFile;

    @Option(names = {"-o", "--output"}, defaultValue = ".",
            description = "Output directory (default: current directory).")
    private Path outputDir;

    @Option(names = {"--dry-run"},
            description = "Simulate generation without writing files.")
    private boolean dryRun;

    private final GenerationPort generator;

    public GenerateCommand(GenerationPort generator) {
        this.generator = generator;
    }

    @Override
    public Integer call() {
        try {
            var config = generator.loadConfig(configFile);
            var result = generator.generate(config, outputDir, dryRun);
            System.out.printf("Generated %d files%n", result.fileCount());
            return 0;
        } catch (ConfigurationException e) {
            System.err.println("Configuration error: " + e.getMessage());
            return 1;
        }
    }
}
```

---

## 3. @Parameters (Positional)

```java
@Command(name = "process", description = "Process input files.")
public class ProcessCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Input file to process.")
    private Path inputFile;

    @Parameters(index = "1..*", description = "Additional files (optional).")
    private List<Path> additionalFiles = List.of();

    @Override
    public Integer call() {
        // Process inputFile and additionalFiles
        return 0;
    }
}
```

---

## 4. Type Converters

```java
// Custom type converter
public class DurationConverter implements ITypeConverter<Duration> {
    @Override
    public Duration convert(String value) {
        return Duration.parse(value);
    }
}

// Usage
@Option(names = "--timeout", converter = DurationConverter.class,
        description = "Request timeout (ISO-8601, e.g. PT30S).")
private Duration timeout = Duration.ofSeconds(30);
```

---

## 5. Exit Codes (Convention)

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Application error (config, validation) |
| 2 | Usage error (wrong arguments — Picocli default) |
| 3+ | Domain-specific errors |

```java
// Picocli exit code constants
public final class ExitCode {
    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int USAGE = 2;

    private ExitCode() {}
}
```

---

## 6. Output Formatting

```java
// Prefer structured output over println
public class TablePrinter {
    public void print(List<Row> rows, PrintWriter out) {
        out.printf("%-20s %-10s %-30s%n", "NAME", "STATUS", "DESCRIPTION");
        out.println("-".repeat(62));
        for (var row : rows) {
            out.printf("%-20s %-10s %-30s%n",
                row.name(), row.status(), row.description());
        }
    }
}
```

---

## Anti-Patterns (Picocli-Specific)

- `System.exit()` inside command logic (return exit code via `Callable<Integer>`)
- `System.out.println` in domain/application layer (pass `PrintWriter` as parameter)
- Mixing business logic inside command classes (delegate to application ports)
- Catching `Exception` broadly without specific handling per error type
- Hard-coding CLI option names in tests (use constants)
