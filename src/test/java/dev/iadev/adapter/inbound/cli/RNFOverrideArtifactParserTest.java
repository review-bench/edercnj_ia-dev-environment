package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.ApprovalStatus;
import dev.iadev.domain.capability.RNFOverride;
import dev.iadev.domain.product.RNFCategory;
import java.util.List;
import org.junit.jupiter.api.Test;

class RNFOverrideArtifactParserTest {

    private final RNFOverrideArtifactParser parser = new RNFOverrideArtifactParser();

    @Test
    void parse_whenInheritanceTablePresent_returnsOverrides() {
        List<RNFOverride> overrides = parser.parse(List.of(
                "# Capability",
                "## 2. RNFs Herdadas (no-relax override)",
                "| Categoria | RNF Original (Produto) | no-relax? | Override Value | Justificação | Approval Status | Approver |",
                "| :--- | :--- | :--- | :--- | :--- | :--- | :--- |",
                "| PERFORMANCE | P99 < 3s | false | P99 < 500ms | Critical auth path | approved | cto@example.com |",
                "| SECURITY | TLS 1.3 | true | — | — | — | — |",
                "## 3. Another section"));

        assertThat(overrides).hasSize(2);
        assertThat(overrides.get(0).category()).isEqualTo(RNFCategory.PERFORMANCE);
        assertThat(overrides.get(0).approvalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(overrides.get(1).noRelaxed()).isTrue();
    }
}
