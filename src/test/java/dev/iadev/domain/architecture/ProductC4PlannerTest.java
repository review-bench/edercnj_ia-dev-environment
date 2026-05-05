package dev.iadev.domain.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProductC4Planner")
class ProductC4PlannerTest {

    private final ProductC4Planner planner = new ProductC4Planner();

    @Test
    void planContext_returnsDiagramWithContextLevel() {
        C4Diagram diagram = planner.planContext("product-0001", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTEXT);
        assertThat(diagram.format()).isEqualTo(C4OutputFormat.MERMAID);
        assertThat(diagram.title()).contains("product-0001");
        assertThat(diagram.content()).contains("C4Context");
    }

    @Test
    void planContainer_returnsDiagramWithContainerLevel() {
        C4Diagram diagram = planner.planContainer("product-0001", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(diagram.content()).contains("C4Container");
    }

    @Test
    void planContext_plantuml_returnsPlantUmlContent() {
        C4Diagram diagram = planner.planContext("product-0002", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).contains("@startuml");
        assertThat(diagram.content()).contains("@enduml");
        assertThat(diagram.content()).contains("product-0002");
    }

    @Test
    void planContainer_plantuml_returnsPlantUmlContent() {
        C4Diagram diagram = planner.planContainer("product-0002", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).contains("@startuml");
        assertThat(diagram.content()).contains("Container(api");
    }

    @Test
    void planContext_plantumlDirectiveInput_isNeutralized() {
        C4Diagram diagram = planner.planContext("@startjson", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).doesNotContain("@startjson");
        assertThat(diagram.content()).contains("startjson");
    }

    @Test
    void planContext_nullProductId_throws() {
        assertThatThrownBy(() -> planner.planContext(null, C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("productId");
    }

    @Test
    void planContext_blankProductId_throws() {
        assertThatThrownBy(() -> planner.planContext("   ", C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
