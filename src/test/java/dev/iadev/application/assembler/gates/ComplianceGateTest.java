package dev.iadev.application.assembler.gates;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.model.ProjectConfig;
import dev.iadev.testutil.TestConfigBuilder;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ComplianceGate")
class ComplianceGateTest {

    private final ComplianceGate gate = new ComplianceGate();

    @Test
    @DisplayName("pci-dss compliance includes" + " x-review-compliance")
    void evaluate_pciDss_includesReviewCompliance() {
        ProjectConfig config = TestConfigBuilder.builder().compliance("pci-dss").build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).containsExactly("x-review-compliance");
    }

    @Test
    @DisplayName("no compliance frameworks returns empty")
    void evaluate_noCompliance_returnsEmpty() {
        ProjectConfig config = TestConfigBuilder.builder().build();

        List<String> skills = gate.evaluate(config);

        assertThat(skills).isEmpty();
    }
}
