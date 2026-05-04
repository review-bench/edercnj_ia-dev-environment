package dev.iadev.application.feature;

import dev.iadev.domain.feature.AutoDecomposeFeatureHeuristic;
import dev.iadev.domain.feature.CapabilityFeatureDecomposition;
import dev.iadev.domain.feature.CapabilityToFeatureTransformer;
import java.util.List;

public final class FeatureDecompositionUseCase {

    private final AutoDecomposeFeatureHeuristic heuristic;
    private final CapabilityToFeatureTransformer transformer;

    public FeatureDecompositionUseCase(
            AutoDecomposeFeatureHeuristic heuristic,
            CapabilityToFeatureTransformer transformer) {
        this.heuristic = heuristic;
        this.transformer = transformer;
    }

    public CapabilityFeatureDecomposition execute(String capabilityId, List<String> explicitNames) {
        if (capabilityId == null || capabilityId.isBlank()) {
            throw new IllegalArgumentException("capabilityId must not be null or blank");
        }
        List<String> names = (explicitNames == null || explicitNames.isEmpty())
                ? heuristic.decompose(capabilityId)
                : explicitNames;
        return transformer.transform(capabilityId, names);
    }
}
