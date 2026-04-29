package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Hard-fail CI gate: every migrated artifact in targets/claude must have requires-capabilities
 * (story-0064-0215, RULE-002, RULE-006 enforcement).
 */
@DisplayName("CapabilityCoverageAuditTest — Rule 28 hard-fail gate")
class CapabilityCoverageAuditTest {

    private static final Path SKILLS_ROOT = Path.of("src/main/resources/targets/claude/skills");
    private static final FrontmatterValidator VALIDATOR = new FrontmatterValidator();

    @Nested
    @DisplayName("hard-fail violations")
    class HardFailViolations {

        @Test
        @DisplayName("all SKILL.md files must have requires-capabilities (v3.0 compliance)")
        void allSkillFilesHaveRequiresCapabilities() throws IOException {
            if (!Files.isDirectory(SKILLS_ROOT)) return;

            List<String> violations;
            try (Stream<Path> paths = Files.walk(SKILLS_ROOT)) {
                violations =
                        paths.filter(p -> p.getFileName().toString().equals("SKILL.md"))
                                .filter(p -> !p.getFileName().toString().startsWith("_"))
                                .filter(
                                        p -> {
                                            var result = VALIDATOR.validate(p);
                                            return !result.ok()
                                                    && result.errors().stream()
                                                            .anyMatch(
                                                                    e ->
                                                                            e.contains(
                                                                                    "requires-capabilities"));
                                        })
                                .map(
                                        p ->
                                                "RULE_28_VIOLATION: "
                                                        + p
                                                        + " missing requires-capabilities")
                                .sorted()
                                .collect(Collectors.toList());
            }
            assertThat(violations)
                    .as(
                            "All SKILL.md files must have requires-capabilities (v3.0). Violations:\n"
                                    + String.join("\n", violations))
                    .isEmpty();
        }

        @Test
        @DisplayName("valid v3.0 file passes the gate (happy)")
        void validV30PassesGate() throws IOException {
            Path tmp = Files.createTempDirectory("audit-test");
            Path file = tmp.resolve("test-skill.md");
            Files.writeString(
                    file,
                    """
                    ---
                    name: x-test-skill
                    requires-capabilities: []
                    ---
                    # Content
                    """);
            var result = VALIDATOR.validate(file);
            assertThat(result.ok()).isTrue();
            Files.deleteIfExists(file);
            Files.deleteIfExists(tmp);
        }

        @Test
        @DisplayName("v2 file (missing requires-capabilities) fails the gate")
        void v2FailsGate() throws IOException {
            Path tmp = Files.createTempDirectory("audit-test-v2");
            Path file = tmp.resolve("legacy.md");
            Files.writeString(
                    file,
                    """
                    ---
                    name: x-legacy
                    model: sonnet
                    ---
                    # Legacy
                    """);
            var result = VALIDATOR.validate(file);
            assertThat(result.ok()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("requires-capabilities"));
            Files.deleteIfExists(file);
            Files.deleteIfExists(tmp);
        }
    }
}
