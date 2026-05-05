package dev.iadev.application.feature;

import java.util.List;

public record FeatureEpicSource(
        String title,
        String featureId,
        String capabilityId,
        String sourceFeatureLink,
        List<String> inScope,
        List<String> outOfScope,
        List<String> storyTitles,
        List<String> references,
        List<InheritedRnfLine> inheritedRnfs) {

    public FeatureEpicSource {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be null or blank");
        }
        if (featureId == null || featureId.isBlank()) {
            throw new IllegalArgumentException("featureId must not be null or blank");
        }
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be null or blank");
        }
        sourceFeatureLink = sourceFeatureLink == null || sourceFeatureLink.isBlank() ? "—" : sourceFeatureLink;
        inScope = List.copyOf(inScope);
        outOfScope = List.copyOf(outOfScope);
        storyTitles = List.copyOf(storyTitles);
        references = List.copyOf(references);
        inheritedRnfs = List.copyOf(inheritedRnfs);
    }
}
