package dev.iadev.adapter.inbound.cli;

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

class XInternalC4ValidateCommandTest {

    private final XInternalC4ValidateCommand command = new XInternalC4ValidateCommand();

    @Test
    void execute_validDiagram_returnsOk() {
        C4Diagram diagram =
                new C4Diagram("T", C4Level.CODE, C4OutputFormat.MERMAID, "classDiagram\n  class A");
        List<CodeEntry> classes =
                List.of(new CodeEntry("A", "domain", LayerType.DOMAIN, ClassType.ENTITY));
        IntegrityResult result = command.execute(diagram, classes, List.of());
        assertTrue(result.valid());
    }

    @Test
    void execute_nullDiagram_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> command.execute(null, null, null));
    }

    @Test
    void format_validResult_returnsOkMessage() {
        C4Diagram diagram =
                new C4Diagram("T", C4Level.CONTEXT, C4OutputFormat.MERMAID, "C4Context\n  title t");
        IntegrityResult result = command.execute(diagram, null, null);
        String output = command.format(result);
        assertEquals("OK: diagram is valid", output);
    }

    @Test
    void format_invalidResult_containsViolations() {
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
        IntegrityResult result = command.execute(diagram, classes, deps);
        String output = command.format(result);
        assertTrue(output.contains("VIOLATIONS:"));
        assertTrue(output.contains("OUTWARD_DEPENDENCY"));
    }
}
