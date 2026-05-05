package dev.iadev.domain.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SourceFeatureReferenceTest {

    @Test
    void notApplicable_returnsNaSentinel() {
        SourceFeatureReference ref = SourceFeatureReference.notApplicable();
        assertThat(ref.featureId()).isEqualTo("N/A");
        assertThat(ref.sourceFeatureLink()).isEqualTo("—");
        assertThat(ref.isLinked()).isFalse();
    }

    @Test
    void of_withValidId_createsLinkedReference() {
        SourceFeatureReference ref =
                SourceFeatureReference.of(
                        "feature-oauth2-integration", "https://example.com/features/oauth2");
        assertThat(ref.featureId()).isEqualTo("feature-oauth2-integration");
        assertThat(ref.sourceFeatureLink()).isEqualTo("https://example.com/features/oauth2");
        assertThat(ref.isLinked()).isTrue();
    }

    @Test
    void of_withNaId_treatedAsNotApplicable() {
        SourceFeatureReference ref = SourceFeatureReference.of("N/A", "—");
        assertThat(ref.isLinked()).isFalse();
    }

    @Test
    void of_withBlankFeatureId_throwsIllegalArgument() {
        assertThatThrownBy(() -> SourceFeatureReference.of("", "https://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_withNullFeatureId_throwsIllegalArgument() {
        assertThatThrownBy(() -> SourceFeatureReference.of(null, "https://example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
