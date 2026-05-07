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
 * Smoke test — validates structural invariants for EPIC-0078 story-0078-0002 (Remove 02-domain.md
 * from always-loaded layer).
 *
 * <p>Verifies that: (a) 02-domain.md is absent from pipeline output; (b) no reference to
 * 02-domain.md remains in 01-project-identity.md; (c) context budget is reduced by ≥4500 tokens vs
 * the pre-EPIC-0078 baseline.
 */
@DisplayName("Epic0078Story0002SmokeIT — Remove 02-domain.md from always-loaded layer")
class Epic0078Story0002SmokeIT extends SmokeTestBase {

    private static final String PROFILE = "java-spring-hexagonal";

    private static final int BASELINE_TOKENS = 59591;
    private static final int REQUIRED_REDUCTION = 4500;

    @Test
    @DisplayName("scenario1_domainRule_absentFromOutput")
    void scenario1_domainRule_absentFromOutput() {
        runPipeline(PROFILE);
        Path outputDir = getOutputDir(PROFILE);

        Path domainRule = outputDir.resolve(".claude/rules/02-domain.md");
        assertThat(domainRule)
                .as("02-domain.md must NOT be generated in the always-loaded rules layer")
                .doesNotExist();
    }

    @Test
    @DisplayName("scenario2_essentials_containsNoDomainRuleReference")
    void scenario2_projectIdentity_containsNoDomainRuleReference() throws IOException {
        runPipeline(PROFILE);
        Path outputDir = getOutputDir(PROFILE);

        // 01-project-identity.md replaced by 00-essentials.md in rules-consolidation-essentials.
        Path essentials = outputDir.resolve(".claude/rules/00-essentials.md");
        assertThat(essentials).as("00-essentials.md must exist").exists();

        String content = Files.readString(essentials, StandardCharsets.UTF_8);
        assertThat(content)
                .as("00-essentials.md must not reference 02-domain.md")
                .doesNotContain("02-domain.md");
    }

    @Test
    @DisplayName("scenario3_contextBudget_reducedByAtLeast4500Tokens")
    void scenario3_contextBudget_reducedByAtLeast4500Tokens() throws IOException {
        runPipeline(PROFILE);
        Path outputDir = getOutputDir(PROFILE);

        Path rulesDir = outputDir.resolve(".claude/rules");
        long totalBytes = 0;
        try (Stream<Path> files = Files.walk(rulesDir)) {
            for (Path f : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
                totalBytes += Files.size(f);
            }
        }
        int estimatedTokens = (int) (totalBytes / 4);

        int domainRuleTokens = BASELINE_TOKENS - estimatedTokens;
        assertThat(domainRuleTokens)
                .as(
                        "Context budget must be reduced by ≥%d tokens vs baseline (%d). "
                                + "Actual estimated rules tokens: %d",
                        REQUIRED_REDUCTION, BASELINE_TOKENS, estimatedTokens)
                .isGreaterThanOrEqualTo(REQUIRED_REDUCTION);
    }

    @Test
    @DisplayName("scenario4_allProfiles_domainRuleAbsent")
    void scenario4_allProfiles_domainRuleAbsent() {
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
            Path domainRule = getOutputDir(profile).resolve(".claude/rules/02-domain.md");
            assertThat(domainRule)
                    .as("02-domain.md must NOT exist for profile: " + profile)
                    .doesNotExist();
        }
    }
}
