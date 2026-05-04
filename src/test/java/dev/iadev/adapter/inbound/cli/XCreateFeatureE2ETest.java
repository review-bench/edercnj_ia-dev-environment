package dev.iadev.adapter.inbound.cli;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.feature.CreateFeaturesOrchestrationUseCase;
import dev.iadev.application.feature.CreateFeaturesResult;
import dev.iadev.application.feature.FeatureDecompositionUseCase;
import dev.iadev.domain.feature.AutoDecomposeFeatureHeuristic;
import dev.iadev.domain.feature.CapabilityToFeatureTransformer;
import dev.iadev.domain.feature.GherkinACGenerator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("XCreateFeature E2E Smoke")
class XCreateFeatureE2ETest {

    @TempDir
    Path outputDir;

    private final CreateFeaturesOrchestrationUseCase useCase =
            new CreateFeaturesOrchestrationUseCase(
                    new FeatureDecompositionUseCase(
                            new AutoDecomposeFeatureHeuristic(),
                            new CapabilityToFeatureTransformer()),
                    new GherkinACGenerator());

    @Test
    void autoDecompose_writesFeatureArtifactsWithGherkin() throws IOException {
        CreateFeaturesResult result = useCase.execute("capability-auth", List.of(), outputDir);

        assertThat(result.featuresCreated()).isBetween(4, 8);
        assertThat(result.skippedCount()).isEqualTo(0);

        Path firstArtifact = outputDir.resolve("capability-auth-feature-0001.json");
        assertThat(firstArtifact).exists();

        String content = Files.readString(firstArtifact);
        assertThat(content).contains("\"capabilityId\": \"capability-auth\"");
        assertThat(content).contains("\"featureId\": \"feature-0001\"");
        assertThat(content).contains("Scenario:");
        assertThat(content).contains("idempotencyHash");
    }

    @Test
    void explicitFeatureNames_writesExactCount() throws IOException {
        var names = List.of("Login", "Logout", "PasswordReset", "MFA");
        CreateFeaturesResult result = useCase.execute("capability-auth", names, outputDir);

        assertThat(result.featuresCreated()).isEqualTo(4);
        assertThat(outputDir.resolve("capability-auth-feature-0001.json")).exists();
        assertThat(outputDir.resolve("capability-auth-feature-0004.json")).exists();
    }

    @Test
    void idempotentRun_skipsExistingArtifacts() throws IOException {
        var names = List.of("Login", "Logout", "PasswordReset", "MFA");
        useCase.execute("capability-auth", names, outputDir);
        CreateFeaturesResult second = useCase.execute("capability-auth", names, outputDir);

        assertThat(second.featuresCreated()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(4);
    }

    @Test
    void gherkinScenarios_coverAllFourCategories() throws IOException {
        useCase.execute("capability-payment", List.of("Checkout", "Refund", "Invoice", "Subscription"), outputDir);

        String content = Files.readString(outputDir.resolve("capability-payment-feature-0001.json"));
        assertThat(content).contains("degenerate");
        assertThat(content).contains("happy path");
        assertThat(content).contains("error");
        assertThat(content).contains("boundary");
    }
}
