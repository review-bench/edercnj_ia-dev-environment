package dev.iadev.adapter.outbound.documentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("C4DiagramGenerator")
class C4DiagramGeneratorTest {

    private final C4DiagramGenerator generator = new C4DiagramGenerator();

    @Test
    void generate_whenProductContext_returnsContextDiagram() {
        var diagram = generator.generate(C4Level.CONTEXT, "product-0001", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.CONTEXT);
        assertThat(diagram.content()).contains("C4Context");
    }

    @Test
    void generate_whenCapabilityComponent_returnsComponentDiagram() {
        var diagram =
                generator.generate(C4Level.COMPONENT, "capability-auth", C4OutputFormat.MERMAID);

        assertThat(diagram.level()).isEqualTo(C4Level.COMPONENT);
        assertThat(diagram.content()).contains("C4Component");
    }

    @Test
    void generatePlaceholder_whenComponentRequested_containsRefinementHint() {
        var diagram =
                generator.generatePlaceholder(
                        C4Level.COMPONENT, "product-0001", C4OutputFormat.MERMAID);

        assertThat(diagram.content()).contains("refine in story-0077-0015");
    }

    @Test
    void generate_whenEntityPrefixUnknown_throws() {
        assertThatThrownBy(
                        () ->
                                generator.generate(
                                        C4Level.CONTEXT, "unknown-1", C4OutputFormat.MERMAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entityId");
    }
}
