package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FeatureNumbering")
class FeatureNumberingTest {

    @Test
    void assignIds_fourNames_returnsFeature0001To0004() {
        Map<String, String> result =
                FeatureNumbering.assignIds(List.of("BasicAuth", "OAuth2", "MFA", "Session"));
        assertThat(result.get("BasicAuth")).isEqualTo("feature-0001");
        assertThat(result.get("OAuth2")).isEqualTo("feature-0002");
        assertThat(result.get("MFA")).isEqualTo("feature-0003");
        assertThat(result.get("Session")).isEqualTo("feature-0004");
    }

    @Test
    void assignIds_preservesInsertionOrder() {
        Map<String, String> result =
                FeatureNumbering.assignIds(List.of("z-feat", "a-feat", "m-feat", "b-feat"));
        assertThat(result.get("z-feat")).isEqualTo("feature-0001");
        assertThat(result.get("a-feat")).isEqualTo("feature-0002");
        assertThat(result.get("m-feat")).isEqualTo("feature-0003");
        assertThat(result.get("b-feat")).isEqualTo("feature-0004");
    }

    @Test
    void assignIds_eightNames_assignsUpToFeature0008() {
        var names = List.of("a", "b", "c", "d", "e", "f", "g", "h");
        var result = FeatureNumbering.assignIds(names);
        assertThat(result.get("h")).isEqualTo("feature-0008");
    }

    @Test
    void assignIds_nullList_throwsIllegalArgument() {
        assertThatThrownBy(() -> FeatureNumbering.assignIds(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void assignIds_emptyList_throwsIllegalArgument() {
        assertThatThrownBy(() -> FeatureNumbering.assignIds(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
