package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;

public final class C4ComponentRenderer {

    private C4ComponentRenderer() {}

    public static String render(C4Diagram diagram) {
        if (diagram.level() != C4Level.COMPONENT) {
            throw new IllegalArgumentException(
                    "Expected COMPONENT diagram, got: " + diagram.level());
        }
        return diagram.content();
    }

    public static String renderHeader(C4Diagram diagram) {
        return "# "
                + escapeMarkdown(diagram.title())
                + "\n\n"
                + "**Level:** Component\n"
                + "**Format:** "
                + diagram.format().name().toLowerCase()
                + "\n\n"
                + "```\n"
                + diagram.content()
                + "\n```";
    }

    private static String escapeMarkdown(String text) {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
}
