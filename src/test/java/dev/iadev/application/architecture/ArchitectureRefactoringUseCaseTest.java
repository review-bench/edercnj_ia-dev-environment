package dev.iadev.application.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4LevelValidator;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.port.output.C4DiagramPort;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ArchitectureRefactoringUseCase")
class ArchitectureRefactoringUseCaseTest {

    private final ArchitectureRefactoringUseCase useCase =
            new ArchitectureRefactoringUseCase(new FakeC4DiagramPort(), new C4LevelValidator());

    @Test
    void planProduct_whenComponentMissing_generatesPlaceholderAndPassesValidation() {
        var result = useCase.planProduct("product-0001", C4OutputFormat.MERMAID);

        assertThat(result.contextDiagram().level()).isEqualTo(C4Level.CONTEXT);
        assertThat(result.containerDiagram().level()).isEqualTo(C4Level.CONTAINER);
        assertThat(result.componentDiagram().level()).isEqualTo(C4Level.COMPONENT);
        assertThat(result.placeholders()).containsExactly("COMPONENT");
    }

    @Test
    void planCapability_whenContextMissing_generatesPlaceholderAndPassesValidation() {
        var result = useCase.planCapability("capability-auth", C4OutputFormat.MERMAID);

        assertThat(result.contextDiagram().level()).isEqualTo(C4Level.CONTEXT);
        assertThat(result.containerDiagram().level()).isEqualTo(C4Level.CONTAINER);
        assertThat(result.componentDiagram().level()).isEqualTo(C4Level.COMPONENT);
        assertThat(result.placeholders()).containsExactly("CONTEXT");
    }

    @Test
    void planFeature_whenComponentMissing_generatesPlaceholderAndPassesValidation() {
        var result = useCase.planFeature("feature-oauth2", C4OutputFormat.MERMAID);

        assertThat(result.contextDiagram().level()).isEqualTo(C4Level.CONTEXT);
        assertThat(result.containerDiagram().level()).isEqualTo(C4Level.CONTAINER);
        assertThat(result.componentDiagram().level()).isEqualTo(C4Level.COMPONENT);
        assertThat(result.placeholders()).containsExactly("COMPONENT");
    }

    private static final class FakeC4DiagramPort implements C4DiagramPort {

        @Override
        public C4Diagram generate(C4Level level, String entityId, C4OutputFormat format) {
            return new C4Diagram(entityId + " " + level, level, format, level + " content");
        }

        @Override
        public C4Diagram generatePlaceholder(C4Level level, String entityId, C4OutputFormat format) {
            return new C4Diagram(entityId + " " + level, level, format, "placeholder " + level);
        }
    }
}
