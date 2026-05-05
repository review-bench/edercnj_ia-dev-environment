package dev.iadev.domain.capability;

import dev.iadev.domain.product.RNFCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RNFNoRelaxValidatorTest {

    private final RNFNoRelaxValidator validator = new RNFNoRelaxValidator();

    @Test
    void allNoRelax_noOverrides_passes() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.noRelax(RNFCategory.PERFORMANCE, "P99 < 3s"),
                RNFOverride.noRelax(RNFCategory.SECURITY, "ICP-Brasil Nível 2")
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void oneRelaxed_withApproval_passes() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.noRelax(RNFCategory.SECURITY, "ICP-Brasil Nível 2"),
                RNFOverride.withApproval(
                        RNFCategory.PERFORMANCE,
                        "P99 < 3s",
                        "P99 < 500ms",
                        "Auth path requires stricter SLA",
                        ApprovalStatus.APPROVED,
                        "cto@example.com")
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isTrue();
    }

    @Test
    void oneRelaxed_noJustification_rejects() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.noRelax(RNFCategory.SECURITY, "ICP-Brasil Nível 2"),
                RNFOverride.withOverride(RNFCategory.PERFORMANCE, "P99 < 3s", "P99 < 1s", null)
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("PERFORMANCE") && e.contains("justification"));
    }

    @Test
    void oneRelaxed_withoutApproval_rejects() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.withOverride(
                        RNFCategory.PERFORMANCE,
                        "P99 < 3s",
                        "P99 < 10s",
                        "Temporary downgrade for migration")
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("without approval"));
    }

    @Test
    void security_relaxed_rejectsEvenWithJustification() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.withApproval(
                        RNFCategory.SECURITY,
                        "ICP-Brasil Nível 2",
                        "basic TLS only",
                        "legacy system",
                        ApprovalStatus.APPROVED,
                        "cto@example.com")
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("SECURITY") && e.contains("mandatory"));
    }

    @Test
    void compliance_relaxed_rejectsEvenWithJustification() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.withApproval(
                        RNFCategory.COMPLIANCE,
                        "LGPD Art.46",
                        "best effort",
                        "startup exemption",
                        ApprovalStatus.APPROVED,
                        "risk@example.com")
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("COMPLIANCE") && e.contains("mandatory"));
    }

    @Test
    void multipleViolations_reportsAll() {
        List<RNFOverride> overrides = List.of(
                RNFOverride.withApproval(
                        RNFCategory.SECURITY,
                        "ICP-Brasil",
                        "TLS only",
                        "legacy",
                        ApprovalStatus.APPROVED,
                        "cto@example.com"),
                RNFOverride.withOverride(RNFCategory.PERFORMANCE, "P99 < 3s", "P99 < 10s", null)
        );

        var result = validator.validate(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).hasSize(3);
    }
}
