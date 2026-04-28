package dev.iadev.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Validates the structure of docs/audit-bypass-catalog.md (story-0063-0009).
 *
 * <p>The catalog must list exactly 11 skills, each with the required 4 sub-sections (evidência,
 * padrão de bypass, plano de blindagem). Schema validation only — content remains free-form per
 * D3 refinement.
 */
class AuditBypassCatalogTest {

    private static final Path CATALOG_PATH = Path.of("docs/audit-bypass-catalog.md");

    private static final int EXPECTED_SKILL_COUNT = 11;

    private static final Pattern SKILL_HEADING =
            Pattern.compile("^## \\d+\\. x-[a-z-]+", Pattern.MULTILINE);

    private static final Pattern EVIDENCE_FIELD =
            Pattern.compile("\\*\\*Tipo de evidência hoje:\\*\\*", Pattern.MULTILINE);

    private static final Pattern BYPASS_FIELD =
            Pattern.compile("\\*\\*Padrão de bypass [^:]+:\\*\\*", Pattern.MULTILINE);

    private static final Pattern HARDENING_FIELD =
            Pattern.compile("\\*\\*Plano de blindagem:\\*\\*", Pattern.MULTILINE);

    @Test
    void catalogExistsAndIsReadable() throws IOException {
        assertThat(CATALOG_PATH).exists().isRegularFile();
        String content = Files.readString(CATALOG_PATH);
        assertThat(content).isNotBlank();
    }

    @Test
    void catalogContainsExactly11Skills() throws IOException {
        String content = Files.readString(CATALOG_PATH);
        Matcher matcher = SKILL_HEADING.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertThat(count)
                .as("audit-bypass-catalog.md must list exactly 11 skills")
                .isEqualTo(EXPECTED_SKILL_COUNT);
    }

    @Test
    void everySkillHasEvidenceTypeField() throws IOException {
        String content = Files.readString(CATALOG_PATH);
        Matcher matcher = EVIDENCE_FIELD.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertThat(count)
                .as("each skill must have **Tipo de evidência hoje:** field")
                .isEqualTo(EXPECTED_SKILL_COUNT);
    }

    @Test
    void everySkillHasBypassPatternField() throws IOException {
        String content = Files.readString(CATALOG_PATH);
        Matcher matcher = BYPASS_FIELD.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertThat(count)
                .as("each skill must have **Padrão de bypass:** field")
                .isEqualTo(EXPECTED_SKILL_COUNT);
    }

    @Test
    void everySkillHasHardeningPlanField() throws IOException {
        String content = Files.readString(CATALOG_PATH);
        Matcher matcher = HARDENING_FIELD.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        assertThat(count)
                .as("each skill must have **Plano de blindagem:** field")
                .isEqualTo(EXPECTED_SKILL_COUNT);
    }

    @Test
    void catalogReferencesEpic0063Rules() throws IOException {
        String content = Files.readString(CATALOG_PATH);
        assertThat(content).contains("EPIC-0063").contains("Rule 24").contains("Rule 27");
    }
}
