package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

@DisplayName("XCreateProductCommand — argument parsing + help")
class XCreateProductCommandTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("Help text")
    class HelpText {

        @Test
        void help_whenRequested_showsIdeationFileOption() {
            var sw = new StringWriter();
            buildCommandLine(sw).execute("--help");
            assertThat(sw.toString()).contains("--ideation-file");
        }

        @Test
        void help_whenRequested_showsOutputDirOption() {
            var sw = new StringWriter();
            buildCommandLine(sw).execute("--help");
            assertThat(sw.toString()).contains("--output-dir");
        }

        @Test
        void help_whenRequested_showsProductIdOption() {
            var sw = new StringWriter();
            buildCommandLine(sw).execute("--help");
            assertThat(sw.toString()).contains("--product-id");
        }

        @Test
        void help_whenRequested_showsDryRunOption() {
            var sw = new StringWriter();
            buildCommandLine(sw).execute("--help");
            assertThat(sw.toString()).contains("--dry-run");
        }
    }

    @Nested
    @DisplayName("Missing required argument")
    class MissingRequired {

        @Test
        void missingIdeationFile_returnsNonZeroExit() {
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute();
            assertThat(exit).isNotZero();
        }

        @Test
        void missingIdeationFile_showsMissingOptionMessage() {
            var errSw = new StringWriter();
            buildCommandLine(new StringWriter(), errSw).execute();
            assertThat(errSw.toString()).containsIgnoringCase("ideation-file");
        }
    }

    @Nested
    @DisplayName("Invalid argument values")
    class InvalidArgs {

        @Test
        void nonExistentIdeationFile_returnsValidationExitCode() {
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute(
                    "--ideation-file", "/nonexistent/idea.md");
            assertThat(exit).isEqualTo(XCreateProductCommand.EXIT_VALIDATION);
        }

        @Test
        void invalidProductIdFormat_returnsValidationExitCode() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute(
                    "--ideation-file", ideation.toString(),
                    "--product-id", "INVALID-FORMAT-TOO-LONG-123456789012345");
            assertThat(exit).isEqualTo(XCreateProductCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("Valid arguments — dry-run mode")
    class ValidArgsDryRun {

        @Test
        void dryRunWithValidFile_returnsSuccessExitCode() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute(
                    "--ideation-file", ideation.toString(),
                    "--dry-run");
            assertThat(exit).isEqualTo(XCreateProductCommand.EXIT_SUCCESS);
        }

        @Test
        void dryRunWithValidFile_outputContainsDryRunMessage() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var sw = new StringWriter();
            buildCommandLine(sw).execute(
                    "--ideation-file", ideation.toString(),
                    "--dry-run");
            assertThat(sw.toString()).containsIgnoringCase("dry-run");
        }

        @Test
        void dryRunWithCustomOutputDir_acceptsDirectory() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            Path outputDir = Files.createTempDirectory(tempDir, "products");
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute(
                    "--ideation-file", ideation.toString(),
                    "--output-dir", outputDir.toString(),
                    "--dry-run");
            assertThat(exit).isEqualTo(XCreateProductCommand.EXIT_SUCCESS);
        }

        @Test
        void dryRunWithProductIdOverride_acceptsValidProductId() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var sw = new StringWriter();
            int exit = buildCommandLine(sw).execute(
                    "--ideation-file", ideation.toString(),
                    "--product-id", "product-0042",
                    "--dry-run");
            assertThat(exit).isEqualTo(XCreateProductCommand.EXIT_SUCCESS);
        }
    }

    @Nested
    @DisplayName("Argument parser — unit tests")
    class ArgumentParserUnit {

        @Test
        void parse_withRequiredArg_setsIdeationFile() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var result = XCreateProductArgumentParser.parse(
                    ideation.toString(), null, null, false);
            assertThat(result.ideationFile()).isEqualTo(ideation);
        }

        @Test
        void parse_withDefaultOutputDir_usesAiProductsDefault() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var result = XCreateProductArgumentParser.parse(
                    ideation.toString(), null, null, false);
            assertThat(result.outputDir().toString()).endsWith("ai/products");
        }

        @Test
        void parse_withCustomOutputDir_usesProvidedDir() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var result = XCreateProductArgumentParser.parse(
                    ideation.toString(), tempDir.toString(), null, true);
            assertThat(result.outputDir()).isEqualTo(tempDir);
        }

        @Test
        void parse_withProductId_capturesIt() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var result = XCreateProductArgumentParser.parse(
                    ideation.toString(), null, "product-0007", false);
            assertThat(result.productId()).contains("product-0007");
        }

        @Test
        void parse_withDryRunTrue_setsDryRun() throws Exception {
            Path ideation = Files.createTempFile(tempDir, "idea", ".md");
            var result = XCreateProductArgumentParser.parse(
                    ideation.toString(), null, null, true);
            assertThat(result.dryRun()).isTrue();
        }
    }

    private CommandLine buildCommandLine(StringWriter out) {
        return buildCommandLine(out, new StringWriter());
    }

    private CommandLine buildCommandLine(StringWriter out, StringWriter err) {
        var cmd = new CommandLine(new XCreateProductCommand());
        cmd.setOut(new PrintWriter(out));
        cmd.setErr(new PrintWriter(err));
        return cmd;
    }
}
