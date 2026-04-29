package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FrontmatterValidator")
class FrontmatterValidatorTest {

    private final FrontmatterValidator validator = new FrontmatterValidator();

    @Nested
    @DisplayName("single file validation")
    class SingleFile {

        @Test
        @DisplayName("file without frontmatter block fails (degenerate)")
        void missingFrontmatterFails(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("no-fm.md");
            Files.writeString(file, "# Just a heading\nNo frontmatter here.");
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("missing frontmatter block"));
        }

        @Test
        @DisplayName("valid v3.0 frontmatter passes (happy)")
        void validV30Passes(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("valid.md");
            Files.writeString(file, """
                    ---
                    name: x-test-skill
                    description: A test skill
                    requires-capabilities: []
                    ---
                    # Content
                    """);
            var result = validator.validate(file);
            assertThat(result.ok()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("frontmatter v2 without requires-capabilities fails (RULE-006)")
        void v2FrontmatterFails(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("legacy.md");
            Files.writeString(file, """
                    ---
                    name: x-legacy-skill
                    description: Old style frontmatter
                    ---
                    # Content
                    """);
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("requires-capabilities"));
        }

        @Test
        @DisplayName("unknown capability ID emits warning (not fatal)")
        void unknownCapabilityIdWarns(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("warn.md");
            Files.writeString(file, """
                    ---
                    name: x-test-skill
                    requires-capabilities:
                      - data.unknown.fake
                    ---
                    """);
            var result = validator.validate(file, Set.of("data.database.postgres"));
            assertThat(result.ok()).isTrue();
            assertThat(result.warnings()).anyMatch(w -> w.contains("data.unknown.fake"));
        }

        @Test
        @DisplayName("missing name field also fails")
        void missingNameFails(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("no-name.md");
            Files.writeString(file, """
                    ---
                    description: Missing name
                    requires-capabilities: []
                    ---
                    """);
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("missing required field: name"));
        }
    }

    @Nested
    @DisplayName("batch validation")
    class BatchValidation {

        @Test
        @DisplayName("validateAll reports each file independently")
        void validateAllReportsPerFile(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("valid.md"), """
                    ---
                    name: valid-skill
                    requires-capabilities: []
                    ---
                    """);
            Files.writeString(tmp.resolve("invalid.md"), """
                    ---
                    name: invalid-skill
                    ---
                    """);
            var results = validator.validateAll(tmp);
            assertThat(results).hasSize(2);
            assertThat(results.stream().filter(r -> r.ok()).count()).isEqualTo(1);
            assertThat(results.stream().filter(r -> !r.ok()).count()).isEqualTo(1);
        }
    }
}
