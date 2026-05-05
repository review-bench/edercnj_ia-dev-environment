package dev.iadev.adapter.inbound.cli;

import dev.iadev.adapter.outbound.reporting.C4ValidationReportGenerator;
import dev.iadev.application.quality.ExecuteC4PhaseGateUseCase;
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

class C4PhaseGateSmokeTest {

    private final ExecuteC4PhaseGateUseCase useCase = new ExecuteC4PhaseGateUseCase();
    private final C4ValidationReportGenerator reporter = new C4ValidationReportGenerator();

    @Test
    void endToEnd_validFullStack_passes() {
        List<C4Diagram> diagrams = List.of(
                new C4Diagram("ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t"),
                new C4Diagram("ctr", C4Level.CONTAINER, C4OutputFormat.MERMAID, "C4Container\n  title c"),
                new C4Diagram("code", C4Level.CODE, C4OutputFormat.MERMAID, "classDiagram\n  class A"));
        List<CodeEntry> classes = List.of(
                new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        PhaseGateResult result = useCase.execute(diagrams, classes, List.of());
        assertTrue(result.passed());
        String report = reporter.generate(result);
        assertTrue(report.contains("PASSED"));
    }

    @Test
    void endToEnd_outwardDep_blocked() {
        C4Diagram code = new C4Diagram("code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("Domain", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("Adapter", "adapter", LayerType.ADAPTER_OUTBOUND, ClassType.RENDERER));
        List<Dependency> deps = List.of(new Dependency("Domain", "Adapter"));
        PhaseGateResult result = useCase.execute(List.of(code), classes, deps);
        assertFalse(result.passed());
        String report = reporter.generate(result);
        assertTrue(report.contains("FAILED"));
        assertTrue(report.contains("OUTWARD_DEPENDENCY"));
    }

    @Test
    void endToEnd_multipleDiagramsAllValid_allPassed() {
        List<C4Diagram> diagrams = List.of(
                new C4Diagram("ctx", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t"),
                new C4Diagram("comp", C4Level.COMPONENT, C4OutputFormat.MERMAID,
                        "C4Component\n  title c"));
        PhaseGateResult result = useCase.execute(diagrams, null, null);
        assertTrue(result.passed());
    }

    @Test
    void report_failedGate_containsViolationDetails() {
        C4Diagram code = new C4Diagram("code", C4Level.CODE, C4OutputFormat.MERMAID,
                "classDiagram\n  class A");
        List<CodeEntry> classes = List.of(
                new CodeEntry("D", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                new CodeEntry("R", "outbound", LayerType.ADAPTER_OUTBOUND, ClassType.REPOSITORY));
        List<Dependency> deps = List.of(new Dependency("D", "R"));
        PhaseGateResult result = useCase.execute(List.of(code), classes, deps);
        String report = reporter.generate(result);
        assertTrue(report.contains("ERROR"));
        assertTrue(report.contains("Violations") || report.contains("-"));
    }
}
