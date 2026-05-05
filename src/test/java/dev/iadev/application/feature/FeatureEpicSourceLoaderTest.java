package dev.iadev.application.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeatureEpicSourceLoaderTest {

    private final FeatureEpicSourceLoader loader = new FeatureEpicSourceLoader();

    @Test
    void load_withExampleArtifacts_extractsSourceFeatureAndRnfs() throws Exception {
        FeatureEpicSource source =
                loader.load(
                        Path.of("ai/examples/example-feature-oauth2-integration.md"),
                        Path.of("ai/examples/example-capability-auth.md"),
                        Path.of("ai/examples/example-product-saas.md"));

        assertThat(source.featureId()).isEqualTo("oauth2-integration");
        assertThat(source.capabilityId()).isEqualTo("auth");
        assertThat(source.storyTitles()).hasSize(3);
        assertThat(source.inheritedRnfs()).extracting(InheritedRnfLine::sourceLevel)
                .contains("Product", "Capability");
        assertThat(source.inheritedRnfs())
                .filteredOn(line -> line.id().equals("CAP-PERFORMANCE"))
                .singleElement()
                .extracting(InheritedRnfLine::waivable)
                .isEqualTo(true);
        assertThat(source.inheritedRnfs())
                .filteredOn(line -> line.id().equals("CAP-SECURITY"))
                .singleElement()
                .extracting(InheritedRnfLine::waivable)
                .isEqualTo(false);
    }

    @Test
    void load_withFeatureOnly_returnsFallbackRnfRow() throws Exception {
        FeatureEpicSource source =
                loader.load(Path.of("ai/examples/example-feature-mfa-support.md"), null, null);

        assertThat(source.inheritedRnfs()).singleElement()
                .extracting(InheritedRnfLine::id)
                .isEqualTo("RNF-N/A");
    }
}
