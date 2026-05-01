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
 * Validates value-driven structural invariants of {@code _TEMPLATE-STORY.md} v2 (story-0070-0003).
 *
 * <p>Checks: all 9 section headers present, v2-specific AC categories in Gherkin section, Decision
 * Rationale micro-template, and File Footprint preserved in Dependências section.
 */
@DisplayName("TemplateStoryV2StructureTest")
class TemplateStoryV2StructureTest {

    private static final Path TEMPLATE =
            Path.of("src", "main", "resources", "shared", "templates", "_TEMPLATE-STORY.md");

    private static final List<String> REQUIRED_SECTIONS =
            List.of(
                    "## 1. Visão",
                    "## 2. Persona & Cenário",
                    "## 3. Entrega de Valor",
                    "## 4. AC (Gherkin — 4 categorias mandatórias)",
                    "## 5. Contratos",
                    "## 6. Tasks",
                    "## 7. Dependências",
                    "## 8. Decision Rationale",
                    "## 9. Refinement Verdict");

    @Test
    @DisplayName("storyTemplate_hasAllNineRa9Sections")
    void storyTemplate_hasAllNineRa9Sections() throws IOException {
        String content = readTemplate();
        for (String section : REQUIRED_SECTIONS) {
            assertThat(content)
                    .as("_TEMPLATE-STORY.md v2 must have section: %s", section)
                    .contains(section);
        }
    }

    @Test
    @DisplayName("storyTemplate_hasMandatoryAcCategories")
    void storyTemplate_hasMandatoryAcCategories() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Story template must have all 4 mandatory AC categories in Gherkin section")
                .contains("Cenario: Degenerate")
                .contains("Cenario: Happy")
                .contains("Cenario: Error")
                .contains("Cenario: Performance/SLA");
    }

    @Test
    @DisplayName("storyTemplate_hasDecisionRationaleMicroTemplate")
    void storyTemplate_hasDecisionRationaleMicroTemplate() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Story template must have Decision Rationale" + " 4-line micro-template")
                .contains("**Decisão:**")
                .contains("**Motivo:**")
                .contains("**Alternativa descartada:**")
                .contains("**Consequência:**");
    }

    @Test
    @DisplayName("storyTemplate_hasGherkinInQualityGatesSection")
    void storyTemplate_hasGherkinInQualityGatesSection() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as(
                        "Gherkin acceptance criteria must be in section 5"
                                + " (Quality Gates) as subsection 5.2")
                .contains("5.2")
                .contains("Gherkin")
                .contains("Cenario:");
    }

    @Test
    @DisplayName("storyTemplate_hasFileFootprintInSection9")
    void storyTemplate_hasFileFootprintInSection9() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("File Footprint block must appear in" + " section 9 (Dependências)")
                .contains("File Footprint")
                .contains("write:")
                .contains("read:");
    }

    @Test
    @DisplayName("storyTemplate_hasTasksInSection9")
    void storyTemplate_hasTasksInSection9() throws IOException {
        String content = readTemplate();
        assertThat(content)
                .as("Tasks section must be in section 9" + " or reference task breakdown")
                .contains("TASK-")
                .contains("## 9.");
    }

    private String readTemplate() throws IOException {
        return Files.readString(TEMPLATE.toAbsolutePath(), StandardCharsets.UTF_8);
    }
}
