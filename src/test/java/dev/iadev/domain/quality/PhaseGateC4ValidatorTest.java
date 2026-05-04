package dev.iadev.domain.quality;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.quality.PhaseGateC4Validator.PhaseGateResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhaseGateC4ValidatorTest {

    private final PhaseGateC4Validator validator = new PhaseGateC4Validator();

    @Test
    void validate_validContextDiagram_passes() {
        C4Diagram ctx = new C4Diagram("ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID,
                "C4Context\n  title t");
        PhaseGateResult result = validator.validate(List.of(ctx), null, null);
        assertTrue(result.passed());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void validate_validCodeDiagram_passes() {
        C4Diagram code = new C4Diagram("code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        PhaseGateResult result = validator.validate(List.of(code), classes, List.of());
        assertTrue(result.passed());
    }

    @Test
    void validate_outwardDep_fails() {
        C4Diagram code = new C4Diagram("code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("Adapter", "adapter", LayerType.ADAPTER_OUTBOUND, ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        PhaseGateResult result = validator.validate(List.of(code), classes, deps);
        assertFalse(result.passed());
        assertFalse(result.violations().isEmpty());
    }

    @Test
    void validate_nullDiagrams_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(null, null, null));
    }

    @Test
    void validate_emptyDiagrams_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate(List.of(), null, null));
    }
}
