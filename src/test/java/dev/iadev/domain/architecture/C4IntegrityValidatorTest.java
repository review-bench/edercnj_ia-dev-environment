package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;
import dev.iadev.domain.architecture.C4IntegrityValidator.Severity;
import dev.iadev.domain.architecture.C4IntegrityValidator.ViolationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class C4IntegrityValidatorTest {

    private final C4IntegrityValidator validator = new C4IntegrityValidator();

    @Test
    void validate_codeDiagramWithValidHexagonal_returnsOk() {
        C4Diagram diagram = new C4Diagram("T — C4 Code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        IntegrityResult result = validator.validate(diagram, classes, List.of());
        assertTrue(result.valid());
    }

    @Test
    void validate_codeDiagramWithOutwardDep_returnsError() {
        C4Diagram diagram = new C4Diagram("T — C4 Code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("Adapter", "adapter", LayerType.ADAPTER_OUTBOUND, ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        IntegrityResult result = validator.validate(diagram, classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.severity() == Severity.ERROR));
        assertTrue(result.violations().stream().anyMatch(v -> v.type() == ViolationType.OUTWARD_DEPENDENCY));
    }

    @Test
    void validate_nonCodeDiagramWithContent_returnsOk() {
        C4Diagram diagram = new C4Diagram("ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID,
                "C4Context\n  title test");
        IntegrityResult result = validator.validate(diagram, null, null);
        assertTrue(result.valid());
    }

    @Test
    void validate_nullDiagram_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(null, null, null));
    }
}
