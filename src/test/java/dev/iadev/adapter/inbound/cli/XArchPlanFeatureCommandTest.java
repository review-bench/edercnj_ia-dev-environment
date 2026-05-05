package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XArchPlanFeatureCommand")
class XArchPlanFeatureCommandTest {

    @Test
    void call_whenValidFeature_writesMandatorySummary() {
        var out = new StringWriter();
        int exit = command(out, new StringWriter()).execute("--feature-id", "feature-oauth2");

        assertThat(exit).isEqualTo(XArchPlanFeatureCommand.EXIT_SUCCESS);
        assertThat(out.toString())
                .contains("C4 diagrams generated for feature-oauth2:")
                .contains("CONTEXT")
                .contains("CONTAINER")
                .contains("COMPONENT")
                .contains("[placeholder]");
    }

    @Test
    void call_whenFeatureBlank_returnsValidationExit() {
        var out = new StringWriter();
        int exit = command(out, new StringWriter()).execute("--feature-id", " ");

        assertThat(exit).isEqualTo(XArchPlanFeatureCommand.EXIT_VALIDATION);
        assertThat(out.toString()).contains("Validation error:");
    }

    private CommandLine command(StringWriter out, StringWriter err) {
        var command = new CommandLine(new XArchPlanFeatureCommand());
        command.setOut(new PrintWriter(out));
        command.setErr(new PrintWriter(err));
        return command;
    }
}
