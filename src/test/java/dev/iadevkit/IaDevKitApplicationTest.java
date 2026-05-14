package dev.iadevkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class IaDevKitApplicationTest {

    private static CommandLine newCli(StringWriter sw) {
        CommandLine cli = new CommandLine(new IaDevKitApplication());
        cli.setOut(new PrintWriter(sw));
        return cli;
    }

    @Test
    void noArgs_displaysUsage() {
        StringWriter sw = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine cli = new CommandLine(new IaDevKitApplication());
        cli.setOut(new PrintWriter(sw));
        cli.setErr(new PrintWriter(err));
        cli.execute();

        // picocli prints usage to err when no subcommand is given; output contains subcommand name
        assertThat(sw.toString() + err.toString()).contains("generate");
    }

    @Test
    void version_printsVersionLine() {
        StringWriter sw = new StringWriter();
        int exit = newCli(sw).execute("--version");

        assertThat(exit).isZero();
        assertThat(sw.toString()).contains("ia-dev-kit");
    }

    @Test
    void unknownSubcommand_returnsNonZero() {
        StringWriter sw = new StringWriter();
        int exit = newCli(sw).execute("nonexistent-command");

        assertThat(exit).isNotZero();
    }

    @Test
    void help_exitsZero() {
        StringWriter sw = new StringWriter();
        int exit = newCli(sw).execute("--help");

        assertThat(exit).isZero();
    }
}
