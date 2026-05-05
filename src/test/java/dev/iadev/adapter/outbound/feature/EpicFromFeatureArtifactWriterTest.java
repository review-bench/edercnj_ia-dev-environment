package dev.iadev.adapter.outbound.feature;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.feature.FeatureEpicSource;
import dev.iadev.application.feature.InheritedRnfLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class EpicFromFeatureArtifactWriterTest {

    @Test
    void render_dropsSectionsTwoFourAndEight() {
        FeatureEpicSource source =
                new FeatureEpicSource(
                        "OAuth2 Integration",
                        "oauth2-integration",
                        "auth",
                        "file:///feature.md",
                        List.of("Authorization Code com PKCE"),
                        List.of("SAML federation"),
                        List.of("Login com Google Workspace", "Login com GitHub Organizations"),
                        List.of("file:///feature.md", "file:///capability.md"),
                        List.of(
                                new InheritedRnfLine(
                                        "CAP-PERFORMANCE", "Capability", "P99 < 500ms", false)));

        String content = EpicFromFeatureArtifactWriter.render("0077", source);

        assertThat(content).contains("**Source Feature:** oauth2-integration");
        assertThat(content).contains("## 0.6 Inherited RNFs");
        assertThat(content).contains("## C4 Context");
        assertThat(content).contains("## C4 Code");
        assertThat(content).doesNotContain("## 2. Persona & Stakeholders");
        assertThat(content).doesNotContain("## 4. Alternativas Consideradas");
        assertThat(content).doesNotContain("## 8. Quality Gates");
    }
}
