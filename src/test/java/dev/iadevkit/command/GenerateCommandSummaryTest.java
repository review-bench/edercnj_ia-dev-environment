package dev.iadevkit.command;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadevkit.IaDevKitApplication;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class GenerateCommandSummaryTest {

    private static CommandLine newCli(StringWriter sw) {
        CommandLine cli = new CommandLine(new IaDevKitApplication());
        cli.setOut(new PrintWriter(sw));
        return cli;
    }

    @Test
    void generate_printsPipelineSuccess(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString());

        assertThat(sw.toString()).contains("Pipeline: Success");
    }

    @Test
    void generate_dryRun_printsPipelineDryRun(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString(), "--dry-run");

        assertThat(sw.toString()).contains("Pipeline: Dry Run");
    }

    @Test
    void generate_printsCategoryTable(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString());

        String out = sw.toString();
        assertThat(out).contains("Category");
        assertThat(out).contains("Skills");
        assertThat(out).contains("Agents");
        assertThat(out).contains("Knowledge");
        assertThat(out).contains("Total");
    }

    @Test
    void categorize_knownDirectories() {
        assertThat(GenerateCommand.categorize("agents/foo.md")).isEqualTo("Agents");
        assertThat(GenerateCommand.categorize("skills/core/x-foo/SKILL.md")).isEqualTo("Skills");
        assertThat(GenerateCommand.categorize("knowledge/governance/rule.md"))
                .isEqualTo("Knowledge");
        assertThat(GenerateCommand.categorize("hooks/post-compile-check.sh")).isEqualTo("Hooks");
        assertThat(GenerateCommand.categorize("rules/01-essentials.md")).isEqualTo("Rules");
        assertThat(GenerateCommand.categorize("scripts/audit.sh")).isEqualTo("Scripts");
        assertThat(GenerateCommand.categorize("templates/_TEMPLATE-EPIC.md"))
                .isEqualTo("Templates");
        assertThat(GenerateCommand.categorize("settings.json")).isEqualTo("Settings");
        assertThat(GenerateCommand.categorize("unknown/file.md")).isEqualTo("Other");
    }

    @Test
    void generate_outputContainsElapsedTime(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString());

        assertThat(sw.toString()).containsPattern("\\(\\d+ms\\)");
    }

    @Test
    void generate_outputContainsTotalCount(@TempDir Path tmpDir) {
        StringWriter sw = new StringWriter();
        newCli(sw).execute("generate", "--output", tmpDir.toString());

        assertThat(sw.toString()).contains("Total");
    }
}
