package dev.iadev.domain.architecture;

import java.util.List;

public record CapabilityC4Model(
        String capabilityId,
        C4OutputFormat format,
        C4Diagram contextDiagram,
        C4Diagram containerDiagram,
        C4Diagram componentDiagram,
        List<String> placeholders) {

    public CapabilityC4Model {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be blank");
        }
        if (format == null) throw new IllegalArgumentException("format must not be null");
        if (contextDiagram == null) throw new IllegalArgumentException("contextDiagram must not be null");
        if (containerDiagram == null) throw new IllegalArgumentException("containerDiagram must not be null");
        if (componentDiagram == null) throw new IllegalArgumentException("componentDiagram must not be null");
        placeholders = List.copyOf(placeholders);
    }

    public boolean isPlaceholder(String level) {
        return placeholders.contains(level);
    }
}
