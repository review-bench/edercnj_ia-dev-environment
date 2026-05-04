package dev.iadev.application.feature;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.feature.GherkinACGenerator;
import dev.iadev.domain.feature.AutoDecomposeFeatureHeuristic;
import dev.iadev.domain.feature.CapabilityToFeatureTransformer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CreateFeaturesOrchestrationUseCase")
class CreateFeaturesOrchestrationUseCaseTest {

    @TempDir
    Path outputDir;

    private final CreateFeaturesOrchestrationUseCase useCase =
            new CreateFeaturesOrchestrationUseCase(
                    new FeatureDecompositionUseCase(
                            new AutoDecomposeFeatureHeuristic(),
                            new CapabilityToFeatureTransformer()),
                    new GherkinACGenerator());

    @Test
    void execute_autoDecompose_writesBetweenFourAndEightArtifacts() throws IOException {
        var result = useCase.execute("capability-c1", List.of(), outputDir);
        assertThat(result.featuresCreated()).isBetween(4, 8);
        assertThat(result.skippedCount()).isEqualTo(0);
    }

    @Test
    void execute_explicitNames_writesExactCount() throws IOException {
        var result = useCase.execute("capability-c1",
                List.of("BasicAuth", "OAuth2", "MFA", "Session"), outputDir);
        assertThat(result.featuresCreated()).isEqualTo(4);
        assertThat(outputDir.resolve("capability-c1-feature-0001.json")).exists();
    }

    @Test
    void execute_rerun_allSkipped() throws IOException {
        var names = List.of("BasicAuth", "OAuth2", "MFA", "Session");
        useCase.execute("capability-c1", names, outputDir);
        var second = useCase.execute("capability-c1", names, outputDir);
        assertThat(second.featuresCreated()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(4);
    }
}
