package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — validates structural invariants for EPIC-0078 story-0078-0003
 * (Renumber 28-tool-call-grammar.md → 30-tool-call-grammar.md) and the subsequent
 * Rules Consolidation (chore/rules-consolidation-essentials) that migrated all
 * numbered rules into a single 00-essentials.md.
 *
 * <p>The 30-tool-call-grammar.md is now a KP under knowledge/governance/rules/ rather
 * than a numbered rule in rules/. The key invariant: no orphan "28-tool-call-grammar"
 * references exist in source.
 */
@DisplayName("Epic0078Story0003SmokeIT — tool-call-grammar consolidation")
class Epic0078Story0003SmokeIT extends SmokeTestBase {

    private static final String PROFILE = "java-spring-hexagonal";

    private static final Path RULES_SOURCE =
            Path.of("src", "main", "resources", "targets", "claude", "rules");

    @Test
    @DisplayName("scenario1_oldRuleFiles_absent_from_source")
    void scenario1_oldRuleFiles_absent_from_source() {
        // All numbered rule files migrated to KPs in rules-consolidation-essentials.
        assertThat(RULES_SOURCE.resolve("30-tool-call-grammar.md"))
                .as("30-tool-call-grammar.md must NOT exist in source-of-truth rules dir"
                        + " (migrated to KP)")
                .doesNotExist();
        assertThat(RULES_SOURCE.resolve("28-tool-call-grammar.md"))
                .as("28-tool-call-grammar.md must NOT exist (renamed to 30, then migrated to KP)")
                .doesNotExist();
    }

    @Test
    @DisplayName("scenario2_pipelineOutput_containsEssentials_notOldRules")
    void scenario2_pipelineOutput_containsEssentials_notOldRules() {
        runPipeline(PROFILE);
        Path outputDir = getOutputDir(PROFILE);

        assertThat(outputDir.resolve(".claude/rules/00-essentials.md"))
                .as("00-essentials.md must be generated in pipeline output")
                .exists();
        assertThat(outputDir.resolve(".claude/rules/30-tool-call-grammar.md"))
                .as("30-tool-call-grammar.md must NOT be in pipeline output (now a KP)")
                .doesNotExist();
        assertThat(outputDir.resolve(".claude/rules/28-tool-call-grammar.md"))
                .as("28-tool-call-grammar.md must NOT be in pipeline output")
                .doesNotExist();
    }

    @Test
    @DisplayName("scenario3_noOrphanReferences_inSourceFiles")
    void scenario3_noOrphanReferences_inSourceFiles() throws IOException {
        // Constructed at runtime so this source file does not contain the orphan literal.
        String orphan = "28" + "-tool-call-grammar";
        // Exclude this test file — it legitimately uses the old name in assertions / javadoc.
        String selfName = getClass().getSimpleName() + ".java";

        Path root = Path.of("src");
        try (Stream<Path> files =
                Files.find(
                        root,
                        Integer.MAX_VALUE,
                        (p, a) ->
                                a.isRegularFile()
                                        && p.toString().matches(".*\\.(md|java|sh|json|yaml)")
                                        && !p.getFileName().toString().equals(selfName))) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                assertThat(content)
                        .as(
                                "ORPHAN_REFERENCE: '%s' must not contain '%s' (renamed to"
                                        + " 30-tool-call-grammar, then migrated to KP)",
                                file, orphan)
                        .doesNotContain(orphan);
            }
        }
    }

    @Test
    @DisplayName("scenario4_allProfiles_essentialsPresent_oldRulesAbsent")
    void scenario4_allProfiles_essentialsPresent_oldRulesAbsent() {
        String[] profiles = {
            "java-spring",
            "java-spring-hexagonal",
            "java-spring-clickhouse",
            "java-spring-cqrs-es",
            "java-spring-event-driven",
            "java-spring-fintech-pci",
            "java-spring-neo4j",
            "java-quarkus"
        };

        for (String profile : profiles) {
            runPipeline(profile);
            Path rulesDir = getOutputDir(profile).resolve(".claude/rules");
            assertThat(rulesDir.resolve("00-essentials.md"))
                    .as("00-essentials.md must exist for profile: " + profile)
                    .exists();
            assertThat(rulesDir.resolve("30-tool-call-grammar.md"))
                    .as("30-tool-call-grammar.md must NOT exist for profile: " + profile
                            + " (now a KP)")
                    .doesNotExist();
            assertThat(rulesDir.resolve("28-tool-call-grammar.md"))
                    .as("28-tool-call-grammar.md must NOT exist for profile: " + profile)
                    .doesNotExist();
        }
    }
}
