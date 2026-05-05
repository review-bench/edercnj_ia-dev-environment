package dev.iadev.application.quality;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.quality.PhaseGateC4Validator.PhaseGateResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecuteC4PhaseGateUseCaseTest {

    private final ExecuteC4PhaseGateUseCase useCase = new ExecuteC4PhaseGateUseCase();

    @Test
    void execute_validDiagram_passes() {
        C4Diagram ctx =
                new C4Diagram(
                        "ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        PhaseGateResult result = useCase.execute(List.of(ctx), null, null);
        assertTrue(result.passed());
    }

    @Test
    void execute_nullDiagrams_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null));
    }

    @Test
    void execute_validCodeDiagram_passes() {
        C4Diagram code =
                new C4Diagram(
                        "code", C4Level.CODE, C4OutputFormat.MERMAID, "classDiagram\n  class A");
        List<CodeEntry> classes =
                List.of(new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        PhaseGateResult result = useCase.execute(List.of(code), classes, List.of());
        assertTrue(result.passed());
    }

    @Test
    void execute_multipleDiagrams_aggregatesViolations() {
        C4Diagram ctx =
                new C4Diagram(
                        "ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        C4Diagram container =
                new C4Diagram(
                        "ctr", C4Level.CONTAINER, C4OutputFormat.MERMAID, "C4Container\n  title c");
        PhaseGateResult result = useCase.execute(List.of(ctx, container), null, null);
        assertTrue(result.passed());
    }
}
