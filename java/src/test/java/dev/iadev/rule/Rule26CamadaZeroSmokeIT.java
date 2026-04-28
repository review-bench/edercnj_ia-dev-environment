package dev.iadev.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test verifying the Camada 0 (Local Hooks Preventivos) contract from Rule 26 (EPIC-0061
 * story-0061-0006 TASK-0061-0006-004).
 *
 * <p>Validates:
 *
 * <ol>
 *   <li>Rule 26 source file contains {@code ## Camada 0} section (RULE-006)
 *   <li>ADR-0017 is referenced from Rule 26
 *   <li>All hook files ({@code verify-*.sh}, {@code enforce-*.sh}) have Camada 0 header
 * </ol>
 */
@DisplayName("Rule26CamadaZeroSmokeIT — Camada 0 contract verification")
class Rule26CamadaZeroSmokeIT {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir")).getParent();

    private static final Path RULE_26_PATH =
            REPO_ROOT.resolve(
                    "java/src/main/resources/targets/claude/rules/26-audit-gate-lifecycle.md");
    private static final Path HOOKS_DIR =
            REPO_ROOT.resolve("java/src/main/resources/targets/claude/hooks");
    private static final Path ADR_DIR = REPO_ROOT.resolve("adr");

    @Test
    @DisplayName("Rule 26 contains ## Camada 0 section (RULE-006)")
    void rule26_containsCamadaZeroSection() throws IOException {
        String content = Files.readString(RULE_26_PATH);

        assertThat(content)
                .as("Rule 26 must have ## Camada 0 — Local Hooks Preventivos section")
                .contains("## Camada 0 — Local Hooks Preventivos");
    }

    @Test
    @DisplayName("Rule 26 taxonomy table has 5 layers")
    void rule26_taxonomyHasFiveLayers() throws IOException {
        String content = Files.readString(RULE_26_PATH);

        long layerRows = content.lines().filter(line -> line.matches("^\\| \\*\\*[0-9].*")).count();

        assertThat(layerRows)
                .as("Rule 26 taxonomy must define 5 layers (Camada 0-4)")
                .isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("Rule 26 references ADR-0017")
    void rule26_referencesAdr0017() throws IOException {
        String content = Files.readString(RULE_26_PATH);

        assertThat(content).as("Rule 26 must reference ADR-0017").contains("ADR-0017");
    }

    @Test
    @DisplayName("ADR-0017 exists and has Accepted status")
    void adr0017_existsWithAcceptedStatus() throws IOException {
        Path adr17 = ADR_DIR.resolve("ADR-0017-local-first-lifecycle.md");

        assertThat(adr17.toFile()).as("ADR-0017-local-first-lifecycle.md must exist").exists();

        String content = Files.readString(adr17);
        assertThat(content).as("ADR-0017 must have Accepted status").contains("Accepted");
        assertThat(content).as("ADR-0017 must reference Rule 26").contains("Rule 26");
    }

    @Test
    @DisplayName("All verify-*.sh and enforce-*.sh hooks have Camada 0 contract header")
    void allHooks_haveCamadaZeroHeader() throws IOException {
        if (!Files.isDirectory(HOOKS_DIR)) {
            return;
        }

        List<Path> hookFiles =
                Files.list(HOOKS_DIR)
                        .filter(
                                p -> {
                                    String name = p.getFileName().toString();
                                    return (name.startsWith("verify-")
                                                    || name.startsWith("enforce-"))
                                            && name.endsWith(".sh");
                                })
                        .sorted()
                        .toList();

        assertThat(hookFiles)
                .as("At least 2 hook files must exist (verify-*.sh or enforce-*.sh)")
                .hasSizeGreaterThanOrEqualTo(2);

        List<String> missingHeader =
                hookFiles.stream()
                        .filter(
                                hook -> {
                                    try {
                                        String content = Files.readString(hook);
                                        return !content.contains("Layer:")
                                                || !content.contains("Exit codes:");
                                    } catch (IOException e) {
                                        return true;
                                    }
                                })
                        .map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());

        assertThat(missingHeader)
                .as(
                        "These hooks are missing Camada 0 contract header (Layer: / Exit codes:): %s",
                        missingHeader)
                .isEmpty();
    }
}
