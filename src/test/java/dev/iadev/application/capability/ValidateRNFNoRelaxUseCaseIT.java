package dev.iadev.application.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import dev.iadev.domain.product.RNFRootValidationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidateRNFNoRelaxUseCaseIT {

    private final ValidateRNFNoRelaxUseCase useCase = new ValidateRNFNoRelaxUseCase();

    @Test
    void execute_allNoRelax_passes() {
        List<RNFOverride> overrides =
                List.of(
                        RNFOverride.noRelax(RNFCategory.PERFORMANCE, "P99 < 200ms"),
                        RNFOverride.noRelax(RNFCategory.SECURITY, "TLS 1.3"));

        RNFRootValidationResult result = useCase.execute(overrides);

        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void execute_relaxedWithJustification_passes() {
        List<RNFOverride> overrides =
                List.of(
                        RNFOverride.withOverride(
                                RNFCategory.PERFORMANCE,
                                "P99 < 200ms",
                                "P99 < 500ms",
                                "Batch processing path"));

        RNFRootValidationResult result = useCase.execute(overrides);

        assertThat(result.passed()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void execute_securityRelaxed_fails() {
        List<RNFOverride> overrides =
                List.of(
                        RNFOverride.withOverride(
                                RNFCategory.SECURITY,
                                "TLS 1.3",
                                "TLS 1.2",
                                "Legacy client support"));

        RNFRootValidationResult result = useCase.execute(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("SECURITY"));
    }

    @Test
    void execute_complianceRelaxed_fails() {
        List<RNFOverride> overrides =
                List.of(
                        RNFOverride.withOverride(
                                RNFCategory.COMPLIANCE,
                                "PCI DSS Level 1",
                                "PCI DSS Level 2",
                                "Cost reduction"));

        RNFRootValidationResult result = useCase.execute(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("COMPLIANCE"));
    }

    @Test
    void execute_relaxedNoJustification_fails() {
        List<RNFOverride> overrides =
                List.of(
                        RNFOverride.withOverride(
                                RNFCategory.PERFORMANCE, "P99 < 200ms", "P99 < 1s", null));

        RNFRootValidationResult result = useCase.execute(overrides);

        assertThat(result.passed()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("PERFORMANCE"));
    }

    @Test
    void execute_nullOverrides_throwsIllegalArgument() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overrides");
    }
}
