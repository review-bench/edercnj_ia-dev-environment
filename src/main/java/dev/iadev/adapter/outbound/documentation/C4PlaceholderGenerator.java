package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;
import dev.iadev.domain.architecture.C4TextSanitizer;

public final class C4PlaceholderGenerator {

    public C4Diagram generate(C4Level level, String entityId, C4OutputFormat format) {
        String safeEntityId = C4TextSanitizer.sanitize(entityId);
        String title = "Placeholder " + level + " — " + safeEntityId;
        String content = format == C4OutputFormat.PLANTUML
                ? plantUmlContent(level, safeEntityId)
                : mermaidContent(level, safeEntityId);
        return new C4Diagram(title, level, format, content);
    }

    private String mermaidContent(C4Level level, String entityId) {
        return levelKeyword(level)
                + "\n  title " + titleFor(level, entityId)
                + "\n  %% TBD — refine in story-0077-0015";
    }

    private String plantUmlContent(C4Level level, String entityId) {
        return "@startuml\n"
                + "title " + titleFor(level, entityId) + "\n"
                + "note as N1\n"
                + "  TBD — refine in story-0077-0015\n"
                + "end note\n"
                + "@enduml";
    }

    private String levelKeyword(C4Level level) {
        return switch (level) {
            case CONTEXT -> "C4Context";
            case CONTAINER -> "C4Container";
            case COMPONENT -> "C4Component";
            case CODE -> "C4Code";
        };
    }

    private String titleFor(C4Level level, String entityId) {
        return level + " TBD — " + entityId;
    }
}
