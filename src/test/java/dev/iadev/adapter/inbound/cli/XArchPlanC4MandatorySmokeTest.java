package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

@DisplayName("XArchPlan C4 Mandatory Smoke")
class XArchPlanC4MandatorySmokeTest {

    @Test
    void productCommand_whenExecuted_marksComponentAsPlaceholder() {
        var out = new StringWriter();
        int exit =
                command(new XArchPlanProductCommand(), out).execute("--product-id", "product-0001");

        assertThat(exit).isZero();
        assertThat(out.toString()).contains("COMPONENT").contains("[placeholder]");
    }

    @Test
    void capabilityCommand_whenExecuted_marksContextAsPlaceholder() {
        var out = new StringWriter();
        int exit =
                command(new XArchPlanCapabilityCommand(), out)
                        .execute("--capability-id", "capability-auth");

        assertThat(exit).isZero();
        assertThat(out.toString()).contains("CONTEXT").contains("[placeholder]");
    }

    @Test
    void featureCommand_whenExecuted_marksComponentAsPlaceholder() {
        var out = new StringWriter();
        int exit =
                command(new XArchPlanFeatureCommand(), out)
                        .execute("--feature-id", "feature-oauth2");

        assertThat(exit).isZero();
        assertThat(out.toString()).contains("COMPONENT").contains("[placeholder]");
    }

    private CommandLine command(Object command, StringWriter out) {
        var line = new CommandLine(command);
        line.setOut(new PrintWriter(out));
        return line;
    }
}
