package dev.iadev.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for migrate-to-local-first.sh.tpl contract (RULE-011 — EPIC-0061).
 *
 * <p>Validates the structural contract of the migration template:
 *
 * <ul>
 *   <li>Template exists under _default/ stack directory
 *   <li>Template has required modes (--dry-run, --apply, --self-check)
 *   <li>Template has stack detection heuristic
 *   <li>Template is idempotent by design (checks before writing)
 * </ul>
 */
@DisplayName("MigrateToLocalFirstSmokeIT — migration script contract")
class MigrateToLocalFirstSmokeIT {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir")).getParent();

    private static final Path MIGRATE_TEMPLATE = REPO_ROOT.resolve(
            "java/src/main/resources/targets/claude/scripts/_default/migrate-to-local-first.sh.tpl");

    @Test
    @DisplayName("migrate-to-local-first.sh.tpl exists in _default stack")
    void migrateTemplate_existsInDefaultStack() {
        assertThat(MIGRATE_TEMPLATE.toFile())
                .as("migrate-to-local-first.sh.tpl must exist for _default stack (RULE-011)")
                .exists();
    }

    @Test
    @DisplayName("migration template supports --dry-run mode")
    void migrateTemplate_supportsDryRun() throws IOException {
        String content = Files.readString(MIGRATE_TEMPLATE);

        assertThat(content)
                .as("Template must have --dry-run mode")
                .contains("--dry-run");
        assertThat(content)
                .as("dry-run must not write files")
                .contains("dry-run") // the mode
                .contains("No files written"); // the message
    }

    @Test
    @DisplayName("migration template supports --apply mode")
    void migrateTemplate_supportsApply() throws IOException {
        String content = Files.readString(MIGRATE_TEMPLATE);

        assertThat(content).contains("--apply");
        assertThat(content).contains("--self-check");
    }

    @Test
    @DisplayName("migration template detects stack from filesystem")
    void migrateTemplate_detectsStackHeuristic() throws IOException {
        String content = Files.readString(MIGRATE_TEMPLATE);

        assertThat(content)
                .as("Template must detect stack from pom.xml, package.json, go.mod")
                .contains("pom.xml")
                .contains("package.json")
                .contains("go.mod");
    }

    @Test
    @DisplayName("migration template is idempotent — checks before writing")
    void migrateTemplate_isIdempotent() throws IOException {
        String content = Files.readString(MIGRATE_TEMPLATE);

        assertThat(content)
                .as("Template must check if file exists before writing (idempotency per RULE-011)")
                .contains("! -f");
    }

    @Test
    @DisplayName("Rule 19 fallback matrix contains flowVersion 3 entry")
    void rule19_containsFlowVersion3() throws IOException {
        Path rule19 = REPO_ROOT.resolve(
                "java/src/main/resources/targets/claude/rules/19-backward-compatibility.md");

        String content = Files.readString(rule19);

        assertThat(content)
                .as("Rule 19 fallback matrix must have entry for flowVersion '3'")
                .contains("\"3\"");
        assertThat(content)
                .as("flowVersion 3 description must mention Local-First or EPIC-0061")
                .containsPattern("3.*EPIC-0061|3.*Local-First|3.*local-first");
    }
}
