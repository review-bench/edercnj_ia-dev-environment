package dev.iadev.domain.feature;

import java.util.List;

public record CapabilityFeatureDecomposition(String capabilityId, List<String> featureNames) {

    private static final int MIN_FEATURES = 4;
    private static final int MAX_FEATURES = 8;

    public CapabilityFeatureDecomposition {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be null or blank");
        }
        if (featureNames == null) {
            throw new IllegalArgumentException("featureNames must not be null");
        }
        if (featureNames.size() < MIN_FEATURES) {
            throw new IllegalArgumentException(
                    "featureNames requires at least "
                            + MIN_FEATURES
                            + " entries; got "
                            + featureNames.size());
        }
        if (featureNames.size() > MAX_FEATURES) {
            throw new IllegalArgumentException(
                    "featureNames allows at most "
                            + MAX_FEATURES
                            + " entries; got "
                            + featureNames.size());
        }
        featureNames = List.copyOf(featureNames);
    }
}
