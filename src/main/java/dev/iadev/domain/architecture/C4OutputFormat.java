package dev.iadev.domain.architecture;

public enum C4OutputFormat {
    MERMAID,
    PLANTUML;

    public static C4OutputFormat fromString(String value) {
        if (value == null) return MERMAID;
        return switch (value.toLowerCase()) {
            case "mmd", "mermaid" -> MERMAID;
            case "plantuml", "puml" -> PLANTUML;
            default -> throw new IllegalArgumentException("Unsupported format: " + value);
        };
    }
}
