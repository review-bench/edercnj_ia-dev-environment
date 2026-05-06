package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AutoDecomposeFeatureHeuristic")
class AutoDecomposeFeatureHeuristicTest {

    private final AutoDecomposeFeatureHeuristic heuristic = new AutoDecomposeFeatureHeuristic();

    @Test
    void decompose_anyCapabilityId_returnsBetweenFourAndEightFeatures() {
        var result = heuristic.decompose("capability-c1");
        assertThat(result).hasSizeBetween(4, 8);
    }

    @Test
    void decompose_returnsNonBlankNames() {
        var result = heuristic.decompose("capability-c1");
        assertThat(result).allSatisfy(name -> assertThat(name).isNotBlank());
    }

    @Test
    void decompose_nullCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(() -> heuristic.decompose(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decompose_blankCapabilityId_throwsIllegalArgument() {
        assertThatThrownBy(() -> heuristic.decompose("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decompose_differentCapabilities_returnsSameDefaults() {
        var r1 = heuristic.decompose("capability-c1");
        var r2 = heuristic.decompose("capability-c9");
        assertThat(r1).containsExactlyElementsOf(r2);
    }
}
