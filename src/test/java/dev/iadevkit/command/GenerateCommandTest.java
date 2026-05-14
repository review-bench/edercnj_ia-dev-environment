package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GenerateCommandTest {

    @Test
    void generate_createsClaudeDirectory(@TempDir Path tmpDir) {
        int exit =
                new CommandLine(new IaDevKitApplication())
                        .execute("generate", "--output", tmpDir.toString());

        assertThat(exit).isZero();
        assertThat(tmpDir.resolve(".claude")).isDirectory();
    }

    @Test
    void generate_createsSkillsAndAgentsDirectories(@TempDir Path tmpDir) {
        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString());

        assertThat(tmpDir.resolve(".claude/skills")).isDirectory();
        assertThat(tmpDir.resolve(".claude/agents")).isDirectory();
        assertThat(tmpDir.resolve(".claude/knowledge")).isDirectory();
    }

    @Test
    void generate_copiesSettingsJson(@TempDir Path tmpDir) {
        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString());

        assertThat(tmpDir.resolve(".claude/settings.json")).isRegularFile();
    }

    @Test
    void generate_dryRun_writesNoFiles(@TempDir Path tmpDir) {
        int exit =
                new CommandLine(new IaDevKitApplication())
                        .execute("generate", "--output", tmpDir.toString(), "--dry-run");

        assertThat(exit).isZero();
        assertThat(tmpDir.resolve(".claude")).doesNotExist();
    }

    @Test
    void generate_force_overwritesExisting(@TempDir Path tmpDir) throws Exception {
        Path settingsDir = tmpDir.resolve(".claude");
        Files.createDirectories(settingsDir);
        Path existing = settingsDir.resolve("settings.json");
        Files.writeString(existing, "old-content");

        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString(), "--force");

        assertThat(Files.readString(existing)).doesNotContain("old-content");
    }

    @Test
    void generate_noForce_skipsExistingFiles(@TempDir Path tmpDir) throws Exception {
        Path settingsDir = tmpDir.resolve(".claude");
        Files.createDirectories(settingsDir);
        Path existing = settingsDir.resolve("settings.json");
        Files.writeString(existing, "preserved-content");

        new CommandLine(new IaDevKitApplication())
                .execute("generate", "--output", tmpDir.toString());

        assertThat(Files.readString(existing)).isEqualTo("preserved-content");
    }

    @Test
    void generate_helpExitsZero() {
        int exit = new CommandLine(new IaDevKitApplication()).execute("generate", "--help");
        assertThat(exit).isZero();
    }
}
