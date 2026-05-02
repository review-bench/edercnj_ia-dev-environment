package dev.iadev.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Governance — quality field integration")
class GovernanceQualityTest {

    @Test
    void fromMap_noQualityBlock_defaultsAllDisabled() {
        var root =
                Map.<String, Object>of(
                        "compliance", "none",
                        "branching-model", "gitflow");

        var gov = Governance.fromMap(root);

        assertThat(gov.quality()).isEqualTo(QualityConfig.DEFAULT);
        assertThat(gov.quality().performance().enabled()).isFalse();
        assertThat(gov.quality().mutation().enabled()).isFalse();
        assertThat(gov.quality().contract().enabled()).isFalse();
    }

    @Test
    void fromMap_qualityBlockPresent_parsedCorrectly() {
        var qualityMap =
                Map.<String, Object>of(
                        "performance", Map.of("enabled", true),
                        "mutation", Map.<String, Object>of("enabled", true, "threshold", 90),
                        "contract", Map.of("enabled", true));

        var root = Map.<String, Object>of("compliance", "none", "quality", qualityMap);

        var gov = Governance.fromMap(root);

        assertThat(gov.quality().performance().enabled()).isTrue();
        assertThat(gov.quality().mutation().enabled()).isTrue();
        assertThat(gov.quality().mutation().threshold()).isEqualTo(90);
        assertThat(gov.quality().contract().enabled()).isTrue();
    }

    @Test
    void constructor_nullQuality_defaultsToDefault() {
        var gov = new Governance("none", null, null, true, null, null, null);

        assertThat(gov.quality()).isEqualTo(QualityConfig.DEFAULT);
    }
}
