package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GherkinACGenerator")
class GherkinACGeneratorTest {

    private final GherkinACGenerator generator = new GherkinACGenerator();

    @Test
    void generate_anyFeatureName_returnsAtLeastTenScenarios() {
        List<String> scenarios = generator.generate("OAuth2 Integration");
        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(10);
    }

    @Test
    void generate_scenariosContainGivenWhenThen() {
        List<String> scenarios = generator.generate("BasicAuth");
        assertThat(scenarios).allSatisfy(scenario ->
                assertThat(scenario).containsAnyOf("Given", "When", "Then"));
    }

    @Test
    void generate_scenariosContainScenarioKeyword() {
        List<String> scenarios = generator.generate("MFA");
        assertThat(scenarios).allSatisfy(scenario ->
                assertThat(scenario).startsWith("Scenario:"));
    }

    @Test
    void generate_coversAllFourCategories() {
        List<String> scenarios = generator.generate("Session Management");
        String all = String.join("\n", scenarios);
        assertThat(all).contains("degenerate");
        assertThat(all).contains("happy");
        assertThat(all).contains("error");
        assertThat(all).contains("boundary");
    }

    @Test
    void generate_nullFeatureName_throwsIllegalArgument() {
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generate_blankFeatureName_throwsIllegalArgument() {
        assertThatThrownBy(() -> generator.generate("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
