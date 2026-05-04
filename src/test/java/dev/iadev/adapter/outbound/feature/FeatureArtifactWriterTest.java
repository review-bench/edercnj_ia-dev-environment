package dev.iadev.adapter.outbound.feature;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.feature.CapabilityFeatureDecomposition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FeatureArtifactWriter")
class FeatureArtifactWriterTest {

    @TempDir
    Path outputDir;

    private CapabilityFeatureDecomposition decomposition() {
        return new CapabilityFeatureDecomposition(
                "capability-c1", List.of("BasicAuth", "OAuth2", "MFA", "Session"));
    }

    @Test
    void write_validDecomposition_createsArtifactPerFeature() throws IOException {
        FeatureArtifactWriter.write(decomposition(), outputDir);
        assertThat(outputDir.resolve("capability-c1-feature-0001.json")).exists();
        assertThat(outputDir.resolve("capability-c1-feature-0002.json")).exists();
        assertThat(outputDir.resolve("capability-c1-feature-0003.json")).exists();
        assertThat(outputDir.resolve("capability-c1-feature-0004.json")).exists();
    }

    @Test
    void write_artifact_containsFeatureName() throws IOException {
        FeatureArtifactWriter.write(decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("capability-c1-feature-0001.json"));
        assertThat(content).contains("BasicAuth");
    }

    @Test
    void write_artifact_containsCapabilityId() throws IOException {
        FeatureArtifactWriter.write(decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("capability-c1-feature-0001.json"));
        assertThat(content).contains("capability-c1");
    }

    @Test
    void write_artifact_containsGherkinScenarios() throws IOException {
        FeatureArtifactWriter.write(decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("capability-c1-feature-0001.json"));
        assertThat(content).contains("Scenario:");
    }

    @Test
    void write_secondRun_skipsExistingArtifacts() throws IOException {
        var first = FeatureArtifactWriter.write(decomposition(), outputDir);
        var second = FeatureArtifactWriter.write(decomposition(), outputDir);
        assertThat(first.writtenCount()).isEqualTo(4);
        assertThat(second.writtenCount()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(4);
    }
}
