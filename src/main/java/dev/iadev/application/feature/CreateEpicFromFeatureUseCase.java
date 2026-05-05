package dev.iadev.application.feature;

import dev.iadev.adapter.outbound.feature.EpicFromFeatureArtifactWriter;
import java.io.IOException;
import java.nio.file.Path;

public final class CreateEpicFromFeatureUseCase {

    private final FeatureEpicSourceLoader loader;

    public CreateEpicFromFeatureUseCase(FeatureEpicSourceLoader loader) {
        this.loader = loader;
    }

    public CreateEpicFromFeatureResult execute(
            String epicId, Path featureFile, Path capabilityFile, Path productFile, Path outputDir)
            throws IOException {
        validate(epicId, featureFile, outputDir);
        FeatureEpicSource source = loader.load(featureFile, capabilityFile, productFile);
        Path epicFile = EpicFromFeatureArtifactWriter.write(epicId, source, outputDir);
        return new CreateEpicFromFeatureResult(epicFile, source.inheritedRnfs().size());
    }

    private static void validate(String epicId, Path featureFile, Path outputDir) {
        if (epicId == null || !epicId.matches("\\d{4}")) {
            throw new IllegalArgumentException("epicId must be a 4-digit string");
        }
        if (featureFile == null) {
            throw new IllegalArgumentException("featureFile must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
    }
}
