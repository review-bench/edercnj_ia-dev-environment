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
 * Smoke test — EPIC-0078 story-0078-0011 (Rewrite Rule 19 as unified Lifecycle Integrity Contract).
 */
@DisplayName("Epic0078Story0011SmokeIT — Rule 19 as Lifecycle Integrity Contract")
class Epic0078Story0011SmokeIT {

    private static final Path RULE_19 = Path.of(
            "src", "main", "resources", "targets", "claude",
            "rules", "19-backward-compatibility.md");

    private static final List<Path> LIFECYCLE_KPS = List.of(
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle", "backward-compatibility.md"),
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle", "execution-integrity.md"),
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle", "zero-bypass.md"),
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle", "refinement-gate.md"),
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle", "ci-watch-integrity.md")
    );

    @Test
    @DisplayName("scenario1_rule19IsSlimmedToAtMost80Lines")
    void scenario1_rule19IsSlimmedToAtMost80Lines() throws IOException {
        assertThat(RULE_19).as("Rule 19 must exist").exists();
        long lineCount = Files.lines(RULE_19, StandardCharsets.UTF_8).count();
        assertThat(lineCount)
                .as("Rule 19 must be ≤80 lines but was %d", lineCount)
                .isLessThanOrEqualTo(80);
    }

    @Test
    @DisplayName("scenario2_rule19ContainsCanonicalSections")
    void scenario2_rule19ContainsCanonicalSections() throws IOException {
        String content = Files.readString(RULE_19, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 19 must contain flowVersion Quick Reference section")
                .contains("## `flowVersion` (Quick Reference)");
        assertThat(content).as("Rule 19 must contain Invariants section")
                .contains("## Invariants");
        assertThat(content).as("Rule 19 must contain Enforcement Layers section")
                .contains("## Enforcement Layers");
        assertThat(content).as("Rule 19 must contain Bypass Exceptions section")
                .contains("## Bypass Exceptions");
        assertThat(content).as("Rule 19 must contain Forbidden section")
                .contains("## Forbidden");
    }

    @Test
    @DisplayName("scenario3_rule19References5LifecycleKps")
    void scenario3_rule19References5LifecycleKps() throws IOException {
        String content = Files.readString(RULE_19, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 19 must reference backward-compatibility KP")
                .contains("lifecycle/backward-compatibility.md");
        assertThat(content).as("Rule 19 must reference execution-integrity KP")
                .contains("lifecycle/execution-integrity.md");
        assertThat(content).as("Rule 19 must reference zero-bypass KP")
                .contains("lifecycle/zero-bypass.md");
        assertThat(content).as("Rule 19 must reference refinement-gate KP")
                .contains("lifecycle/refinement-gate.md");
        assertThat(content).as("Rule 19 must reference ci-watch-integrity KP")
                .contains("lifecycle/ci-watch-integrity.md");
    }

    @Test
    @DisplayName("scenario4_allFiveLifecycleKpsExist")
    void scenario4_allFiveLifecycleKpsExist() {
        for (Path kp : LIFECYCLE_KPS) {
            assertThat(kp).as("Lifecycle KP %s must exist", kp.getFileName()).exists();
        }
    }

    @Test
    @DisplayName("scenario5_rule19Contains5InvariantsAnd5Layers")
    void scenario5_rule19Contains5InvariantsAnd5Layers() throws IOException {
        String content = Files.readString(RULE_19, StandardCharsets.UTF_8);
        assertThat(content).as("Rule 19 must contain TASK_TRACKING_REQUIRED reference")
                .contains("TASK_TRACKING_REQUIRED");
        assertThat(content).as("Rule 19 must contain REFINEMENT_REQUIRED reference")
                .contains("REFINEMENT_REQUIRED");
        assertThat(content).as("Rule 19 must contain EIE_EVIDENCE_MISSING reference")
                .contains("EIE_EVIDENCE_MISSING");
        assertThat(content).as("Rule 19 must reference Camada 0 (preventive)")
                .contains("0");
    }
}
