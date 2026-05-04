package dev.iadev.domain.feature;

import java.util.List;

public final class AutoDecomposeFeatureHeuristic {

    private static final List<String> DEFAULT_FEATURES = List.of(
            "basic-flow", "validation", "error-handling", "integration", "reporting");

    public List<String> decompose(String capabilityId) {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be null or blank");
        }
        return DEFAULT_FEATURES;
    }
}
