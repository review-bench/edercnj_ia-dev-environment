package dev.iadev.adapter.outbound.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4CodeRenderer")
class C4CodeRendererTest {

    private final C4Diagram codeDiagram =
            new C4Diagram(
                    "TASK-X — C4 Code",
                    C4Level.CODE,
                    C4OutputFormat.MERMAID,
                    "classDiagram\n  class MyEntity { <<ENTITY>> }");

    @Test
    void render_returnsContent() {
        assertThat(C4CodeRenderer.render(codeDiagram)).contains("classDiagram");
    }

    @Test
    void renderHeader_containsLevelAndFormat() {
        String header = C4CodeRenderer.renderHeader(codeDiagram);
        assertThat(header).contains("**Level:** Code");
        assertThat(header).contains("**Format:** MERMAID");
        assertThat(header).contains("```mermaid");
    }

    @Test
    void render_wrongLevel_throwsException() {
        var container =
                new C4Diagram("t", C4Level.CONTAINER, C4OutputFormat.MERMAID, "C4Container\n  x");
        assertThatThrownBy(() -> C4CodeRenderer.render(container))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CODE");
    }

    @Test
    void renderHeader_plantuml_usesPlantumlFence() {
        var plantuml =
                new C4Diagram(
                        "TASK-Y — C4 Code",
                        C4Level.CODE,
                        C4OutputFormat.PLANTUML,
                        "@startuml\nclass X\n@enduml");
        String header = C4CodeRenderer.renderHeader(plantuml);
        assertThat(header).contains("```plantuml");
    }
}
