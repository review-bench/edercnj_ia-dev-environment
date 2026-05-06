package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityFeatureDecomposition")
class CapabilityFeatureDecompositionTest {

    @Test
    void constructor_validArgs_createsRecord() {
        var decomp =
                new CapabilityFeatureDecomposition(
                        "capability-c1", List.of("BasicAuth", "OAuth2", "MFA", "Session"));
        assertThat(decomp.capabilityId()).isEqualTo("capability-c1");
        assertThat(decomp.featureNames()).hasSize(4);
    }

    @Test
    void constructor_nullCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(
                        () -> new CapabilityFeatureDecomposition(null, List.of("a", "b", "c", "d")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_emptyCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(
                        () -> new CapabilityFeatureDecomposition("", List.of("a", "b", "c", "d")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_tooFewFeatures_throwsIllegalArgument() {
        assertThatThrownBy(
                        () -> new CapabilityFeatureDecomposition("cap-c1", List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_tooManyFeatures_throwsIllegalArgument() {
        assertThatThrownBy(
                        () ->
                                new CapabilityFeatureDecomposition(
                                        "cap-c1",
                                        List.of("a", "b", "c", "d", "e", "f", "g", "h", "i")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void featureNames_isUnmodifiable() {
        var decomp =
                new CapabilityFeatureDecomposition("capability-c1", List.of("a", "b", "c", "d"));
        assertThatThrownBy(() -> decomp.featureNames().add("extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
