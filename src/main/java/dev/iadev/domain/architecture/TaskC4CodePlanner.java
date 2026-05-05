package dev.iadev.domain.architecture;

import dev.iadev.domain.architecture.C4CodeLevelValidator.CodeEntry;
import dev.iadev.domain.architecture.C4CodeLevelValidator.Dependency;
import dev.iadev.domain.architecture.C4CodeLevelValidator.LayerType;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import java.util.List;
import java.util.stream.Collectors;

public final class TaskC4CodePlanner {

    public C4Diagram planCode(
            String taskId,
            List<CodeEntry> classes,
            List<Dependency> dependencies,
            C4OutputFormat format) {
        if (taskId == null || taskId.isBlank())
            throw new IllegalArgumentException("taskId must not be blank");
        if (classes == null || classes.isEmpty())
            throw new IllegalArgumentException("classes must not be empty");

        String content =
                format == C4OutputFormat.PLANTUML
                        ? buildPlantuml(taskId, classes, dependencies)
                        : buildMermaid(taskId, classes, dependencies);
        return new C4Diagram(taskId + " — C4 Code", C4Level.CODE, format, content);
    }

    private String buildMermaid(String taskId, List<CodeEntry> classes, List<Dependency> deps) {
        StringBuilder sb =
                new StringBuilder("classDiagram\n")
                        .append("  %% Task: ")
                        .append(escape(taskId))
                        .append("\n");

        for (LayerType layer : LayerType.values()) {
            List<CodeEntry> layerClasses =
                    classes.stream().filter(c -> c.layer() == layer).collect(Collectors.toList());
            if (layerClasses.isEmpty()) continue;
            sb.append("  namespace ").append(layer.name()).append(" {\n");
            for (CodeEntry c : layerClasses) {
                sb.append("    class ").append(c.className()).append(" {\n");
                sb.append("      <<").append(c.classType().name()).append(">>\n");
                sb.append("    }\n");
            }
            sb.append("  }\n");
        }

        if (deps != null) {
            for (Dependency d : deps) {
                sb.append("  ")
                        .append(d.fromClass())
                        .append(" --> ")
                        .append(d.toClass())
                        .append("\n");
            }
        }

        return sb.toString().strip();
    }

    private String buildPlantuml(String taskId, List<CodeEntry> classes, List<Dependency> deps) {
        StringBuilder sb =
                new StringBuilder("@startuml\n")
                        .append("' Task: ")
                        .append(escape(taskId))
                        .append("\n");

        for (LayerType layer : LayerType.values()) {
            List<CodeEntry> layerClasses =
                    classes.stream().filter(c -> c.layer() == layer).collect(Collectors.toList());
            if (layerClasses.isEmpty()) continue;
            sb.append("package \"").append(layer.name()).append("\" {\n");
            for (CodeEntry c : layerClasses) {
                sb.append("  class ")
                        .append(c.className())
                        .append(" <<")
                        .append(c.classType().name())
                        .append(">>\n");
            }
            sb.append("}\n");
        }

        if (deps != null) {
            for (Dependency d : deps) {
                sb.append(d.fromClass()).append(" --> ").append(d.toClass()).append("\n");
            }
        }

        sb.append("@enduml");
        return sb.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
