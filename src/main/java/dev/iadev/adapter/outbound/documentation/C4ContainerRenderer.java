package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class C4ContainerRenderer {

    private C4ContainerRenderer() {}

    public static String render(C4Diagram diagram) {
        if (diagram.level() != C4Level.CONTAINER) {
            throw new IllegalArgumentException("Expected CONTAINER diagram, got: " + diagram.level());
        }
        return diagram.content();
    }

    public static String renderHeader(C4Diagram diagram) {
        return "# " + escapeMarkdown(diagram.title()) + "\n\n"
                + "**Level:** Container\n"
                + "**Format:** " + diagram.format().name().toLowerCase() + "\n\n"
                + "```\n" + diagram.content() + "\n```";
    }

    private static String escapeMarkdown(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
