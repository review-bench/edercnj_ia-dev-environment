package dev.iadev.application.architecture;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;
import dev.iadev.domain.architecture.C4OutputFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class ValidateC4IntegrityUseCaseTest {

    private final ValidateC4IntegrityUseCase useCase = new ValidateC4IntegrityUseCase();

    @Test
    void execute_validCodeDiagram_returnsOk() {
        C4Diagram diagram =
                new C4Diagram(
                        "T — C4 Code",
                        C4Level.CODE,
                        C4OutputFormat.MERMAID,
                        "classDiagram\n  class A");
        List<CodeEntry> classes =
                List.of(new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        IntegrityResult result = useCase.execute(diagram, classes, List.of());
        assertTrue(result.valid());
    }

    @Test
    void execute_nonCodeDiagram_returnsOk() {
        C4Diagram diagram =
                new C4Diagram(
                        "ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        IntegrityResult result = useCase.execute(diagram, null, null);
        assertTrue(result.valid());
    }

    @Test
    void execute_nullDiagram_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null, null, null));
    }

    @Test
    void execute_outwardDep_returnsInvalid() {
        C4Diagram diagram =
                new C4Diagram("T", C4Level.CODE, C4OutputFormat.MERMAID, "classDiagram\n  class A");
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "Adapter",
                                "adapter",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        IntegrityResult result = useCase.execute(diagram, classes, deps);
        assertFalse(result.valid());
    }
}
