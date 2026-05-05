package dev.iadev.adapter.outbound.documentation;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;

final class C4PlaceholderGenerator {

    C4Diagram generate(C4Level level, String entityId, C4OutputFormat format) {
        String title = "Placeholder — " + entityId + " (" + level.name() + ")";
        String content = switch (format) {
            case PLANTUML -> "@startuml\ntitle " + title + "\nnote as N1\n  Diagram not yet generated.\nend note\n@enduml";
            case MERMAID -> "graph TD\n    N1[\"" + title + "\"]";
        };
        return new C4Diagram(title, level, format, content);
    }
}
