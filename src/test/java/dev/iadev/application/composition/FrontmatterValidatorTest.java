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
            Files.writeString(
                    file,
                    """
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
            Files.writeString(
                    file,
                    """
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
            Files.writeString(
                    file,
                    """
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
            Files.writeString(
                    file,
                    """
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
    @DisplayName("ValidationResult helpers")
    class ValidationResultHelpers {

        @Test
        @DisplayName("ok() returns true for passed result")
        void passedResultOk() throws IOException {
            Path file = Files.createTempFile("v3", ".md");
            Files.writeString(file, "---\nname: x\nrequires-capabilities: []\n---\n");
            assertThat(validator.validate(file).ok()).isTrue();
            Files.delete(file);
        }

        @Test
        @DisplayName("validate missing file returns failure with cannot-read message")
        void missingFileReturnsCannotRead(@TempDir Path tmp) {
            var result = validator.validate(tmp.resolve("nonexistent.md"));
            assertThat(result.ok()).isFalse();
            assertThat(result.errors())
                    .anyMatch(e -> e.contains("cannot read") || e.contains("missing"));
        }

        @Test
        @DisplayName("invalid YAML in frontmatter returns failure")
        void invalidYamlFrontmatterFails(@TempDir Path tmp) throws java.io.IOException {
            Path file = tmp.resolve("bad-yaml.md");
            java.nio.file.Files.writeString(file, "---\n{invalid: yaml: [\n---\n");
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
        }

        @Test
        @DisplayName("requires-capabilities with known ID emits no warning")
        void knownCapabilityNoWarning(@TempDir Path tmp) throws java.io.IOException {
            Path file = tmp.resolve("known.md");
            java.nio.file.Files.writeString(
                    file, "---\nname: x\nrequires-capabilities: [data.database.postgres]\n---\n");
            var result = validator.validate(file, java.util.Set.of("data.database.postgres"));
            assertThat(result.ok()).isTrue();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("withWarnings result has ok=true and non-empty warnings")
        void withWarningsIsOk(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("warn.md");
            Files.writeString(
                    file, "---\nname: x\nrequires-capabilities:\n  - unknown.cap.ability\n---\n");
            var result = validator.validate(file, java.util.Set.of("data.database.postgres"));
            assertThat(result.ok()).isTrue();
            assertThat(result.warnings()).isNotEmpty();
        }

        @Test
        @DisplayName("requires-capabilities as scalar (not list) with knownIds — no warning emitted")
        void requiresCapabilitiesNotList_noWarning(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("scalar.md");
            Files.writeString(file, "---\nname: x\nrequires-capabilities: just_a_string\n---\n");
            var result = validator.validate(file, java.util.Set.of("some.cap.id"));
            assertThat(result.ok()).isTrue();
            assertThat(result.warnings()).isEmpty();
        }

        @Test
        @DisplayName("frontmatter without closing delimiter returns failure")
        void unclosedFrontmatterFails(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("unclosed.md");
            Files.writeString(file, "---\nname: x\nrequires-capabilities: []\n(no closing)");
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
        }

        @Test
        @DisplayName("YAML frontmatter that parses to non-Map returns failure")
        void yamlFrontmatterNotMap(@TempDir Path tmp) throws IOException {
            Path file = tmp.resolve("listfm.md");
            Files.writeString(file, "---\n- item1\n- item2\n---\n# content");
            var result = validator.validate(file);
            assertThat(result.ok()).isFalse();
        }
    }

    @Nested
    @DisplayName("batch validation")
    class BatchValidation {

        @Test
        @DisplayName("validateAll reports each file independently")
        void validateAllReportsPerFile(@TempDir Path tmp) throws IOException {
            Files.writeString(
                    tmp.resolve("valid.md"),
                    """
                    ---
                    name: valid-skill
                    requires-capabilities: []
                    ---
                    """);
            Files.writeString(
                    tmp.resolve("invalid.md"),
                    """
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
