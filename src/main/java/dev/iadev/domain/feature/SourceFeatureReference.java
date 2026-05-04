package dev.iadev.domain.feature;

public record SourceFeatureReference(String featureId, String sourceFeatureLink) {

    private static final String NOT_APPLICABLE_ID = "N/A";
    private static final String NOT_APPLICABLE_LINK = "—";

    public SourceFeatureReference {
        if (featureId == null || featureId.isBlank()) {
            throw new IllegalArgumentException("featureId must not be null or blank");
        }
    }

    public static SourceFeatureReference notApplicable() {
        return new SourceFeatureReference(NOT_APPLICABLE_ID, NOT_APPLICABLE_LINK);
    }

    public static SourceFeatureReference of(String featureId, String sourceFeatureLink) {
        return new SourceFeatureReference(featureId, sourceFeatureLink == null ? NOT_APPLICABLE_LINK : sourceFeatureLink);
    }

    public boolean isLinked() {
        return !NOT_APPLICABLE_ID.equals(featureId);
    }
}
