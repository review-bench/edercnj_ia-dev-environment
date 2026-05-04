package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XCreateCapabilityCommand")
class XCreateCapabilityCommandTest {

    private CommandLine buildCli() {
        return new CommandLine(new XCreateCapabilityCommand());
    }

    @Nested
    @DisplayName("Help text")
    class HelpText {
        @Test
        void helpFlag_printsUsage() {
            StringWriter out = new StringWriter();
            CommandLine cli = buildCli();
            cli.setOut(new PrintWriter(out));
            cli.execute("--help");
            assertThat(out.toString()).contains("x-create-capability");
        }

        @Test
        void helpFlag_containsProductId() {
            StringWriter out = new StringWriter();
            CommandLine cli = buildCli();
            cli.setOut(new PrintWriter(out));
            cli.execute("--help");
            assertThat(out.toString()).contains("--product-id");
        }
    }

    @Nested
    @DisplayName("Required arguments")
    class RequiredArgs {
        @Test
        void missingProductId_returnsNonZero() {
            int exit = buildCli().execute("--auto-decompose");
            assertThat(exit).isNotEqualTo(0);
        }

        @Test
        void noDecomposeFlag_noCapabilities_returnsValidation() {
            int exit = buildCli().execute("--product-id", "product-0001");
            assertThat(exit).isEqualTo(XCreateCapabilityCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("Auto-decompose mode")
    class AutoDecompose {
        @Test
        void autoDecompose_returnsSuccess() {
            int exit = buildCli().execute("--product-id", "product-0001", "--auto-decompose");
            assertThat(exit).isEqualTo(XCreateCapabilityCommand.EXIT_SUCCESS);
        }

        @Test
        void autoDecompose_outputContainsProductId() {
            StringWriter out = new StringWriter();
            CommandLine cli = buildCli();
            cli.setOut(new PrintWriter(out));
            cli.execute("--product-id", "product-0001", "--auto-decompose");
            assertThat(out.toString()).contains("product-0001");
        }
    }

    @Nested
    @DisplayName("Capabilities JSON mode")
    class CapabilitiesJson {
        @Test
        void validCapabilitiesJson_returnsSuccess() {
            int exit = buildCli().execute(
                    "--product-id", "product-0001",
                    "--capabilities", "[\"ingest\",\"query\",\"storage\"]");
            assertThat(exit).isEqualTo(XCreateCapabilityCommand.EXIT_SUCCESS);
        }

        @Test
        void invalidCapabilitiesJson_returnsValidation() {
            int exit = buildCli().execute(
                    "--product-id", "product-0001",
                    "--capabilities", "not-json");
            assertThat(exit).isEqualTo(XCreateCapabilityCommand.EXIT_VALIDATION);
        }

        @Test
        void tooFewCapabilities_returnsValidation() {
            int exit = buildCli().execute(
                    "--product-id", "product-0001",
                    "--capabilities", "[\"ingest\",\"query\"]");
            assertThat(exit).isEqualTo(XCreateCapabilityCommand.EXIT_VALIDATION);
        }
    }

    @Nested
    @DisplayName("CapabilityInteractiveInputParser unit")
    class ParserUnit {
        @Test
        void parse_validThreeElements_returnsList() {
            var result = CapabilityInteractiveInputParser.parseCapabilities(
                    "[\"ingest\",\"query\",\"storage\"]");
            assertThat(result).containsExactly("ingest", "query", "storage");
        }

        @Test
        void parse_sevenElements_returnsList() {
            var result = CapabilityInteractiveInputParser.parseCapabilities(
                    "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]");
            assertThat(result).hasSize(7);
        }

        @Test
        void parse_notArray_throwsIllegalArgument() {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> CapabilityInteractiveInputParser.parseCapabilities("{\"key\":\"val\"}"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void parse_tooMany_throwsIllegalArgument() {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> CapabilityInteractiveInputParser.parseCapabilities(
                            "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\"]"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void parse_nullInput_throwsIllegalArgument() {
            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> CapabilityInteractiveInputParser.parseCapabilities(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
