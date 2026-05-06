package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XArchPlanCapabilityCommand")
class XArchPlanCapabilityCommandTest {

    @Test
    void call_whenValidCapability_writesMandatorySummary() {
        var out = new StringWriter();
        int exit = command(out, new StringWriter()).execute("--capability-id", "capability-auth");

        assertThat(exit).isEqualTo(XArchPlanCapabilityCommand.EXIT_SUCCESS);
        assertThat(out.toString())
                .contains("C4 diagrams generated for capability-auth:")
                .contains("CONTEXT")
                .contains("CONTAINER")
                .contains("COMPONENT")
                .contains("[placeholder]");
    }

    @Test
    void call_whenCapabilityBlank_returnsValidationExit() {
        var out = new StringWriter();
        int exit = command(out, new StringWriter()).execute("--capability-id", " ");

        assertThat(exit).isEqualTo(XArchPlanCapabilityCommand.EXIT_VALIDATION);
        assertThat(out.toString()).contains("Validation error:");
    }

    private CommandLine command(StringWriter out, StringWriter err) {
        var command = new CommandLine(new XArchPlanCapabilityCommand());
        command.setOut(new PrintWriter(out));
        command.setErr(new PrintWriter(err));
        return command;
    }
}
