package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;

public final class C4CodeRenderer {

    public static String render(C4Diagram diagram) {
        if (diagram.level() != C4Level.CODE) {
            throw new IllegalArgumentException("Expected CODE diagram, got: " + diagram.level());
        }
        return diagram.content();
    }

    public static String renderHeader(C4Diagram diagram) {
        if (diagram.level() != C4Level.CODE) {
            throw new IllegalArgumentException("Expected CODE diagram, got: " + diagram.level());
        }
        String fenceLabel = diagram.format() == C4OutputFormat.PLANTUML ? "plantuml" : "mermaid";
        return "# "
                + escapeMarkdown(diagram.title())
                + "\n"
                + "**Level:** Code\n"
                + "**Format:** "
                + diagram.format().name()
                + "\n"
                + "```"
                + fenceLabel
                + "\n"
                + diagram.content()
                + "\n"
                + "```";
    }

    private static String escapeMarkdown(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
