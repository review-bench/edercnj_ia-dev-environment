package dev.iadev.domain.architecture;

public record C4Diagram(String title, C4Level level, C4OutputFormat format, String content) {

    public enum C4Level {
        CONTEXT,
        CONTAINER,
        COMPONENT,
        CODE
    }

    public C4Diagram {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("title must not be blank");
        if (level == null) throw new IllegalArgumentException("level must not be null");
        if (format == null) throw new IllegalArgumentException("format must not be null");
        if (content == null || content.isBlank())
            throw new IllegalArgumentException("content must not be blank");
    }
}
