package dev.iadev.domain.capability;

import java.util.Objects;

public record ResolutionWarning(ResolutionWarning.Kind kind, String message, String context) {

    public enum Kind {
        UNKNOWN_CAPABILITY,
        ASYMMETRIC_MUTEX,
        DEPRECATED_CAPABILITY,
        CYCLIC_DEPENDENCY_SUSPECT,
        GLOB_MATCHED_ZERO
    }

    public ResolutionWarning {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(message, "message must not be null");
        context = context == null ? "" : context;
    }

    public static ResolutionWarning of(Kind kind, String message) {
        return new ResolutionWarning(kind, message, "");
    }

    @Override
    public String toString() {
        return "[" + kind + "] " + message + (context.isEmpty() ? "" : " (context: " + context + ")");
    }
}
