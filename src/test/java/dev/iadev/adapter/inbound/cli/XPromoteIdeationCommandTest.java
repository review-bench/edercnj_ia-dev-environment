package dev.iadev.adapter.inbound.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class XPromoteIdeationCommandTest {

    private CommandLine cmd() {
        return new CommandLine(new XPromoteIdeationCommand());
    }

    @Test
    void call_noSource_returnsValidationError() {
        int exit = cmd().execute("--validate");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_VALIDATION);
    }

    @Test
    void call_fromStdin_validateOnly_returnsSuccess() {
        int exit = cmd().execute("--from-stdin", "--validate");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_SUCCESS);
    }

    @Test
    void call_fromFile_returnsSuccess() {
        int exit = cmd().execute("--from-file", "/tmp/idea.md");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_SUCCESS);
    }

    @Test
    void call_fromStdin_returnsSuccess() {
        int exit = cmd().execute("--from-stdin");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_SUCCESS);
    }

    @Test
    void call_invalidIdeationId_returnsValidationError() {
        int exit = cmd().execute("--from-stdin", "--ideation-id", "bad-id");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_VALIDATION);
    }

    @Test
    void call_validIdeationId_returnsSuccess() {
        int exit = cmd().execute("--from-stdin", "--ideation-id", "ideation-0001");
        assertThat(exit).isEqualTo(XPromoteIdeationCommand.EXIT_SUCCESS);
    }

    @Test
    void call_fromFile_outputContainsSource() {
        StringWriter out = new StringWriter();
        CommandLine cl = cmd();
        cl.setOut(new PrintWriter(out));
        cl.execute("--from-file", "/tmp/idea.md");
        assertThat(out.toString()).contains("source=/tmp/idea.md");
    }

    @Test
    void call_validateOnly_outputContainsMode() {
        StringWriter out = new StringWriter();
        CommandLine cl = cmd();
        cl.setOut(new PrintWriter(out));
        cl.execute("--from-stdin", "--validate");
        assertThat(out.toString()).contains("mode=validate-only");
    }
}
