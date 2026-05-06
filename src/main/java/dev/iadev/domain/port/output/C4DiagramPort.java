package dev.iadev.domain.port.output;

import dev.iadev.domain.architecture.C4Diagram;
import dev.iadev.domain.architecture.C4Diagram.C4Level;
import dev.iadev.domain.architecture.C4OutputFormat;

public interface C4DiagramPort {

    C4Diagram generate(C4Level level, String entityId, C4OutputFormat format);

    C4Diagram generatePlaceholder(C4Level level, String entityId, C4OutputFormat format);
}
