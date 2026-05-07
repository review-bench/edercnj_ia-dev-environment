package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Smoke test — EPIC-0078 story-0078-0006 (Slim Rule 25 → KP lifecycle/task-hierarchy.md). */
@DisplayName("Epic0078Story0006SmokeIT — Slim Rule 25 task-hierarchy KP")
class Epic0078Story0006SmokeIT {

    private static final Path KP_FILE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "lifecycle",
                    "task-hierarchy.md");

    // EPIC-0078 rules-consolidation-essentials: Rule 25 merged into 00-essentials.md +
    // knowledge/governance/rules/task-hierarchy.md (compact contract) + lifecycle KP
    // knowledge/lifecycle/task-hierarchy.md (full BNF/metadata reference).
    private static final Path RULE_25 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "rules",
                    "task-hierarchy.md");

    @Test
    @DisplayName("scenario1_kpFileExists")
    void scenario1_kpFileExists() {
        assertThat(KP_FILE).as("lifecycle/task-hierarchy.md must exist").exists();
    }

    @Test
    @DisplayName("scenario2_kpContainsRequiredSections")
    void scenario2_kpContainsRequiredSections() throws IOException {
        String content = Files.readString(KP_FILE, StandardCharsets.UTF_8);
        assertThat(content)
                .as("KP must contain Subject Regex BNF section")
                .contains("## Subject Regex BNF");
        assertThat(content)
                .as("KP must contain activeForm Convention section")
                .contains("## `activeForm` Convention");
        assertThat(content)
                .as("KP must contain metadata Convention section")
                .contains("## `metadata` Convention");
        assertThat(content)
                .as("KP must contain taskTracking JSON shape section")
                .contains("## `taskTracking` JSON Shape");
        assertThat(content).as("KP must contain the U+203A separator character").contains("›");
    }

    @Test
    @DisplayName("scenario3_rule25IsSlimmedToAtMost60Lines")
    void scenario3_rule25IsSlimmedToAtMost60Lines() throws IOException {
        assertThat(RULE_25).as("Rule 25 must exist").exists();
        long lineCount = Files.lines(RULE_25, StandardCharsets.UTF_8).count();
        assertThat(lineCount)
                .as("Rule 25 must be ≤60 lines but was %d", lineCount)
                .isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("scenario4_rule25ContainsInvariants")
    void scenario4_rule25ContainsInvariants() throws IOException {
        String content = Files.readString(RULE_25, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 25 must contain Invariants section").contains("## Invariants");
        assertThat(content)
                .as("Rule 25 must contain Enforcement Layers")
                .contains("## Enforcement Layers");
        assertThat(content).as("Rule 25 must contain Forbidden").contains("## Forbidden");
    }

    @Test
    @DisplayName("scenario5_kpHasFrontmatterWithRequiresCapabilities")
    void scenario5_kpHasFrontmatterWithRequiresCapabilities() throws IOException {
        String content = Files.readString(KP_FILE, StandardCharsets.UTF_8);
        assertThat(content).as("KP must start with frontmatter delimiter").startsWith("---");
        assertThat(content)
                .as("KP must have requires-capabilities")
                .contains("requires-capabilities");
    }
}
