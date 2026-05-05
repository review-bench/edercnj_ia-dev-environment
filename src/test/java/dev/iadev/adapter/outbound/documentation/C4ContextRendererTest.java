package dev.iadev.adapter.outbound.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4ContextRenderer")
class C4ContextRendererTest {

    private final C4Diagram contextDiagram =
            new C4Diagram(
                    "product-0001 — C4 Context",
                    C4Level.CONTEXT,
                    C4OutputFormat.MERMAID,
                    "C4Context\n  title test");

    @Test
    void render_returnsContent() {
        assertThat(C4ContextRenderer.render(contextDiagram)).contains("C4Context");
    }

    @Test
    void renderHeader_containsLevelAndFormat() {
        String header = C4ContextRenderer.renderHeader(contextDiagram);
        assertThat(header).contains("**Level:** Context");
        assertThat(header).contains("**Format:** mermaid");
    }

    @Test
    void render_wrongLevel_throws() {
        var container =
                new C4Diagram("t", C4Level.CONTAINER, C4OutputFormat.MERMAID, "C4Container\n  x");
        assertThatThrownBy(() -> C4ContextRenderer.render(container))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CONTEXT");
    }
}
