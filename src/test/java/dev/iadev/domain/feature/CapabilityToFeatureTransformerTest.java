package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityToFeatureTransformer")
class CapabilityToFeatureTransformerTest {

    private final CapabilityToFeatureTransformer transformer = new CapabilityToFeatureTransformer();

    @Test
    void transform_validArgs_returnsDecomposition() {
        var result =
                transformer.transform("cap-c1", List.of("BasicAuth", "OAuth2", "MFA", "Session"));
        assertThat(result.capabilityId()).isEqualTo("cap-c1");
        assertThat(result.featureNames()).containsExactly("BasicAuth", "OAuth2", "MFA", "Session");
    }

    @Test
    void transform_eightFeatures_succeeds() {
        var names = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        var result = transformer.transform("cap-c2", names);
        assertThat(result.featureNames()).hasSize(8);
    }

    @Test
    void transform_nullCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(() -> transformer.transform(null, List.of("a", "b", "c", "d")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_nullNames_throwsIllegalArgument() {
        assertThatThrownBy(() -> transformer.transform("cap-c1", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_tooFewNames_throwsIllegalArgument() {
        assertThatThrownBy(() -> transformer.transform("cap-c1", List.of("a", "b", "c")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void transform_tooManyNames_throwsIllegalArgument() {
        assertThatThrownBy(
                        () ->
                                transformer.transform(
                                        "cap-c1",
                                        List.of("a", "b", "c", "d", "e", "f", "g", "h", "i")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
