package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Smoke test — EPIC-0078 story-0078-0012 (Create 5 lifecycle KPs under knowledge/lifecycle/). */
@DisplayName("Epic0078Story0012SmokeIT — 5 lifecycle KPs created")
class Epic0078Story0012SmokeIT {

    private static final Path KP_DIR =
            Path.of("src", "main", "resources", "targets", "claude", "knowledge", "lifecycle");

    private static final List<String> EXPECTED_KP_FILES =
            List.of(
                    "backward-compatibility.md",
                    "execution-integrity.md",
                    "zero-bypass.md",
                    "refinement-gate.md",
                    "ci-watch-integrity.md");

    @Test
    @DisplayName("scenario1_allFiveKpFilesExist")
    void scenario1_allFiveKpFilesExist() {
        assertThat(KP_DIR).as("knowledge/lifecycle/ directory must exist").isDirectory();
        for (String fileName : EXPECTED_KP_FILES) {
            assertThat(KP_DIR.resolve(fileName)).as("KP file %s must exist", fileName).exists();
        }
    }

    @Test
    @DisplayName("scenario2_eachKpHasFrontmatterWithRequiresCapabilities")
    void scenario2_eachKpHasFrontmatterWithRequiresCapabilities() throws IOException {
        for (String fileName : EXPECTED_KP_FILES) {
            String content = Files.readString(KP_DIR.resolve(fileName), StandardCharsets.UTF_8);
            assertThat(content)
                    .as("%s must start with frontmatter delimiter", fileName)
                    .startsWith("---");
            assertThat(content)
                    .as("%s must have requires-capabilities field", fileName)
                    .contains("requires-capabilities");
        }
    }

    @Test
    @DisplayName("scenario3_backwardCompatibilityKpContainsFlowVersionMatrix")
    void scenario3_backwardCompatibilityKpContainsFlowVersionMatrix() throws IOException {
        String content =
                Files.readString(
                        KP_DIR.resolve("backward-compatibility.md"), StandardCharsets.UTF_8);
        assertThat(content)
                .as("backward-compatibility KP must contain flowVersion Fallback Matrix")
                .contains("## `flowVersion` Fallback Matrix");
        assertThat(content)
                .as("backward-compatibility KP must contain taskTracking Fallback Matrix")
                .contains("## `taskTracking` Fallback Matrix");
        assertThat(content)
                .as("backward-compatibility KP must contain refinementVerdict Fallback Matrix")
                .contains("## `refinementVerdict` Fallback Matrix");
    }

    @Test
    @DisplayName("scenario4_executionIntegrityKpContainsMandatoryArtifacts")
    void scenario4_executionIntegrityKpContainsMandatoryArtifacts() throws IOException {
        String content =
                Files.readString(KP_DIR.resolve("execution-integrity.md"), StandardCharsets.UTF_8);
        assertThat(content)
                .as("execution-integrity KP must contain Mandatory Evidence Artifacts")
                .contains("## Mandatory Evidence Artifacts");
        assertThat(content)
                .as("execution-integrity KP must reference x-internal-verify-story")
                .contains("x-internal-verify-story");
        assertThat(content)
                .as("execution-integrity KP must reference x-review-codebase")
                .contains("x-review-codebase");
    }

    @Test
    @DisplayName("scenario5_zeroBypassKpContains13Surfaces")
    void scenario5_zeroBypassKpContains13Surfaces() throws IOException {
        String content = Files.readString(KP_DIR.resolve("zero-bypass.md"), StandardCharsets.UTF_8);
        assertThat(content)
                .as("zero-bypass KP must contain 13 Orchestration Surfaces")
                .contains("## 13 Orchestration Surfaces");
        assertThat(content).as("zero-bypass KP must have 13 surface entries").contains("| 13 |");
    }

    @Test
    @DisplayName("scenario6_refinementGateKpContainsStateMachineAndDimensions")
    void scenario6_refinementGateKpContainsStateMachineAndDimensions() throws IOException {
        String content =
                Files.readString(KP_DIR.resolve("refinement-gate.md"), StandardCharsets.UTF_8);
        assertThat(content)
                .as("refinement-gate KP must contain State Machine")
                .contains("## State Machine");
        assertThat(content)
                .as("refinement-gate KP must contain Story Dimensions")
                .contains("## Story Dimensions");
        assertThat(content)
                .as("refinement-gate KP must contain refinementVerdict JSON Shape")
                .contains("refinementVerdict");
    }

    @Test
    @DisplayName("scenario7_ciWatchKpContains8ExitCodes")
    void scenario7_ciWatchKpContains8ExitCodes() throws IOException {
        String content =
                Files.readString(KP_DIR.resolve("ci-watch-integrity.md"), StandardCharsets.UTF_8);
        assertThat(content)
                .as("ci-watch-integrity KP must contain Exit Codes Matrix")
                .contains("## Exit Codes Matrix");
        assertThat(content)
                .as("ci-watch-integrity KP must reference PR_ALREADY_MERGED")
                .contains("PR_ALREADY_MERGED");
        assertThat(content)
                .as("ci-watch-integrity KP must reference NO_CI_CONFIGURED")
                .contains("NO_CI_CONFIGURED");
        assertThat(content)
                .as("ci-watch-integrity KP must contain Fallback Matrix")
                .contains("## Fallback Matrix");
    }
}
