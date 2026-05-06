package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityId;
import dev.iadev.domain.capability.ResolvedCapabilitySet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("CapabilityAwareComposer")
class CapabilityAwareComposerTest {

    private final CapabilityAwareComposer composer = new CapabilityAwareComposer();
    private final CapabilityAwareComposer hardComposer =
            new CapabilityAwareComposer(CapabilityAwareComposer.PruningMode.HARD);

    private static ResolvedCapabilitySet activeSet(List<String> capIds) {
        List<CapabilityId> ids = capIds.stream().map(CapabilityId::of).toList();
        return new ResolvedCapabilitySet("test-profile", ids, Map.of(), List.of());
    }

    private static Path writeArtifact(Path dir, String name, String... capIds) throws IOException {
        Path file = dir.resolve(name);
        Files.createDirectories(file.getParent());
        String capList =
                capIds.length == 0
                        ? "[]"
                        : "\n"
                                + String.join(
                                        "\n",
                                        java.util.Arrays.stream(capIds)
                                                .map(c -> "  - " + c)
                                                .toList());
        Files.writeString(
                file,
                "---\nname: "
                        + name.replace(".md", "").replace("/", "-")
                        + "\nrequires-capabilities:"
                        + capList
                        + "\n---\n# Content\n");
        return file;
    }

    @Nested
    @DisplayName("execute()")
    class ExecuteMethod {

        @Test
        @DisplayName("execute writes included artifacts to outputRoot")
        void executesWritesArtifacts(@TempDir Path targets, @TempDir Path output)
                throws IOException {
            writeArtifact(targets, "skills/skill.md");
            ResolvedCapabilitySet active = activeSet(List.of());
            CompositionPlan plan = composer.plan(active, targets);
            composer.execute(plan, output);
            assertThat(Files.exists(output.resolve("skills/skill.md"))).isTrue();
        }

        @Test
        @DisplayName("execute with empty plan creates no files")
        void executesEmptyPlanNoFiles(@TempDir Path output) throws IOException {
            composer.execute(CompositionPlan.empty(), output);
            assertThat(
                            java.util.stream.StreamSupport.stream(
                                            java.nio.file.Files.walk(output).spliterator(), false)
                                    .filter(Files::isRegularFile)
                                    .count())
                    .isZero();
        }
    }

    @Nested
    @DisplayName("totalArtifacts() and toSummary()")
    class PlanHelpers {

        @Test
        @DisplayName("totalArtifacts sums included + excluded")
        void totalArtifacts(@TempDir Path targets) throws IOException {
            writeArtifact(targets, "skills/incl.md");
            writeArtifact(targets, "skills/excl.md", "data.database.postgres");
            CompositionPlan plan = composer.plan(activeSet(List.of()), targets);
            assertThat(plan.totalArtifacts()).isEqualTo(2);
        }

        @Test
        @DisplayName("toSummary contains counts")
        void toSummary(@TempDir Path targets) throws IOException {
            writeArtifact(targets, "skill.md");
            CompositionPlan plan = composer.plan(activeSet(List.of()), targets);
            assertThat(plan.toSummary()).contains("included=1");
        }

        @Test
        @DisplayName("non-existent targets root returns empty plan")
        void missingTargetsRoot() {
            CompositionPlan plan = composer.plan(activeSet(List.of()), Path.of("/does/not/exist"));
            assertThat(plan).isEqualTo(CompositionPlan.empty());
        }
    }

    @Nested
    @DisplayName("plan()")
    class PlanMethod {

        @Test
        @DisplayName("universal artifact (requires-capabilities: []) is always included (happy)")
        void universalArtifactIncluded(@TempDir Path root) throws IOException {
            writeArtifact(root, "universal.md");
            ResolvedCapabilitySet active = activeSet(List.of());
            CompositionPlan plan = composer.plan(active, root);
            assertThat(plan.included()).hasSize(1);
            assertThat(plan.excluded()).isEmpty();
        }

        @Test
        @DisplayName("artifact with matching capability is included")
        void matchingCapabilityIncluded(@TempDir Path root) throws IOException {
            writeArtifact(root, "spring-skill.md", "framework.spring-boot.mvc");
            ResolvedCapabilitySet active = activeSet(List.of("framework.spring-boot.mvc"));
            CompositionPlan plan = composer.plan(active, root);
            assertThat(plan.included()).hasSize(1);
        }

        @Test
        @DisplayName("artifact with non-matching capability is excluded in HARD mode")
        void nonMatchingCapabilityExcludedHard(@TempDir Path root) throws IOException {
            writeArtifact(root, "postgres-skill.md", "data.database.postgres");
            ResolvedCapabilitySet active = activeSet(List.of("framework.spring-boot.mvc"));
            CompositionPlan plan = hardComposer.plan(active, root);
            assertThat(plan.excluded()).hasSize(1);
            assertThat(plan.excluded().get(0).excludeReason()).isNotNull();
        }

        @Test
        @DisplayName("artifact with non-matching capability emits warning in ADVISORY mode")
        void nonMatchingCapabilityAdvisoryWarning(@TempDir Path root) throws IOException {
            CapabilityAwareComposer advisory =
                    new CapabilityAwareComposer(CapabilityAwareComposer.PruningMode.ADVISORY);
            writeArtifact(root, "java-rule.md", "lang.java.*");
            ResolvedCapabilitySet active = activeSet(List.of());
            CompositionPlan plan = advisory.plan(active, root);
            assertThat(plan.included()).hasSize(1);
            assertThat(plan.excluded()).isEmpty();
            assertThat(plan.warnings()).hasSize(1);
            assertThat(plan.warnings().get(0)).contains("rule-pruning-advisory");
        }

        @Test
        @DisplayName("empty targets root returns empty plan")
        void emptyRootReturnsEmpty(@TempDir Path root) {
            ResolvedCapabilitySet active = activeSet(List.of());
            CompositionPlan plan = composer.plan(active, root);
            assertThat(plan.included()).isEmpty();
            assertThat(plan.excluded()).isEmpty();
        }

        @Test
        @DisplayName("plan is deterministic across 3 invocations (RULE-004)")
        void planIsDeterministic(@TempDir Path root) throws IOException {
            writeArtifact(root, "artifact-a.md");
            writeArtifact(root, "artifact-b.md", "data.database.postgres");
            ResolvedCapabilitySet active = activeSet(List.of("data.database.postgres"));

            CompositionPlan p1 = composer.plan(active, root);
            CompositionPlan p2 = composer.plan(active, root);
            CompositionPlan p3 = composer.plan(active, root);

            assertThat(
                            p1.included().stream()
                                    .map(CompositionPlan.ArtifactEntry::relativePath)
                                    .toList())
                    .isEqualTo(
                            p2.included().stream()
                                    .map(CompositionPlan.ArtifactEntry::relativePath)
                                    .toList())
                    .isEqualTo(
                            p3.included().stream()
                                    .map(CompositionPlan.ArtifactEntry::relativePath)
                                    .toList());
        }
    }
}
