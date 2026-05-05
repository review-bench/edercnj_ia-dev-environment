package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XArchPlanProductCommand")
class XArchPlanProductCommandTest {

    @Test
    void call_whenValidProduct_writesMandatorySummary() {
        var out = new StringWriter();
        int exit = command(out, new StringWriter()).execute("--product-id", "product-0001");

        assertThat(exit).isEqualTo(XArchPlanProductCommand.EXIT_SUCCESS);
        assertThat(out.toString())
                .contains("C4 diagrams generated for product-0001:")
                .contains("CONTEXT")
                .contains("CONTAINER")
                .contains("COMPONENT")
                .contains("[placeholder]");
    }

    @Test
    void call_whenFormatInvalid_returnsValidationExit() {
        var out = new StringWriter();
        int exit =
                command(out, new StringWriter())
                        .execute("--product-id", "product-0001", "--output-format", "svg");

        assertThat(exit).isEqualTo(XArchPlanProductCommand.EXIT_VALIDATION);
        assertThat(out.toString()).contains("Validation error:");
    }

    private CommandLine command(StringWriter out, StringWriter err) {
        var command = new CommandLine(new XArchPlanProductCommand());
        command.setOut(new PrintWriter(out));
        command.setErr(new PrintWriter(err));
        return command;
    }
}
