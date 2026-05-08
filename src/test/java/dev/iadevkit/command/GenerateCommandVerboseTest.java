package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GenerateCommandVerboseTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void setUp() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void verbose_printsEachCopiedFile(@TempDir Path tmpDir) {
        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString(), "--verbose");

        String output = captured.toString();
        assertThat(output).contains("copy");
    }

    @Test
    void verbose_dryRun_printsWouldCopy(@TempDir Path tmpDir) {
        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString(), "--dry-run", "--verbose");

        String output = captured.toString();
        assertThat(output).contains("would copy");
    }

    @Test
    void verbose_skipsExistingFile(@TempDir Path tmpDir) throws Exception {
        Path claudeDir = tmpDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), "existing");

        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString(), "--verbose");

        String output = captured.toString();
        assertThat(output).contains("skip");
    }

    @Test
    void generate_copiesClaudioMd(@TempDir Path tmpDir) {
        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString());

        assertThat(tmpDir.resolve("CLAUDE.md")).isRegularFile();
    }

    @Test
    void generate_force_overwritesClaudioMd(@TempDir Path tmpDir) throws Exception {
        Files.writeString(tmpDir.resolve("CLAUDE.md"), "old");

        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString(), "--force");

        assertThat(Files.readString(tmpDir.resolve("CLAUDE.md"))).doesNotContain("old");
    }
}
