package dev.iadevkit.audit;

import java.util.Objects;

public record AuditTarget(String id, String content) {

    public AuditTarget {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(content, "content must not be null");
    }
}
