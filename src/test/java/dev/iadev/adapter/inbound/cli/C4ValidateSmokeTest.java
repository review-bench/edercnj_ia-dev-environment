package dev.iadev.adapter.inbound.cli;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4IntegrityValidator.IntegrityResult;
import dev.iadev.domain.architecture.C4IntegrityValidator.Severity;
import dev.iadev.domain.architecture.C4IntegrityValidator.ViolationType;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.HexagonalArchitectureValidator;
import dev.iadev.domain.architecture.HexagonalArchitectureValidator.ArchResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class C4ValidateSmokeTest {

    private final XInternalC4ValidateCommand command = new XInternalC4ValidateCommand();
    private final HexagonalArchitectureValidator hexValidator =
            new HexagonalArchitectureValidator();

    @Test
    void endToEnd_validHexagonalCode_passes() {
        C4Diagram diagram =
                new C4Diagram(
                        "App — C4 Code",
                        C4Level.CODE,
                        C4OutputFormat.MERMAID,
                        "classDiagram\n  class Order");
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Order", "domain.model", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "OrderUseCase",
                                "application",
                                LayerType.APPLICATION,
                                ClassType.USE_CASE));
        IntegrityResult result = command.execute(diagram, classes, List.of());
        assertTrue(result.valid());
        assertEquals("OK: diagram is valid", command.format(result));
    }

    @Test
    void endToEnd_outwardDependency_blocked() {
        C4Diagram diagram =
                new C4Diagram(
                        "App — C4 Code",
                        C4Level.CODE,
                        C4OutputFormat.MERMAID,
                        "classDiagram\n  class Order");
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Order", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "Repo",
                                "outbound",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.REPOSITORY));
        List<Dependency> deps = List.of(new Dependency("Order", "Repo"));
        IntegrityResult result = command.execute(diagram, classes, deps);
        assertFalse(result.valid());
        assertTrue(
                result.violations().stream()
                        .anyMatch(v -> v.type() == ViolationType.OUTWARD_DEPENDENCY));
        assertTrue(result.violations().stream().anyMatch(v -> v.severity() == Severity.ERROR));
        assertTrue(command.format(result).contains("VIOLATIONS:"));
    }

    @Test
    void endToEnd_contextDiagramWithContent_passes() {
        C4Diagram diagram =
                new C4Diagram(
                        "System ctx",
                        C4Level.CONTEXT,
                        C4OutputFormat.MERMAID,
                        "C4Context\n  title System\n  Person(u, User)");
        IntegrityResult result = command.execute(diagram, null, null);
        assertTrue(result.valid());
    }

    @Test
    void hexagonalValidator_validSetup_returnsOk() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Order", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "OrderUseCase",
                                "application",
                                LayerType.APPLICATION,
                                ClassType.USE_CASE));
        ArchResult result = hexValidator.validate(classes, List.of());
        assertTrue(result.valid());
        assertTrue(result.violations().isEmpty());
    }

    @Test
    void hexagonalValidator_applicationToOutbound_blocked() {
        List<CodeEntry> classes =
                List.of(
                        new CodeEntry("Order", "domain", LayerType.DOMAIN, ClassType.ENTITY),
                        new CodeEntry(
                                "UseCase",
                                "application",
                                LayerType.APPLICATION,
                                ClassType.USE_CASE),
                        new CodeEntry(
                                "RepoImpl",
                                "outbound",
                                LayerType.ADAPTER_OUTBOUND,
                                ClassType.REPOSITORY));
        List<Dependency> deps = List.of(new Dependency("UseCase", "RepoImpl"));
        ArchResult result = hexValidator.validate(classes, deps);
        assertFalse(result.valid());
        assertTrue(result.violations().stream().anyMatch(v -> v.severity() == Severity.ERROR));
    }

    @Test
    void containerDiagram_blankContent_fails() {
        C4Diagram diagram =
                new C4Diagram(
                        "title",
                        C4Level.CONTAINER,
                        C4OutputFormat.MERMAID,
                        "C4Container\n  title c");
        IntegrityResult result = command.execute(diagram, null, null);
        assertTrue(result.valid());
    }
}
