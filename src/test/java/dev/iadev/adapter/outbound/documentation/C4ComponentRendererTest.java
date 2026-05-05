package dev.iadev.adapter.outbound.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4ComponentRenderer")
class C4ComponentRendererTest {

    private final C4Diagram componentDiagram =
            new C4Diagram(
                    "capability-auth — C4 Component",
                    C4Level.COMPONENT,
                    C4OutputFormat.PLANTUML,
                    "@startuml\nComponent(x)\n@enduml");

    @Test
    void render_returnsContent() {
        assertThat(C4ComponentRenderer.render(componentDiagram)).contains("@startuml");
    }

    @Test
    void renderHeader_containsLevelAndFormat() {
        String header = C4ComponentRenderer.renderHeader(componentDiagram);
        assertThat(header).contains("**Level:** Component");
        assertThat(header).contains("**Format:** plantuml");
    }

    @Test
    void render_wrongLevel_throws() {
        var container =
                new C4Diagram("t", C4Level.CONTAINER, C4OutputFormat.MERMAID, "C4Container\n  x");
        assertThatThrownBy(() -> C4ComponentRenderer.render(container))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COMPONENT");
    }
}
