package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Epic0075MemorySearchSmokeIT — x-memory-search structural invariants")
class Epic0075MemorySearchSmokeIT {

    private static final Path SKILL_FILE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "skills",
                    "core",
                    "ops",
                    "x-memory-search",
                    "SKILL.md");

    private String readSkill() throws IOException {
        assertThat(SKILL_FILE).as("x-memory-search/SKILL.md must exist").exists();
        return Files.readString(SKILL_FILE, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("scenario1_frontmatter_publicVisibilityHaikuModel")
    void scenario1_frontmatter_publicVisibilityHaikuModel() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must be user-invocable: true").contains("user-invocable: true");
        assertThat(skill).as("must declare model: haiku (Rule 23)").contains("model: haiku");
        assertThat(skill)
                .as("must require governance.ai-memory")
                .contains("requires-capabilities: [governance.ai-memory]");
    }

    @Test
    @DisplayName("scenario2_parameters_mandatoryFlagsDeclared")
    void scenario2_parameters_mandatoryFlagsDeclared() throws IOException {
        String skill = readSkill();
        assertThat(skill).contains("--by-tag");
        assertThat(skill).contains("--by-pattern");
        assertThat(skill).contains("--by-epic");
        assertThat(skill).contains("--by-rule");
        assertThat(skill).contains("--by-adr");
        assertThat(skill).contains("--include-archived");
        assertThat(skill).contains("--format");
        assertThat(skill).contains("--limit");
    }

    @Test
    @DisplayName("scenario3_exitCodes_threeCodesDeclared")
    void scenario3_exitCodes_threeCodesDeclared() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("exit 0 OK").contains("OK");
        assertThat(skill).as("exit 1 INDEX_NOT_FOUND").contains("INDEX_NOT_FOUND");
        assertThat(skill).as("exit 2 INVALID_ARGS").contains("INVALID_ARGS");
    }

    @Test
    @DisplayName("scenario4_executionProtocol_fourStepsDeclared")
    void scenario4_executionProtocol_fourStepsDeclared() throws IOException {
        String skill = readSkill();
        assertThat(skill).contains("Step 1");
        assertThat(skill).contains("Step 2");
        assertThat(skill).contains("Step 3");
        assertThat(skill).contains("Step 4");
    }

    @Test
    @DisplayName("scenario5_filterLogic_allFiltersDescribed")
    void scenario5_filterLogic_allFiltersDescribed() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("archived filter").contains("indexable: true");
        assertThat(skill).as("tag filter").contains("tags:");
        assertThat(skill).as("pattern filter").contains("patterns-introduced");
        assertThat(skill).as("rule filter").contains("rules-affected");
        assertThat(skill).as("adr filter").contains("adrs-referenced");
    }

    @Test
    @DisplayName("scenario6_outputFormats_compactAndFull")
    void scenario6_outputFormats_compactAndFull() throws IOException {
        String skill = readSkill();
        assertThat(skill).contains("compact");
        assertThat(skill).contains("full");
    }

    @Test
    @DisplayName("scenario7_determinismContract_statedExplicitly")
    void scenario7_determinismContract_statedExplicitly() throws IOException {
        String skill = readSkill();
        assertThat(skill).as("must state determinism contract").contains("Determinism contract");
        assertThat(skill).as("must be read-only").contains("read-only");
    }
}
