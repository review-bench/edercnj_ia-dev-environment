package dev.iadev.domain.architecture;

import static org.junit.jupiter.api.Assertions.*;

import dev.iadev.domain.architecture.C4CodeLevelValidator.ClassType;
import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import java.util.List;
import org.junit.jupiter.api.Test;

class TaskC4CodePlannerTest {

    private final TaskC4CodePlanner planner = new TaskC4CodePlanner();

    private final List<CodeEntry> sampleClasses =
            List.of(
                    new CodeEntry("MyEntity", "domain.model", LayerType.DOMAIN, ClassType.ENTITY),
                    new CodeEntry(
                            "MyUseCase", "application", LayerType.APPLICATION, ClassType.USE_CASE));

    @Test
    void planCode_mermaid_returnsClassDiagram() {
        C4Diagram diagram =
                planner.planCode("TASK-0077-0015-001", sampleClasses, null, C4OutputFormat.MERMAID);
        assertTrue(diagram.content().startsWith("classDiagram"));
        assertTrue(diagram.content().contains("DOMAIN"));
    }

    @Test
    void planCode_plantuml_returnsPackageDiagram() {
        C4Diagram diagram =
                planner.planCode(
                        "TASK-0077-0015-001", sampleClasses, null, C4OutputFormat.PLANTUML);
        assertTrue(diagram.content().startsWith("@startuml"));
        assertTrue(diagram.content().endsWith("@enduml"));
        assertTrue(diagram.content().contains("DOMAIN"));
    }

    @Test
    void planCode_levelIsCode() {
        C4Diagram diagram = planner.planCode("TASK-X", sampleClasses, null, C4OutputFormat.MERMAID);
        assertEquals(C4Level.CODE, diagram.level());
    }

    @Test
    void planCode_titleContainsTaskId() {
        C4Diagram diagram = planner.planCode("TASK-X", sampleClasses, null, C4OutputFormat.MERMAID);
        assertTrue(diagram.title().contains("TASK-X"));
    }

    @Test
    void planCode_nullTaskId_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.planCode(null, sampleClasses, null, C4OutputFormat.MERMAID));
    }

    @Test
    void planCode_emptyClasses_throwsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> planner.planCode("TASK-X", List.of(), null, C4OutputFormat.MERMAID));
    }

    @Test
    void planCode_withDependencies_includesDependencyArrow() {
        List<Dependency> deps = List.of(new Dependency("MyUseCase", "MyEntity"));
        C4Diagram diagram = planner.planCode("TASK-X", sampleClasses, deps, C4OutputFormat.MERMAID);
        assertTrue(diagram.content().contains("MyUseCase --> MyEntity"));
    }

    @Test
    void planCode_htmlEscapingInTaskId() {
        C4Diagram diagram =
                planner.planCode("TASK-<xss>", sampleClasses, null, C4OutputFormat.MERMAID);
        assertFalse(diagram.content().contains("<xss>"));
        assertTrue(diagram.content().contains("&lt;xss&gt;"));
    }
}
