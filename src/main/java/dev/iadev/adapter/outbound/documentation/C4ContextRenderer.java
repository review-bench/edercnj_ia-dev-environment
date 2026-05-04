package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class C4ContextRenderer {

    private C4ContextRenderer() {}

    public static String render(C4Diagram diagram) {
        if (diagram.level() != C4Level.CONTEXT) {
            throw new IllegalArgumentException("Expected CONTEXT diagram, got: " + diagram.level());
        }
        return diagram.content();
    }

    public static String renderHeader(C4Diagram diagram) {
        return "# " + escapeMarkdown(diagram.title()) + "\n\n"
                + "**Level:** Context\n"
                + "**Format:** " + diagram.format().name().toLowerCase() + "\n\n"
                + "```\n" + diagram.content() + "\n```";
    }

    private static String escapeMarkdown(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
