package dev.iadev.migrate;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.composition.FrontmatterValidator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Smoke test for EPIC-0064 frontmatter migration contract (story-0064-0202).
 *
 * <p>Validates that FrontmatterValidator correctly detects v2 (legacy) vs v3.0 (migrated) files,
 * which is the gate the x-frontmatter-migrate skill enforces after writing.
 */
@DisplayName("Epic0064FrontmatterMigrateSmokeTest")
class Epic0064FrontmatterMigrateSmokeTest {

    private final FrontmatterValidator validator = new FrontmatterValidator();

    @Nested
    @DisplayName("v2 vs v3.0 detection — migration gate contract")
    class MigrationGate {

        @Test
        @DisplayName("v2 frontmatter (no requires-capabilities) is detected as needing migration")
        void v2DetectedAsNeedingMigration(@TempDir Path tmp) throws IOException {
            Path v2File = tmp.resolve("legacy-skill.md");
            Files.writeString(v2File, """
                    ---
                    name: x-legacy-skill
                    description: A skill without requires-capabilities
                    model: sonnet
                    ---
                    # x-legacy-skill
                    This skill needs migration to v3.0.
                    """);
            var result = validator.validate(v2File);
            assertThat(result.ok()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("requires-capabilities"));
        }

        @Test
        @DisplayName("v3.0 frontmatter (with requires-capabilities) passes validation gate")
        void v30PassesGate(@TempDir Path tmp) throws IOException {
            Path v3File = tmp.resolve("migrated-skill.md");
            Files.writeString(v3File, """
                    ---
                    name: x-migrated-skill
                    description: A skill migrated to v3.0
                    model: sonnet
                    requires-capabilities: [framework.spring-boot.mvc]
                    ---
                    # x-migrated-skill
                    Migrated to frontmatter v3.0.
                    """);
            var result = validator.validate(v3File);
            assertThat(result.ok()).isTrue();
        }

        @Test
        @DisplayName("internal skill preserves visibility and user-invocable after migration")
        void internalSkillPreservesFields(@TempDir Path tmp) throws IOException {
            Path internalFile = tmp.resolve("x-internal-skill.md");
            Files.writeString(internalFile, """
                    ---
                    name: x-internal-example
                    description: Internal skill
                    visibility: internal
                    user-invocable: false
                    requires-capabilities: []
                    ---
                    # Internal skill
                    """);
            var result = validator.validate(internalFile);
            assertThat(result.ok()).isTrue();
            String content = Files.readString(internalFile);
            assertThat(content).contains("visibility: internal");
            assertThat(content).contains("user-invocable: false");
        }

        @Test
        @DisplayName("x-frontmatter-migrate SKILL.md itself is v3.0 compliant")
        void skillFileIsV30Compliant() throws Exception {
            var resource = getClass().getClassLoader().getResource(
                    "targets/claude/skills/core/internal/plan/x-frontmatter-migrate/SKILL.md");
            assertThat(resource).as("SKILL.md should be on classpath").isNotNull();
            Path skillPath = Path.of(resource.toURI());
            var result = validator.validate(skillPath);
            assertThat(result.ok())
                    .as("x-frontmatter-migrate SKILL.md should be valid v3.0 frontmatter, errors: " + result.errors())
                    .isTrue();
        }
    }
}
