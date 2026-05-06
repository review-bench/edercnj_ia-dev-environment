package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0009 (Slim Rule 30 → KP governance/tool-call-grammar.md).
 */
@DisplayName("Epic0078Story0009SmokeIT — Slim Rule 30 tool-call-grammar KP")
class Epic0078Story0009SmokeIT {

    private static final Path KP_FILE = Path.of(
            "src", "main", "resources", "targets", "claude",
            "knowledge", "governance", "tool-call-grammar.md");

    private static final Path RULE_30 = Path.of(
            "src", "main", "resources", "targets", "claude",
            "rules", "30-tool-call-grammar.md");

    @Test
    @DisplayName("scenario1_kpFileExists")
    void scenario1_kpFileExists() {
        assertThat(KP_FILE).as("governance/tool-call-grammar.md must exist").exists();
    }

    @Test
    @DisplayName("scenario2_kpContainsRequiredSections")
    void scenario2_kpContainsRequiredSections() throws IOException {
        String content = Files.readString(KP_FILE, StandardCharsets.UTF_8);
        assertThat(content).as("KP must contain BNF Grammar section")
                .contains("## BNF Grammar");
        assertThat(content).as("KP must contain Conditional Expression Whitelist section")
                .contains("## Conditional Expression Whitelist");
        assertThat(content).as("KP must contain Static Check Algorithm section")
                .contains("## Static Check Algorithm");
        assertThat(content).as("KP must contain Dynamic Check Algorithm section")
                .contains("## Dynamic Check Algorithm");
        assertThat(content).as("KP must contain BNF marker production rule")
                .contains("marker        ::=");
    }

    @Test
    @DisplayName("scenario3_rule30IsSlimmedToAtMost50Lines")
    void scenario3_rule30IsSlimmedToAtMost50Lines() throws IOException {
        assertThat(RULE_30).as("Rule 30 must exist").exists();
        long lineCount = Files.lines(RULE_30, StandardCharsets.UTF_8).count();
        assertThat(lineCount)
                .as("Rule 30 must be ≤50 lines but was %d", lineCount)
                .isLessThanOrEqualTo(50);
    }

    @Test
    @DisplayName("scenario4_rule30ContainsMarkerKindsAndExitCodes")
    void scenario4_rule30ContainsMarkerKindsAndExitCodes() throws IOException {
        String content = Files.readString(RULE_30, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 30 must contain Marker Kinds section").contains("## Marker Kinds");
        assertThat(content).as("Rule 30 must contain Exit Codes section").contains("## Exit Codes");
        assertThat(content).as("Rule 30 must contain Forbidden section").contains("## Forbidden");
        assertThat(content).as("Rule 30 must reference [required] marker").contains("[required]");
        assertThat(content).as("Rule 30 must reference [optional] marker").contains("[optional]");
        assertThat(content).as("Rule 30 must reference [conditional] marker").contains("[conditional");
    }

    @Test
    @DisplayName("scenario5_kpHasFrontmatterWithRequiresCapabilities")
    void scenario5_kpHasFrontmatterWithRequiresCapabilities() throws IOException {
        String content = Files.readString(KP_FILE, StandardCharsets.UTF_8);
        assertThat(content).as("KP must start with frontmatter delimiter").startsWith("---");
        assertThat(content).as("KP must have requires-capabilities").contains("requires-capabilities");
    }
}
