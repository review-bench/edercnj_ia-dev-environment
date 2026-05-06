package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class XInternalRnfValidateCommandTest {

    private XInternalRnfValidateCommand command() {
        return new XInternalRnfValidateCommand();
    }

    private CommandLine buildCommandLine(XInternalRnfValidateCommand cmd, StringWriter err) {
        CommandLine cli = new CommandLine(cmd);
        cli.setErr(new PrintWriter(err));
        return cli;
    }

    @Test
    void parseOne_norelaxSpec_returnsNoRelaxOverride() {
        RNFOverride override = command().parseOne("PERFORMANCE:norelax");

        assertThat(override.category()).isEqualTo(RNFCategory.PERFORMANCE);
        assertThat(override.noRelaxed()).isTrue();
        assertThat(override.isRelaxed()).isFalse();
    }

    @Test
    void parseOne_norelaxWithValue_returnsOverrideWithOriginalValue() {
        RNFOverride override = command().parseOne("SECURITY:norelax:TLS 1.3");

        assertThat(override.category()).isEqualTo(RNFCategory.SECURITY);
        assertThat(override.originalValue()).isEqualTo("TLS 1.3");
        assertThat(override.noRelaxed()).isTrue();
    }

    @Test
    void parseOne_relaxedSpec_returnsRelaxedOverride() {
        RNFOverride override =
                command().parseOne("PERFORMANCE:relaxed:P99<200ms:P99<500ms:justified");

        assertThat(override.category()).isEqualTo(RNFCategory.PERFORMANCE);
        assertThat(override.isRelaxed()).isTrue();
        assertThat(override.justification()).isEqualTo("justified");
    }

    @Test
    void parseOne_invalidSpec_throwsIllegalArgument() {
        assertThatThrownBy(() -> command().parseOne("INVALID_FORMAT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void call_allValid_returnsZero() {
        var err = new StringWriter();
        var cmd = new XInternalRnfValidateCommand();
        var cli = buildCommandLine(cmd, err);

        int exit = cli.execute("--override", "PERFORMANCE:norelax");

        assertThat(exit).isEqualTo(0);
    }

    @Test
    void call_securityRelaxed_returnsOne() {
        var err = new StringWriter();
        var cmd = new XInternalRnfValidateCommand();
        var cli = buildCommandLine(cmd, err);

        int exit = cli.execute("--override", "SECURITY:relaxed:TLS 1.3:TLS 1.2:legacy client");

        assertThat(exit).isEqualTo(1);
        assertThat(err.toString()).contains("SECURITY");
    }

    @Test
    void call_dryRunWithViolation_returnsZero() {
        var err = new StringWriter();
        var cmd = new XInternalRnfValidateCommand();
        var cli = buildCommandLine(cmd, err);

        int exit =
                cli.execute("--dry-run", "--override", "SECURITY:relaxed:TLS 1.3:TLS 1.2:legacy");

        assertThat(exit).isEqualTo(0);
    }

    @Test
    void call_invalidSpec_returnsTwo() {
        var err = new StringWriter();
        var cmd = new XInternalRnfValidateCommand();
        var cli = buildCommandLine(cmd, err);

        int exit = cli.execute("--override", "BADFORMAT");

        assertThat(exit).isEqualTo(2);
    }
}
