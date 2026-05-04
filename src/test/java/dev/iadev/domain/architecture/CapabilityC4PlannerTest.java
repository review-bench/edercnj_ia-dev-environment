package dev.iadev.domain.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CapabilityC4Planner")
class CapabilityC4PlannerTest {

    private final CapabilityC4Planner planner = new CapabilityC4Planner();

    @Test
    void planContainer_returnsDiagramWithContainerLevel() {
        C4Diagram diagram = planner.planContainer("capability-auth", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTAINER);
        assertThat(diagram.format()).isEqualTo(C4OutputFormat.MERMAID);
        assertThat(diagram.title()).contains("capability-auth");
        assertThat(diagram.content()).contains("C4Container");
    }

    @Test
    void planComponent_returnsDiagramWithComponentLevel() {
        C4Diagram diagram = planner.planComponent("capability-auth", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.COMPONENT);
        assertThat(diagram.content()).contains("C4Component");
    }

    @Test
    void planContainer_plantuml_returnsPlantUmlContent() {
        C4Diagram diagram = planner.planContainer("capability-payment", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).contains("@startuml");
        assertThat(diagram.content()).contains("@enduml");
        assertThat(diagram.content()).contains("capability-payment");
    }

    @Test
    void planComponent_plantuml_returnsPlantUmlContent() {
        C4Diagram diagram = planner.planComponent("capability-payment", C4OutputFormat.PLANTUML);

        assertThat(diagram.content()).contains("@startuml");
        assertThat(diagram.content()).contains("Component(domain");
    }

    @Test
    void planContainer_nullCapabilityId_throws() {
        assertThatThrownBy(() -> planner.planContainer(null, C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capabilityId");
    }

    @Test
    void planComponent_blankCapabilityId_throws() {
        assertThatThrownBy(() -> planner.planComponent("", C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
