package dev.iadev.domain.feature;

import java.util.List;

public final class CapabilityToFeatureTransformer {

    public CapabilityFeatureDecomposition transform(
            String capabilityId, List<String> featureNames) {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be null or blank");
        }
        if (featureNames == null) {
            throw new IllegalArgumentException("featureNames must not be null");
        }
        return new CapabilityFeatureDecomposition(capabilityId, featureNames);
    }
}
