package dev.iadevkit.audit;

import java.util.Objects;

public record AuditViolation(String targetId, String ruleCode, String message) {

    public AuditViolation {
        Objects.requireNonNull(targetId, "targetId must not be null");
        Objects.requireNonNull(ruleCode, "ruleCode must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
