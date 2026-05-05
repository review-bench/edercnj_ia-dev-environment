package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("XEpicCreateCommand")
class XEpicCreateCommandTest {

    @TempDir Path tempDir;

    @Test
    void help_whenRequested_mentionsFromFeature() {
        StringWriter out = new StringWriter();
        CommandLine cli = buildCli(out, new StringWriter());

        cli.execute("--help");

        assertThat(out.toString()).contains("--from-feature");
    }

    @Test
    void execute_withoutFromFeature_returnsNonZero() {
        int exit = buildCli(new StringWriter(), new StringWriter()).execute();

        assertThat(exit).isNotEqualTo(XEpicCreateCommand.EXIT_SUCCESS);
    }

    @Test
    void execute_dryRunWithExampleArtifacts_returnsSuccess() {
        int exit =
                buildCli(new StringWriter(), new StringWriter())
                        .execute(
                                "--from-feature",
                                "ai/examples/example-feature-oauth2-integration.md",
                                "--capability-file",
                                "ai/examples/example-capability-auth.md",
                                "--product-file",
                                "ai/examples/example-product-saas.md",
                                "--dry-run");

        assertThat(exit).isEqualTo(XEpicCreateCommand.EXIT_SUCCESS);
    }

    @Test
    void execute_withExampleArtifacts_writesEpicWithFeatureLineage() throws Exception {
        StringWriter out = new StringWriter();
        int exit =
                buildCli(out, new StringWriter())
                        .execute(
                                "--from-feature",
                                "ai/examples/example-feature-oauth2-integration.md",
                                "--capability-file",
                                "ai/examples/example-capability-auth.md",
                                "--product-file",
                                "ai/examples/example-product-saas.md",
                                "--epic-id",
                                "0077",
                                "--output-dir",
                                tempDir.toString());

        Path epicFile = tempDir.resolve("epic-0077-oauth2-integration").resolve("epic-0077.md");

        assertThat(exit).isEqualTo(XEpicCreateCommand.EXIT_SUCCESS);
        assertThat(out.toString()).contains("inheritedRnfs=");
        assertThat(Files.readString(epicFile)).contains("**Source Feature:** oauth2-integration");
        assertThat(Files.readString(epicFile)).contains("## 7. Índice de Histórias");
        assertThat(Files.readString(epicFile)).doesNotContain("## 2. Persona & Stakeholders");
    }

    private CommandLine buildCli(StringWriter out, StringWriter err) {
        CommandLine cli = new CommandLine(new XEpicCreateCommand());
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));
        return cli;
    }
}
