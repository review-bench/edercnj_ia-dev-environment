package dev.iadev.domain.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FeatureC4Planner")
class FeatureC4PlannerTest {

    private final FeatureC4Planner planner = new FeatureC4Planner();

    @Test
    void planContext_returnsDiagramWithContextLevel() {
        C4Diagram diagram = planner.planContext("feature-oauth2", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTEXT);
        assertThat(diagram.content()).contains("C4Context").contains("OAuth2 Provider");
    }

    @Test
    void planContainer_returnsDiagramWithContainerLevel() {
        C4Diagram diagram = planner.planContainer("feature-oauth2", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(diagram.content()).contains("auth-service").contains("cache");
    }

    @Test
    void planContext_plantumlDirectiveInput_isNeutralized() {
        C4Diagram diagram = planner.planContext("@startjson", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).doesNotContain("@startjson");
        assertThat(diagram.content()).contains("startjson");
    }

    @Test
    void planContainer_blankFeatureId_throws() {
        assertThatThrownBy(() -> planner.planContainer(" ", C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("featureId");
    }
}
