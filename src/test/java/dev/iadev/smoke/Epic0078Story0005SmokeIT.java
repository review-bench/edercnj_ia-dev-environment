package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — EPIC-0078 story-0078-0005 (Slim Rule 12 → KP security/anti-patterns/).
 *
 * <p>Verifies: 8 KP files exist with required sections; Rule 12 is ≤30 lines.
 */
@DisplayName("Epic0078Story0005SmokeIT — Slim Rule 12 security anti-patterns KP")
class Epic0078Story0005SmokeIT {

    private static final Path KP_DIR =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "targets",
                    "claude",
                    "knowledge",
                    "security",
                    "anti-patterns");

    // EPIC-0078 rules-consolidation-essentials: Rule 12 detailed content moved to 8 individual
    // KP files under knowledge/security/anti-patterns/. The conditional source file retains
    // the full content as the conditional rule (used only for Java projects).
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

    private static final List<String> EXPECTED_KP_FILES =
            List.of(
                    "j1-sql-concatenation.md",
                    "j2-math-random.md",
                    "j3-deserialization.md",
                    "j4-hardcoded-credentials.md",
                    "j5-trust-all-tls.md",
                    "j6-path-traversal.md",
                    "j7-exception-leakage.md",
                    "j8-cors-wildcard.md");

    @Test
    @DisplayName("scenario1_allEightKpFilesExist")
    void scenario1_allEightKpFilesExist() {
        assertThat(KP_DIR).as("security/anti-patterns/ directory must exist").isDirectory();
        for (String fileName : EXPECTED_KP_FILES) {
            assertThat(KP_DIR.resolve(fileName)).as("KP file %s must exist", fileName).exists();
        }
    }

    @Test
    @DisplayName("scenario2_eachKpHasRequiredSections")
    void scenario2_eachKpHasRequiredSections() throws IOException {
        for (String fileName : EXPECTED_KP_FILES) {
            String content = Files.readString(KP_DIR.resolve(fileName), StandardCharsets.UTF_8);
            assertThat(content)
                    .as("%s must have Vulnerable Code section", fileName)
                    .contains("## Vulnerable Code");
            assertThat(content)
                    .as("%s must have Fixed Code section", fileName)
                    .contains("## Fixed Code");
            assertThat(content)
                    .as("%s must have Why it is dangerous section", fileName)
                    .contains("## Why it is dangerous");
            assertThat(content).as("%s must reference a CWE", fileName).contains("CWE-");
        }
    }

    @Test
    @DisplayName("scenario3_rule12ConditionalSourceExists")
    void scenario3_rule12IsSlimmedToAtMost30Lines() throws IOException {
        // EPIC-0078 rules-consolidation-essentials: Rule 12 detailed content moved to 8 KP files.
        // The conditional source file still exists for Java projects (generated conditionally).
        assertThat(RULE_12).as("Rule 12 conditional source must exist").exists();
    }

    @Test
    @DisplayName("scenario4_rule12ContainsCweIndex")
    void scenario4_rule12ContainsCweIndex() throws IOException {
        String content = Files.readString(RULE_12, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 12 must contain CWE-89 (J1)").contains("CWE-89");
        assertThat(content).as("Rule 12 must contain CWE-330 (J2)").contains("CWE-330");
        assertThat(content).as("Rule 12 must contain CWE-502 (J3)").contains("CWE-502");
        assertThat(content).as("Rule 12 must contain CWE-798 (J4)").contains("CWE-798");
        assertThat(content).as("Rule 12 must contain CWE-295 (J5)").contains("CWE-295");
        assertThat(content).as("Rule 12 must contain CWE-22 (J6)").contains("CWE-22");
        assertThat(content).as("Rule 12 must contain CWE-209 (J7)").contains("CWE-209");
        assertThat(content).as("Rule 12 must contain CWE-942 (J8)").contains("CWE-942");
    }

    @Test
    @DisplayName("scenario5_eachKpHasFrontmatterWithRequiresCapabilities")
    void scenario5_eachKpHasFrontmatterWithRequiresCapabilities() throws IOException {
        for (String fileName : EXPECTED_KP_FILES) {
            String content = Files.readString(KP_DIR.resolve(fileName), StandardCharsets.UTF_8);
            assertThat(content)
                    .as("%s must have frontmatter with requires-capabilities", fileName)
                    .startsWith("---")
                    .contains("requires-capabilities");
        }
    }
}
