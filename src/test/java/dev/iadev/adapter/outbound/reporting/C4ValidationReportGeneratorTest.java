package dev.iadev.adapter.outbound.reporting;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.quality.PhaseGateC4Validator;
import dev.iadev.domain.quality.PhaseGateC4Validator.PhaseGateResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class C4ValidationReportGeneratorTest {

    private final C4ValidationReportGenerator generator = new C4ValidationReportGenerator();
    private final PhaseGateC4Validator validator = new PhaseGateC4Validator();

    @Test
    void generate_passedResult_containsPassed() {
        C4Diagram ctx =
                new C4Diagram(
                        "ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        PhaseGateResult result = validator.validate(List.of(ctx), null, null);
        String report = generator.generate(result);
        assertTrue(report.contains("PASSED"));
        assertFalse(report.contains("FAILED"));
    }

    @Test
    void generate_failedResult_containsFailedAndViolations() {
        C4Diagram code =
                new C4Diagram(
                        "code", C4Level.CODE, C4OutputFormat.MERMAID, "classDiagram\n  class A");
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "Adapter",
                                "adapter",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        PhaseGateResult result = validator.validate(List.of(code), classes, deps);
        String report = generator.generate(result);
        assertTrue(report.contains("FAILED"));
        assertTrue(report.contains("Violations") || report.contains("OUTWARD_DEPENDENCY"));
    }

    @Test
    void generate_nullResult_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(null));
    }

    @Test
    void generate_passedResult_hasHeader() {
        C4Diagram ctx =
                new C4Diagram(
                        "ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        PhaseGateResult result = validator.validate(List.of(ctx), null, null);
        String report = generator.generate(result);
        assertTrue(report.startsWith("# C4 Phase Gate Report"));
    }
}
