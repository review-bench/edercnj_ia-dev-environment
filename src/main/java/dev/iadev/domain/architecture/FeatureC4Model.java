package dev.iadev.domain.architecture;

import java.util.List;

public record FeatureC4Model(
        String featureId,
        C4OutputFormat format,
        C4Diagram contextDiagram,
        C4Diagram containerDiagram,
        C4Diagram componentDiagram,
        List<String> placeholders) {

    public FeatureC4Model {
        if (featureId == null || featureId.isBlank()) {
            throw new IllegalArgumentException("featureId must not be blank");
        }
        if (format == null) {
            throw new IllegalArgumentException("format must not be null");
        }
        if (contextDiagram == null || containerDiagram == null || componentDiagram == null) {
            throw new IllegalArgumentException("feature diagrams must not be null");
        }
        placeholders = List.copyOf(placeholders);
    }

    public boolean isPlaceholder(String level) {
        return placeholders.contains(level);
    }
}
