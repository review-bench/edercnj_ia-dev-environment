package dev.iadev.adapter.inbound.cli;

import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.application.capability.ValidateRNFNoRelaxUseCase;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRootValidationResult;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.io.TempDir;

class XInternalRnfValidateCommandTest {

    @TempDir
    Path tempDir;

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
    void parseOne_relaxedSpecWithApproval_returnsRelaxedOverride() {
        RNFOverride override = command().parseOne(
                "PERFORMANCE:relaxed:P99<200ms:P99<500ms:justified:approved:cto@example.com");

        assertThat(override.category()).isEqualTo(RNFCategory.PERFORMANCE);
        assertThat(override.isRelaxed()).isTrue();
        assertThat(override.justification()).isEqualTo("justified");
        assertThat(override.approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(override.approver()).isEqualTo("cto@example.com");
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

        int exit = cli.execute("--dry-run", "--override", "SECURITY:relaxed:TLS 1.3:TLS 1.2:legacy");

        assertThat(exit).isEqualTo(0);
    }

    @Test
    void call_relaxedWithoutApproval_returnsOne() {
        var err = new StringWriter();
        var cli = buildCommandLine(new XInternalRnfValidateCommand(), err);

        int exit = cli.execute("--override", "PERFORMANCE:relaxed:P99<200ms:P99<1s:migration");

        assertThat(exit).isEqualTo(1);
        assertThat(err.toString()).contains("without approval");
    }

    @Test
    void call_artifactWithApprovedOverride_returnsZero() throws Exception {
        Path artifact = writeArtifact("""
                ## 2. RNFs Herdadas (no-relax override)
                | Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
                | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
                | PERFORMANCE | P99 < 3s | false | P99 < 500ms | Stricter login SLA | approved | cto@example.com |
                | SECURITY | TLS 1.3 | true | — | — | — | — |
                """);
        var err = new StringWriter();
        var cli = buildCommandLine(new XInternalRnfValidateCommand(), err);

        int exit = cli.execute("--artifact", artifact.toString());

        assertThat(exit).isZero();
    }

    @Test
    void call_artifactWithoutApproval_returnsOne() throws Exception {
        Path artifact = writeArtifact("""
                ## 2. RNFs Herdadas (no-relax override)
                | Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |
                | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
                | PERFORMANCE | P99 < 3s | false | P99 < 500ms | Stricter login SLA | — | — |
                """);
        var err = new StringWriter();
        var cli = buildCommandLine(new XInternalRnfValidateCommand(), err);

        int exit = cli.execute("--artifact", artifact.toString());

        assertThat(exit).isEqualTo(1);
        assertThat(err.toString()).contains("without approval");
    }

    @Test
    void call_invalidSpec_returnsTwo() {
        var err = new StringWriter();
        var cmd = new XInternalRnfValidateCommand();
        var cli = buildCommandLine(cmd, err);

        int exit = cli.execute("--override", "BADFORMAT");

        assertThat(exit).isEqualTo(2);
    }

    private Path writeArtifact(String content) throws Exception {
        Path artifact = tempDir.resolve("capability-auth.md");
        Files.writeString(artifact, content);
        return artifact;
    }
}
