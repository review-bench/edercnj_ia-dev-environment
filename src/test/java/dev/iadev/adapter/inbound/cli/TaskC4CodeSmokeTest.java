package dev.iadev.adapter.inbound.cli;

import dev.iadev.adapter.outbound.documentation.C4CodeRenderer;
import dev.iadev.domain.architecture.C4CodeLevelValidator;
import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.ValidationResult;
import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.TaskC4CodePlanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskC4Code Smoke")
class TaskC4CodeSmokeTest {

    private final C4CodeLevelValidator validator = new C4CodeLevelValidator();
    private final TaskC4CodePlanner planner = new TaskC4CodePlanner();

    private final List<CodeEntry> hexagonalClasses = List.of(
            new CodeEntry("C4CodeLevelValidator", "domain.architecture", LayerType.DOMAIN, ClassType.SERVICE),
            new CodeEntry("TaskC4CodePlanner", "domain.architecture", LayerType.DOMAIN, ClassType.SERVICE),
            new CodeEntry("C4CodeRenderer", "adapter.outbound.documentation", LayerType.ADAPTER_OUTBOUND, ClassType.RENDERER));

    private final List<Dependency> validDeps = List.of(
            new Dependency("C4CodeRenderer", "C4CodeLevelValidator"),
            new Dependency("C4CodeRenderer", "TaskC4CodePlanner"));

    @Test
    void mermaid_endToEnd_validatesAndGeneratesAndRendersCodeDiagram() {
        ValidationResult result = validator.validate(hexagonalClasses, validDeps);
        assertTrue(result.valid(), "Classes should be valid hexagonal architecture");

        C4Diagram diagram = planner.planCode("TASK-0077-0015-001", hexagonalClasses, validDeps, C4OutputFormat.MERMAID);
        assertEquals(C4Diagram.C4Level.CODE, diagram.level());
        assertTrue(diagram.content().startsWith("classDiagram"));

        String rendered = C4CodeRenderer.renderHeader(diagram);
        assertTrue(rendered.contains("**Level:** Code"));
        assertTrue(rendered.contains("```mermaid"));
    }

    @Test
    void plantuml_endToEnd_validatesAndGeneratesAndRendersCodeDiagram() {
        ValidationResult result = validator.validate(hexagonalClasses, validDeps);
        assertTrue(result.valid());

        C4Diagram diagram = planner.planCode("TASK-0077-0015-002", hexagonalClasses, validDeps, C4OutputFormat.PLANTUML);
        assertTrue(diagram.content().startsWith("@startuml"));

        String rendered = C4CodeRenderer.renderHeader(diagram);
        assertTrue(rendered.contains("```plantuml"));
    }

    @Test
    void validator_rejectsOutwardDependencyFromDomainToAdapter() {
        List<Dependency> outward = List.of(
                new Dependency("C4CodeLevelValidator", "C4CodeRenderer"));
        ValidationResult result = validator.validate(hexagonalClasses, outward);
        assertFalse(result.valid());
        assertFalse(result.violations().isEmpty());
    }

    @Test
    void defaultFormat_MERMAID_producesClassDiagram() {
        C4Diagram diagram = planner.planCode("TASK-DEFAULT", hexagonalClasses, null, C4OutputFormat.MERMAID);
        assertTrue(diagram.content().contains("classDiagram"));
        assertFalse(diagram.content().contains("@startuml"));
    }

    @Test
    void htmlEscaping_inTaskId_preventsXSS() {
        C4Diagram diagram = planner.planCode("TASK-<xss>", hexagonalClasses, null, C4OutputFormat.MERMAID);
        assertFalse(diagram.content().contains("<xss>"), "Raw < should be escaped in diagram content");
        assertTrue(diagram.content().contains("&lt;xss&gt;"));
    }

    @Test
    void renderer_render_returnsRawContent() {
        C4Diagram diagram = planner.planCode("TASK-RAW", hexagonalClasses, null, C4OutputFormat.MERMAID);
        String raw = C4CodeRenderer.render(diagram);
        assertEquals(diagram.content(), raw);
    }
}
