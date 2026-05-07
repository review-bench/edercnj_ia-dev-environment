package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.composition.CapabilityAwareComposer;
import dev.iadev.application.composition.CompositionPlan;
import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0014 (requires-capabilities annotations + advisory pruning).
 */
@DisplayName("Epic0078Story0014SmokeIT — rule frontmatter annotations + advisory pruning")
class Epic0078Story0014SmokeIT {

    // EPIC-0078: numbered rules replaced by KPs in knowledge/governance/rules/ (23 files,
    // all with requires-capabilities frontmatter v3.0).
    private static final Path RULES_DIR =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "rules");
    // Rule 12 lives as a conditional Java-specific file; frontmatter added in EPIC-0078.
    private static final Path RULE_12 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "rules",
                    "conditional",
                    "security-anti-patterns",
                    "12-security-anti-patterns.java.md");

    private static ResolvedCapabilitySet capSet(List<String> ids) {
        return new ResolvedCapabilitySet(
                "test", ids.stream().map(CapabilityId::of).toList(), Map.of(), List.of());
    }

    @Test
    @DisplayName("scenario1_allRulesHaveRequiresCapabilitiesFrontmatter")
    void scenario1_allRulesHaveRequiresCapabilitiesFrontmatter() throws IOException {
        assertThat(RULES_DIR).as("rules directory must exist").isDirectory();
        try (Stream<Path> paths = Files.list(RULES_DIR)) {
            List<Path> mdFiles = paths.filter(p -> p.toString().endsWith(".md")).toList();
            assertThat(mdFiles).as("rules directory must have .md files").isNotEmpty();
            for (Path rule : mdFiles) {
                String content = Files.readString(rule, StandardCharsets.UTF_8);
                assertThat(content)
                        .as(
                                "Rule %s must have requires-capabilities frontmatter",
                                rule.getFileName())
                        .startsWith("---")
                        .contains("requires-capabilities");
            }
        }
    }

    @Test
    @DisplayName("scenario2_rule12HasJavaCapability")
    void scenario2_rule12HasJavaCapability() throws IOException {
        String content = Files.readString(RULE_12, StandardCharsets.UTF_8);
        assertThat(content)
                .as("Rule 12 must require lang.java.* capability")
                .contains("lang.java.*");
    }

    @Test
    @DisplayName("scenario3_hardModeIsDefaultAndExcludesNonMatchingArtifacts")
    void scenario3_advisoryModeIsDefaultAndEmitsWarning() throws IOException {
        CapabilityAwareComposer composer = new CapabilityAwareComposer();
        assertThat(composer.mode())
                .as("default mode must be HARD since story-0078-0016")
                .isEqualTo(CapabilityAwareComposer.PruningMode.HARD);

        Path tempDir = Files.createTempDirectory("story0014-smoke");
        try {
            Path rule = tempDir.resolve("12-security-anti-patterns.md");
            Files.writeString(rule, "---\nrequires-capabilities: [lang.java.*]\n---\n# Rule 12\n");

            ResolvedCapabilitySet pythonProfile = capSet(List.of());
            CompositionPlan plan = composer.plan(pythonProfile, tempDir);
            // HARD mode: non-matching artifact moves to excluded, not included
            assertThat(plan.included()).isEmpty();
            assertThat(plan.excluded()).hasSize(1);
            assertThat(plan.warnings()).isEmpty();
        } finally {
            Files.deleteIfExists(tempDir.resolve("12-security-anti-patterns.md"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    @DisplayName("scenario4_javaProfileKeepsRule12Active")
    void scenario4_javaProfileKeepsRule12Active() throws IOException {
        CapabilityAwareComposer composer = new CapabilityAwareComposer();
        Path tempDir = Files.createTempDirectory("story0014-java");
        try {
            Path rule = tempDir.resolve("12-security-anti-patterns.md");
            Files.writeString(rule, "---\nrequires-capabilities: [lang.java.*]\n---\n# Rule 12\n");

            ResolvedCapabilitySet javaProfile = capSet(List.of("lang.java.21"));
            CompositionPlan plan = composer.plan(javaProfile, tempDir);
            assertThat(plan.included()).hasSize(1);
            assertThat(plan.warnings()).isEmpty();
        } finally {
            Files.deleteIfExists(tempDir.resolve("12-security-anti-patterns.md"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    @DisplayName("scenario5_hardModeExcludesNonMatchingArtifacts")
    void scenario5_hardModeExcludesNonMatchingArtifacts() throws IOException {
        CapabilityAwareComposer hardComposer =
                new CapabilityAwareComposer(CapabilityAwareComposer.PruningMode.HARD);
        Path tempDir = Files.createTempDirectory("story0014-hard");
        try {
            Path rule = tempDir.resolve("12-security-anti-patterns.md");
            Files.writeString(rule, "---\nrequires-capabilities: [lang.java.*]\n---\n# Rule 12\n");

            ResolvedCapabilitySet pythonProfile = capSet(List.of());
            CompositionPlan plan = hardComposer.plan(pythonProfile, tempDir);
            assertThat(plan.included()).isEmpty();
            assertThat(plan.excluded()).hasSize(1);
        } finally {
            Files.deleteIfExists(tempDir.resolve("12-security-anti-patterns.md"));
            Files.deleteIfExists(tempDir);
        }
    }
}
