package dev.iadev.application.feature;

import dev.iadev.adapter.outbound.feature.StoryFromFeatureArtifactWriter;
import dev.iadev.domain.feature.Feature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CreateStoriesFromFeatureUseCase {

    private final FeatureMarkdownParser featureParser;
    private final FeatureEpicSourceLoader sourceLoader;
    private final FeatureToStoryDecompositionUseCase decompositionUseCase;

    public CreateStoriesFromFeatureUseCase(
            FeatureMarkdownParser featureParser,
            FeatureEpicSourceLoader sourceLoader,
            FeatureToStoryDecompositionUseCase decompositionUseCase) {
        this.featureParser = featureParser;
        this.sourceLoader = sourceLoader;
        this.decompositionUseCase = decompositionUseCase;
    }

    public CreateStoriesFromFeatureResult execute(
            String epicId, Path featureFile, Path capabilityFile, Path productFile, Path outputDir)
            throws IOException {
        validate(epicId, featureFile, outputDir);
        Feature feature = featureParser.parse(Files.readString(featureFile));
        FeatureEpicSource source = sourceLoader.load(featureFile, capabilityFile, productFile);
        var proposals = decompositionUseCase.decompose(feature);
        return new CreateStoriesFromFeatureResult(
                StoryFromFeatureArtifactWriter.write(epicId, source, proposals, outputDir));
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
