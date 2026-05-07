package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0008 (Slim Rule 28 → KP governance/capability-composition.md).
 */
@DisplayName("Epic0078Story0008SmokeIT — Slim Rule 28 capability-composition KP")
class Epic0078Story0008SmokeIT {

    private static final Path KP_FILE =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "capability-composition.md");

    // EPIC-0078 rules-consolidation-essentials: Rule 28 merged into 00-essentials.md +
    // knowledge/governance/rules/capability-frontmatter.md KP.
    private static final Path RULE_28 =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "governance",
                    "rules",
                    "capability-frontmatter.md");

    @Test
    @DisplayName("scenario1_kpFileExists")
    void scenario1_kpFileExists() {
        assertThat(KP_FILE).as("governance/capability-composition.md must exist").exists();
    }

    @Test
    @DisplayName("scenario2_kpContainsRequiredSections")
    void scenario2_kpContainsRequiredSections() throws IOException {
        String content = Files.readString(KP_FILE, StandardCharsets.UTF_8);
        assertThat(content)
                .as("KP must contain Capability ID Format section")
                .contains("## Capability ID Format");
        assertThat(content).as("KP must contain Glob Rules section").contains("## Glob Rules");
        assertThat(content)
                .as("KP must contain YAML Example (a) section")
                .contains("## YAML Example (a)");
        assertThat(content)
                .as("KP must contain YAML Example (b) section")
                .contains("## YAML Example (b)");
        assertThat(content)
                .as("KP must contain YAML Example (c) section")
                .contains("## YAML Example (c)");
        assertThat(content)
                .as("KP must contain YAML Example (d) section")
                .contains("## YAML Example (d)");
        assertThat(content)
                .as("KP must contain Migration Notes section")
                .contains("## Migration Notes");
    }

    @Test
    @DisplayName("scenario3_rule28IsSlimmedToAtMost60Lines")
    void scenario3_rule28IsSlimmedToAtMost60Lines() throws IOException {
        assertThat(RULE_28).as("Rule 28 must exist").exists();
        long lineCount = Files.lines(RULE_28, StandardCharsets.UTF_8).count();
        assertThat(lineCount)
                .as("Rule 28 must be ≤60 lines but was %d", lineCount)
                .isLessThanOrEqualTo(60);
    }

    @Test
    @DisplayName("scenario4_rule28ContainsInvariantsAndAuditTable")
    void scenario4_rule28ContainsInvariantsAndAuditTable() throws IOException {
        String content = Files.readString(RULE_28, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 28 must contain Invariants section").contains("## Invariants");
        assertThat(content).as("Rule 28 must contain Audit section").contains("## Audit");
        assertThat(content).as("Rule 28 must contain Forbidden section").contains("## Forbidden");
        assertThat(content)
                .as("Rule 28 must reference 6 audit scripts")
                .contains("audit-capability-coverage.sh")
                .contains("audit-frontmatter-schema.sh")
                .contains("audit-capability-graph.sh")
                .contains("audit-fragment-coherence.sh")
                .contains("audit-output-pruning.sh")
                .contains("audit-capability-determinism.sh");
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
