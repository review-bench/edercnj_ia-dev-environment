package dev.iadev.adapter.outbound.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4ContainerRenderer")
class C4ContainerRendererTest {

    private final C4Diagram containerDiagram =
            new C4Diagram(
                    "product-0001 — C4 Container",
                    C4Level.CONTAINER,
                    C4OutputFormat.MERMAID,
                    "C4Container\n  title test");

    @Test
    void render_returnsContent() {
        assertThat(C4ContainerRenderer.render(containerDiagram)).contains("C4Container");
    }

    @Test
    void renderHeader_containsLevelAndFormat() {
        String header = C4ContainerRenderer.renderHeader(containerDiagram);
        assertThat(header).contains("**Level:** Container");
    }

    @Test
    void render_wrongLevel_throws() {
        var context = new C4Diagram("t", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  x");
        assertThatThrownBy(() -> C4ContainerRenderer.render(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONTAINER");
    }
}
