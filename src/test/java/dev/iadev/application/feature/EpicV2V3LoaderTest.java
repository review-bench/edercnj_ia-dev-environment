package dev.iadev.application.feature;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.feature.SourceFeatureReference;
import org.junit.jupiter.api.Test;

class EpicV2V3LoaderTest {

    private final EpicV2V3Loader loader = new EpicV2V3Loader();

    @Test
    void parseSourceFeature_withLinkedFeature_returnsLinkedReference() {
        String epicContent =
                """
                **Status:** Em Andamento
                **Source Feature:** feature-oauth2-integration
                **Source Feature Link:** https://example.com/features/oauth2
                """;
        SourceFeatureReference ref = loader.parseSourceFeature(epicContent);
        assertThat(ref.isLinked()).isTrue();
        assertThat(ref.featureId()).isEqualTo("feature-oauth2-integration");
        assertThat(ref.sourceFeatureLink()).isEqualTo("https://example.com/features/oauth2");
    }

    @Test
    void parseSourceFeature_withNaSentinel_returnsNotApplicable() {
        String epicContent =
                """
                **Status:** Em Andamento
                **Source Feature:** N/A
                **Source Feature Link:** —
                """;
        SourceFeatureReference ref = loader.parseSourceFeature(epicContent);
        assertThat(ref.isLinked()).isFalse();
    }

    @Test
    void parseSourceFeature_withMissingField_returnsNotApplicable() {
        String epicContent =
                """
                **Status:** Em Andamento
                ## 1. Visão & Problema
                """;
        SourceFeatureReference ref = loader.parseSourceFeature(epicContent);
        assertThat(ref.isLinked()).isFalse();
    }

    @Test
    void parseSourceFeature_withV2EpicNoSourceFeatureField_returnsNotApplicable() {
        String v2EpicContent =
                """
                **Autor:** Eder
                **Data:** 2026-01-01
                **Versão:** 1.0
                **Status:** Concluída
                ## 1. Visão & Problema
                Old v2 epic without source feature field.
                """;
        SourceFeatureReference ref = loader.parseSourceFeature(v2EpicContent);
        assertThat(ref.isLinked()).isFalse();
    }
}
