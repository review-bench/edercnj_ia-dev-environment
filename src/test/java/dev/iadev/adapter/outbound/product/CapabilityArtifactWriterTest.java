package dev.iadev.adapter.outbound.product;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.product.ProductCapabilityDecomposition;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CapabilityArtifactWriter")
class CapabilityArtifactWriterTest {

    @TempDir
    Path outputDir;

    private ProductCapabilityDecomposition decomposition() {
        return new ProductCapabilityDecomposition(
                "product-0001", List.of("ingest", "query", "storage"));
    }

    @Test
    void write_validDecomposition_createsArtifactPerCapability() throws IOException {
        CapabilityArtifactWriter.write(decomposition(), outputDir);
        assertThat(outputDir.resolve("product-0001-capability-c1.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c2.json")).exists();
        assertThat(outputDir.resolve("product-0001-capability-c3.json")).exists();
    }

    @Test
    void write_artifact_containsCapabilityName() throws IOException {
        CapabilityArtifactWriter.write(decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("product-0001-capability-c1.json"));
        assertThat(content).contains("ingest");
    }

    @Test
    void write_secondRun_skipsExistingArtifacts() throws IOException {
        CapabilityArtifactWriter.WriteResult first = CapabilityArtifactWriter.write(decomposition(), outputDir);
        CapabilityArtifactWriter.WriteResult second = CapabilityArtifactWriter.write(decomposition(), outputDir);
        assertThat(first.writtenCount()).isEqualTo(3);
        assertThat(second.writtenCount()).isEqualTo(0);
        assertThat(second.skippedCount()).isEqualTo(3);
    }

    @Test
    void write_artifact_containsCapabilityId() throws IOException {
        CapabilityArtifactWriter.write(decomposition(), outputDir);
        String content = Files.readString(outputDir.resolve("product-0001-capability-c1.json"));
        assertThat(content).contains("capability-c1");
    }
}
