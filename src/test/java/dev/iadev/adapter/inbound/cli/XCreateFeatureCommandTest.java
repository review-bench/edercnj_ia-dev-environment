package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.adapter.inbound.cli.FeatureInputParser;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XCreateFeatureCommand")
class XCreateFeatureCommandTest {

    private CommandLine.ParseResult parse(String... args) {
        return new CommandLine(new XCreateFeatureCommand()).parseArgs(args);
    }

    private int execute(String... args) {
        return new CommandLine(new XCreateFeatureCommand()).execute(args);
    }

    private String errOf(String... args) {
        StringWriter err = new StringWriter();
        CommandLine cli = new CommandLine(new XCreateFeatureCommand());
        cli.setErr(new PrintWriter(err));
        cli.execute(args);
        return err.toString();
    }

    private String outOf(String... args) {
        StringWriter out = new StringWriter();
        CommandLine cli = new CommandLine(new XCreateFeatureCommand());
        cli.setOut(new PrintWriter(out));
        cli.execute(args);
        return out.toString();
    }

    @Nested
    @DisplayName("HelpText")
    class HelpText {

        @Test
        void helpFlag_printsUsage() {
            StringWriter out = new StringWriter();
            CommandLine cli = new CommandLine(new XCreateFeatureCommand());
            cli.setOut(new PrintWriter(out));
            cli.execute("--help");
            assertThat(out.toString()).contains("x-create-feature");
        }

        @Test
        void helpFlag_mentionsMandatoryOption() {
            StringWriter out = new StringWriter();
            CommandLine cli = new CommandLine(new XCreateFeatureCommand());
            cli.setOut(new PrintWriter(out));
            cli.execute("--help");
            assertThat(out.toString()).contains("--capability-id");
        }
    }

    @Nested
    @DisplayName("RequiredArgs")
    class RequiredArgs {

        @Test
        void missingCapabilityId_returnsNonZero() {
            int exit = execute("--auto-decompose");
            assertThat(exit).isNotEqualTo(XCreateFeatureCommand.EXIT_SUCCESS);
        }

        @Test
        void missingDecomposeModeFlag_returnsValidationError() {
            int exit = execute("--capability-id", "capability-c1");
            assertThat(exit).isEqualTo(XCreateFeatureCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("AutoDecompose")
    class AutoDecompose {

        @Test
        void autoDecompose_returnsSuccess() {
            int exit = execute("--capability-id", "capability-c1", "--auto-decompose");
            assertThat(exit).isEqualTo(XCreateFeatureCommand.EXIT_SUCCESS);
        }

        @Test
        void autoDecompose_printsSummaryLine() {
            String out = outOf("--capability-id", "capability-c1", "--auto-decompose");
            assertThat(out).contains("capability-id=capability-c1").contains("auto-decompose=true");
        }
    }

    @Nested
    @DisplayName("FeaturesJson")
    class FeaturesJson {

        @Test
        void validFeaturesJson_returnsSuccess() {
            int exit = execute("--capability-id", "capability-c1",
                    "--features", "[{\"name\":\"BasicAuth\"},{\"name\":\"OAuth2\"},{\"name\":\"MFA\"},{\"name\":\"Session\"}]");
            assertThat(exit).isEqualTo(XCreateFeatureCommand.EXIT_SUCCESS);
        }

        @Test
        void tooFewFeatures_returnsValidationError() {
            int exit = execute("--capability-id", "capability-c1",
                    "--features", "[{\"name\":\"BasicAuth\"},{\"name\":\"OAuth2\"},{\"name\":\"MFA\"}]");
            assertThat(exit).isEqualTo(XCreateFeatureCommand.EXIT_VALIDATION);
        }

        @Test
        void invalidFeaturesFormat_returnsValidationError() {
            int exit = execute("--capability-id", "capability-c1",
                    "--features", "not-json");
            assertThat(exit).isEqualTo(XCreateFeatureCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("FeatureInputParserUnit")
    class FeatureInputParserUnit {

        @Test
        void parseFeatures_fourNames_returnsFour() {
            var result = FeatureInputParser.parseFeatures(
                    "[{\"name\":\"BasicAuth\"},{\"name\":\"OAuth2\"},{\"name\":\"MFA\"},{\"name\":\"Session\"}]");
            assertThat(result).hasSize(4).containsExactly("BasicAuth", "OAuth2", "MFA", "Session");
        }

        @Test
        void parseFeatures_eightNames_returnsEight() {
            String json = "[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"},{\"name\":\"d\"},"
                    + "{\"name\":\"e\"},{\"name\":\"f\"},{\"name\":\"g\"},{\"name\":\"h\"}]";
            var result = FeatureInputParser.parseFeatures(json);
            assertThat(result).hasSize(8);
        }

        @Test
        void parseFeatures_nullInput_throwsIllegalArgument() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> FeatureInputParser.parseFeatures(null));
        }

        @Test
        void parseFeatures_tooFew_throwsIllegalArgument() {
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> FeatureInputParser.parseFeatures(
                            "[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"}]"));
        }

        @Test
        void parseFeatures_tooMany_throwsIllegalArgument() {
            String json = "[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"},{\"name\":\"d\"},"
                    + "{\"name\":\"e\"},{\"name\":\"f\"},{\"name\":\"g\"},{\"name\":\"h\"},{\"name\":\"i\"}]";
            org.junit.jupiter.api.Assertions.assertThrows(
                    IllegalArgumentException.class,
                    () -> FeatureInputParser.parseFeatures(json));
        }
    }
}
