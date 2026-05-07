---
name: picocli-command
description: "Generates a Picocli @Command with subcommands, options, converters, and unit tests."
visibility: public
model: sonnet
requires-capabilities: [cli.picocli.framework]
allowed-tools: [Read, Write, Edit, Bash, Skill]
context-budget: light
---

# Skill: Picocli Command Generator

## Purpose

Generates a complete Picocli CLI command (or subcommand) following the hexagonal architecture pattern. Produces:

1. `@Command` class implementing `Callable<Integer>`
2. `@Option` and `@Parameters` declarations with type converters
3. Exit code constants
4. Unit tests with `CommandLine.execute()` assertions

## Triggers

- `/picocli-command <CommandName> [--subcommand]` — generate a top-level or subcommand
- `/picocli-command <CommandName> --root` — generate root command with standard options
- `/picocli-command <CommandName> --crud` — generate subcommands for create/list/delete operations

## Parameters

| Parameter | Required | Description |
|-----------|----------|-------------|
| `CommandName` | Yes | PascalCase command class name (e.g., `GenerateCommand`, `ValidateCommand`) |
| `--subcommand` | No | Generate as subcommand (registers with parent via `subcommands =`) |
| `--root` | No | Generate as root command with `mixinStandardHelpOptions = true` |
| `--crud` | No | Generate create/list/delete subcommands |

## Workflow

### Step 1 — Read Project Context

Read existing conventions:
- `knowledge/stack-patterns/picocli/index.md` — Picocli patterns
- `knowledge/layer-templates.md` — Layer template patterns
- Existing commands in `src/main/java/**/adapter/inbound/cli/` for naming conventions

### Step 2 — Generate Command Class

```java
// src/main/java/{package}/adapter/inbound/cli/{CommandName}.java
@Command(
    name = "{command-name}",
    description = "{description}",
    mixinStandardHelpOptions = true
)
public class {CommandName} implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, required = true,
            description = "Config file path.")
    private Path configFile;

    private final {Port} port;

    public {CommandName}({Port} port) {
        this.port = port;
    }

    @Override
    public Integer call() {
        try {
            // Delegate to application port
            var result = port.execute(...);
            System.out.printf("Success: %s%n", result);
            return ExitCode.OK;
        } catch (DomainException e) {
            System.err.println("Error: " + e.getMessage());
            return ExitCode.ERROR;
        }
    }
}
```

### Step 3 — Generate Exit Codes (if not exists)

```java
// src/main/java/{package}/adapter/inbound/cli/ExitCode.java
public final class ExitCode {
    public static final int OK = 0;
    public static final int ERROR = 1;
    public static final int USAGE = 2;

    private ExitCode() {}
}
```

### Step 4 — Generate Tests

```java
// src/test/java/{package}/adapter/inbound/cli/{CommandName}Test.java
class {CommandName}Test {
    private final {Port} port = mock({Port}.class);
    private final CommandLine cmd = new CommandLine(new {CommandName}(port));

    @Test
    void call_validInput_returnsZero() {
        // Arrange
        when(port.execute(any())).thenReturn(expectedResult);

        // Act
        int exitCode = cmd.execute("--config", "test.yaml");

        // Assert
        assertThat(exitCode).isEqualTo(ExitCode.OK);
    }

    @Test
    void call_missingRequiredOption_returnsUsageError() {
        int exitCode = cmd.execute();
        assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    }
}
```

## Output Checklist

- [ ] Command implements `Callable<Integer>` (not `Runnable`)
- [ ] Constructor injection of port (no static dependencies)
- [ ] `ExitCode` constants used (not magic numbers)
- [ ] `System.err` for errors, `System.out` for success output
- [ ] `mixinStandardHelpOptions = true` on root command
- [ ] Tests use `CommandLine.execute()` (not direct `call()`)
- [ ] No `System.exit()` inside command logic

## Knowledge Pack References

- `knowledge/stack-patterns/picocli/index.md` — Picocli patterns
- `knowledge/layer-templates.md` — Layer template patterns
- `knowledge/architecture-hexagonal.md` — Hexagonal architecture
