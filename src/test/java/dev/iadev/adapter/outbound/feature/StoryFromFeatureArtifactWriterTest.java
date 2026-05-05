package dev.iadev.adapter.outbound.feature;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.feature.FeatureEpicSource;
import dev.iadev.application.feature.InheritedRnfLine;
import dev.iadev.application.feature.StoryProposal;
import dev.iadev.domain.feature.AcceptanceCriterion;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoryFromFeatureArtifactWriterTest {

    @Test
    void render_omitsLegacySectionsAndAddsRnfSection() {
        FeatureEpicSource source =
                new FeatureEpicSource(
                        "OAuth2 Integration",
                        "oauth2-integration",
                        "auth",
                        "file:///feature.md",
                        List.of("Authorization Code com PKCE"),
                        List.of("SAML federation"),
                        List.of("Login com Google Workspace"),
                        List.of("file:///feature.md"),
                        List.of(
                                new InheritedRnfLine(
                                        "CAP-PERFORMANCE", "Capability", "P99 < 500ms", false)));
        StoryProposal proposal =
                new StoryProposal(
                        "Login com Google Workspace",
                        "Engenheiro de Software",
                        List.of(
                                new AcceptanceCriterion(
                                        "Login bem-sucedido com Google Workspace")));

        String content =
                StoryFromFeatureArtifactWriter.render("story-0077-0001", "0077", source, proposal);

        assertThat(content).contains("**Epic ID:** EPIC-0077");
        assertThat(content).contains("## 2. RNFs Herdadas");
        assertThat(content).contains("## C4 Context");
        assertThat(content).contains("## C4 Code");
        assertThat(content).contains("| PERFORMANCE | P99 < 500ms | true |");
        assertThat(content).doesNotContain("## 2. Persona & Cenário");
        assertThat(content).doesNotContain("## 4. AC (Gherkin");
        assertThat(content).doesNotContain("## 8. Decision Rationale");
    }
}
