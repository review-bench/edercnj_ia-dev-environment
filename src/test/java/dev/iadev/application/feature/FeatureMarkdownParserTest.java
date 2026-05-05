package dev.iadev.application.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeatureMarkdownParserTest {

    private final FeatureMarkdownParser parser = new FeatureMarkdownParser();

    @Test
    void parse_withExampleFeature_extractsUseCasesAndAcceptanceCriteria() throws Exception {
        var feature =
                parser.parse(
                        Files.readString(
                                Path.of("ai/examples/example-feature-oauth2-integration.md")));

        assertThat(feature.featureId()).isEqualTo("oauth2-integration");
        assertThat(feature.useCases()).hasSize(3);
        assertThat(feature.acceptanceCriteria()).isNotEmpty();
    }
}
