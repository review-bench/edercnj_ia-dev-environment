package dev.iadev.generator.templates;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates value-driven structural invariants of {@code _TEMPLATE-EPIC.md} v2 (story-0070-0002).
 *
 * <p>Checks: all 9 section headers present and v2-specific value-driven content for hypothesis,
 * OKRs, and alternatives.
 */
@DisplayName("TemplateEpicV2StructureTest")
class TemplateEpicV2StructureTest {

    private static final Path TEMPLATE =
            Path.of("src", "main", "resources", "shared", "templates", "_TEMPLATE-EPIC.md");

    private static final List<String> REQUIRED_SECTIONS =
            List.of(
                    "## 1. Visão & Problema",
                    "## 2. Persona & Stakeholders",
                    "## 3. Hipótese & OKRs",
                    "## 4. Alternativas Consideradas",
                    "## 5. Escopo",
                    "## 6. Riscos",
                    "## 7. Índice de Histórias",
                    "## 8. Quality Gates",
                    "## Refinement Verdict");

    private static final List<String> REQUIRED_V2_CONTENT =
            List.of("Hipótese de Valor", "Decisão de rejeição:", "story-XXXX-", "File Footprint");

    @Test
    @DisplayName("epicTemplate_hasAllNineRa9Sections")
    void epicTemplate_hasAllNineRa9Sections() throws IOException {
        String content = readTemplate();
        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("_TEMPLATE-EPIC.md v2 must have section: %s", section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("epicTemplate_hasV2ValueDrivenContent")
    void epicTemplate_hasV2ValueDrivenContent() throws IOException {
        String content = readTemplate();
        for (String marker : REQUIRED_V2_CONTENT) {
            assertThat(content)
                    .as("_TEMPLATE-EPIC.md v2 must have content: %s", marker)
                    .contains(marker);
        }
    }

    @Test
    @DisplayName("epicTemplate_hasHypothesisAndOkrsMicroTemplate")
    void epicTemplate_hasHypothesisAndOkrsMicroTemplate() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Epic template must include value-driven hypothesis and OKRs fields")
                .contains("Hipótese de Valor")
                .contains("Decisão de rejeição:")
                .contains("OKRs");
    }

    @Test
    @DisplayName("epicTemplate_preservesStoryIndexSection")
    void epicTemplate_preservesStoryIndexSection() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Story index must be preserved (in section 7)")
                .contains("story-XXXX-")
                .contains("Dependências");
    }

    private String readTemplate() throws IOException {
        return Files.readString(TEMPLATE.toAbsolutePath(), StandardCharsets.UTF_8);
    }
}
