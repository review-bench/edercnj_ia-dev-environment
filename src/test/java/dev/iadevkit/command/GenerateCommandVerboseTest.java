package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GenerateCommandVerboseTest {

    private static CommandLine newCli(StringWriter sw) {
        CommandLine cli = new CommandLine(new IaDevKitApplication());
        cli.setOut(new PrintWriter(sw));
        return cli;
    }

    @Test
    void verbose_printsEachCopiedFile(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--verbose");

        assertThat(sw.toString()).contains("copy");
    }

    @Test
    void verbose_dryRun_printsWouldCopy(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--dry-run", "--verbose");

        assertThat(sw.toString()).contains("would copy");
    }

    @Test
    void verbose_skipsExistingFile(@TempDir Path tmpDir) throws Exception {
        Path claudeDir = tmpDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("settings.json"), "existing");

        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--verbose");

        assertThat(sw.toString()).contains("skip");
    }

    @Test
    void generate_copiesClaudioMd(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString());

        assertThat(tmpDir.resolve("CLAUDE.md")).isRegularFile();
    }

    @Test
    void generate_force_overwritesClaudioMd(@TempDir Path tmpDir) throws Exception {
        Files.writeString(tmpDir.resolve("CLAUDE.md"), "old");

        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--force");

        assertThat(Files.readString(tmpDir.resolve("CLAUDE.md"))).doesNotContain("old");
    }

    @Test
    void verbose_skipsExistingClaudioMd(@TempDir Path tmpDir) throws Exception {
        Files.writeString(tmpDir.resolve("CLAUDE.md"), "existing");

        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--verbose");

        assertThat(sw.toString()).contains("skip");
    }

    @Test
    void verbose_dryRun_printsClaudioMd(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--dry-run", "--verbose");

        assertThat(sw.toString()).contains("CLAUDE.md");
    }
}
