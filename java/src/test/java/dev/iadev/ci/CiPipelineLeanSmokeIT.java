package dev.iadev.ci;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test verifying the lean CI pipeline contract (RULE-007, RULE-008 — EPIC-0061
 * story-0061-0005).
 *
 * <p>After story-0061-0005 merges:
 *
 * <ul>
 *   <li>No {@code audit-*.sh} files remain in {@code scripts/} root (RULE-007)
 *   <li>{@code .github/workflows/audit.yml} is absent (RULE-008)
 * </ul>
 */
@DisplayName("CiPipelineLeanSmokeIT — lean CI contract verification")
class CiPipelineLeanSmokeIT {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir")).getParent();

    @Test
    @DisplayName("scripts/ root has no audit-*.sh files (RULE-007)")
    void scriptsRoot_hasNoAuditShFiles() throws Exception {
        Path scriptsDir = REPO_ROOT.resolve("scripts");

        if (!Files.isDirectory(scriptsDir)) {
            return;
        }

        List<Path> auditShFiles =
                Files.list(scriptsDir)
                        .filter(
                                p -> {
                                    String name = p.getFileName().toString();
                                    return name.startsWith("audit-") && name.endsWith(".sh");
                                })
                        .toList();

        assertThat(auditShFiles)
                .as(
                        "scripts/ root must not contain audit-*.sh files (RULE-007: bash audits live only in targets/claude/scripts/{stack}/)")
                .isEmpty();
    }

    @Test
    @DisplayName(".github/workflows/audit.yml is absent (RULE-008)")
    void auditYml_isAbsent() {
        Path auditYml = REPO_ROOT.resolve(".github/workflows/audit.yml");

        assertThat(auditYml.toFile())
                .as(
                        "audit.yml workflow must be deleted (RULE-008: CI runs only via ci.yml + mvn verify)")
                .doesNotExist();
    }

    @Test
    @DisplayName("targets/claude/scripts/ has java-maven stack templates (replacement exists)")
    void scriptTemplates_existForJavaMavenStack() throws Exception {
        Path stackDir =
                REPO_ROOT.resolve("java/src/main/resources/targets/claude/scripts/java-maven");

        assertThat(stackDir.toFile())
                .as(
                        "java-maven stack template directory must exist as replacement for root audit scripts")
                .isDirectory();

        List<Path> templates =
                Files.list(stackDir)
                        .filter(p -> p.getFileName().toString().endsWith(".sh.tpl"))
                        .toList();

        assertThat(templates)
                .as("java-maven stack must have at least 8 .sh.tpl templates")
                .hasSizeGreaterThanOrEqualTo(8);
    }
}
