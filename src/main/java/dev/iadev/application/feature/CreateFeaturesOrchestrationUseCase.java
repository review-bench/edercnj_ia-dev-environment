package dev.iadev.application.feature;

import dev.iadev.adapter.outbound.feature.FeatureArtifactWriter;
import dev.iadev.domain.feature.GherkinACGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class CreateFeaturesOrchestrationUseCase {

    private final FeatureDecompositionUseCase decompositionUseCase;
    private final GherkinACGenerator gherkinGenerator;

    public CreateFeaturesOrchestrationUseCase(
            FeatureDecompositionUseCase decompositionUseCase,
            GherkinACGenerator gherkinGenerator) {
        this.decompositionUseCase = decompositionUseCase;
        this.gherkinGenerator = gherkinGenerator;
    }

    public CreateFeaturesResult execute(String capabilityId, List<String> featureNames, Path outputDir)
            throws IOException {
        var decomposition = decompositionUseCase.execute(capabilityId, featureNames);
        var result = FeatureArtifactWriter.write(decomposition, gherkinGenerator, outputDir);
        return new CreateFeaturesResult(result.writtenCount(), result.skippedCount());
    }
}
