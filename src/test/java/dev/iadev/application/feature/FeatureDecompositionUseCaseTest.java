package dev.iadev.application.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.feature.AutoDecomposeFeatureHeuristic;
import dev.iadev.domain.feature.CapabilityToFeatureTransformer;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FeatureDecompositionUseCase")
class FeatureDecompositionUseCaseTest {

    private final FeatureDecompositionUseCase useCase = new FeatureDecompositionUseCase(
            new AutoDecomposeFeatureHeuristic(),
            new CapabilityToFeatureTransformer());

    @Test
    void execute_autoDecompose_returnsBetweenFourAndEightFeatures() {
        var result = useCase.execute("cap-c1", List.of());
        assertThat(result.featureNames()).hasSizeBetween(4, 8);
    }

    @Test
    void execute_explicitNames_returnsSameNames() {
        var names = List.of("BasicAuth", "OAuth2", "MFA", "Session");
        var result = useCase.execute("cap-c1", names);
        assertThat(result.featureNames()).containsExactlyElementsOf(names);
    }

    @Test
    void execute_nullNames_treatsAsAutoDecompose() {
        var result = useCase.execute("cap-c1", null);
        assertThat(result.featureNames()).hasSizeBetween(4, 8);
    }

    @Test
    void execute_nullCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(() -> useCase.execute(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
