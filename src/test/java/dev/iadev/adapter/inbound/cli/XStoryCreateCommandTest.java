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

@DisplayName("XStoryCreateCommand")
class XStoryCreateCommandTest {

    @TempDir Path tempDir;

    @Test
    void help_whenRequested_mentionsEpicIdAndFromFeature() {
        StringWriter out = new StringWriter();
        CommandLine cli = buildCli(out, new StringWriter());

        cli.execute("--help");

        assertThat(out.toString()).contains("--from-feature").contains("--epic-id");
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
                                "--epic-id",
                                "EPIC-0077",
                                "--dry-run");

        assertThat(exit).isEqualTo(XStoryCreateCommand.EXIT_SUCCESS);
    }

    @Test
    void execute_withExampleArtifacts_writesThreeStories() throws Exception {
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

        Path epicDir = tempDir.resolve("epic-0077-oauth2-integration");
        Path storyOne = epicDir.resolve("story-0077-0001.md");
        Path storyThree = epicDir.resolve("story-0077-0003.md");

        assertThat(exit).isEqualTo(XStoryCreateCommand.EXIT_SUCCESS);
        assertThat(out.toString()).contains("Stories created: 3");
        assertThat(Files.readString(storyOne)).contains("**Source Feature:** oauth2-integration");
        assertThat(Files.readString(storyThree)).contains("## 2. RNFs Herdadas");
    }

    private CommandLine buildCli(StringWriter out, StringWriter err) {
        CommandLine cli = new CommandLine(new XStoryCreateCommand());
        cli.setOut(new PrintWriter(out));
        cli.setErr(new PrintWriter(err));
        return cli;
    }
}
