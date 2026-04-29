package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.application.composition.CapabilityAwareComposer;
import dev.iadev.application.composition.CompositionPlan;
import dev.iadev.application.composition.CompositionPlanReporter;
import dev.iadev.application.composition.OutputPruner;
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

/**
 * E2E integration smoke test for the EPIC-0064 capability composer pipeline (story-0064-0310).
 *
 * <p>Validates: resolver → composer → pruner → reporter chain, determinism, and pruning correctness.
 */
@DisplayName("Epic0064ComposerIntegrationSmokeTest")
class Epic0064ComposerIntegrationSmokeTest {

    private final CapabilityAwareComposer composer = new CapabilityAwareComposer();
    private final OutputPruner pruner = new OutputPruner();
    private final CompositionPlanReporter reporter = new CompositionPlanReporter();

    private static ResolvedCapabilitySet springActiveSet() {
        return new ResolvedCapabilitySet("spring-rest-postgres", List.of(
                CapabilityId.of("framework.spring-boot.mvc"),
                CapabilityId.of("data.database.postgres"),
                CapabilityId.of("runtime.jvm.openjdk")
        ), Map.of(), List.of());
    }

    private static void writeArtifact(Path dir, String rel, String... caps) throws IOException {
        Path file = dir.resolve(rel);
        Files.createDirectories(file.getParent());
        String capList = caps.length == 0 ? "[]" : "\n" + String.join("\n",
                java.util.Arrays.stream(caps).map(c -> "  - " + c).toList());
        Files.writeString(file, "---\nname: " + rel.replace("/SKILL.md", "").replace("/", "-")
                + "\nrequires-capabilities:" + capList + "\n---\n# Content\n");
    }

    @Nested
    @DisplayName("E2E pipeline — Spring REST + Postgres profile")
    class SpringProfile {

        @Test
        @DisplayName("composer includes spring + postgres artifacts, excludes quarkus-only (happy)")
        void composerFiltersCorrectly(@TempDir Path targets, @TempDir Path output) throws IOException {
            writeArtifact(targets, "skills/x-universal/SKILL.md");
            writeArtifact(targets, "skills/x-spring-ctrl/SKILL.md", "framework.spring-boot.mvc");
            writeArtifact(targets, "skills/x-quarkus-res/SKILL.md", "framework.quarkus.rest");
            writeArtifact(targets, "skills/x-pg-helper/SKILL.md", "data.database.postgres");

            CompositionPlan plan = composer.plan(springActiveSet(), targets);
            assertThat(plan.included()).hasSize(3);
            assertThat(plan.excluded()).hasSize(1);
            assertThat(plan.excluded().get(0).relativePath()).contains("quarkus");
        }

        @Test
        @DisplayName("output pruner writes only included artifacts")
        void prunerWritesIncluded(@TempDir Path targets, @TempDir Path output) throws IOException {
            writeArtifact(targets, "skills/x-universal/SKILL.md");
            writeArtifact(targets, "skills/x-quarkus-res/SKILL.md", "framework.quarkus.rest");

            CompositionPlan plan = composer.plan(springActiveSet(), targets);
            var result = pruner.execute(plan, output);

            assertThat(result.written()).isEqualTo(1);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(Files.exists(output.resolve("skills/x-universal/SKILL.md"))).isTrue();
            assertThat(Files.exists(output.resolve("skills/x-quarkus-res/SKILL.md"))).isFalse();
        }

        @Test
        @DisplayName("3 consecutive plans are deterministic — same included/excluded order (RULE-004)")
        void planIsDeterministic(@TempDir Path targets) throws IOException {
            writeArtifact(targets, "skills/x-a/SKILL.md", "framework.spring-boot.mvc");
            writeArtifact(targets, "skills/x-b/SKILL.md");
            writeArtifact(targets, "skills/x-c/SKILL.md", "data.database.postgres");

            CompositionPlan p1 = composer.plan(springActiveSet(), targets);
            CompositionPlan p2 = composer.plan(springActiveSet(), targets);
            CompositionPlan p3 = composer.plan(springActiveSet(), targets);

            List<String> paths1 = p1.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();
            List<String> paths2 = p2.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();
            List<String> paths3 = p3.included().stream().map(CompositionPlan.ArtifactEntry::relativePath).toList();
            assertThat(paths1).isEqualTo(paths2).isEqualTo(paths3);
        }
    }

    @Nested
    @DisplayName("reporter — dry-run output")
    class DryRunOutput {

        @Test
        @DisplayName("text report lists included/excluded counts")
        void textReport(@TempDir Path targets) throws IOException {
            writeArtifact(targets, "skills/x-spring/SKILL.md", "framework.spring-boot.mvc");
            writeArtifact(targets, "skills/x-quarkus/SKILL.md", "framework.quarkus.rest");

            CompositionPlan plan = composer.plan(springActiveSet(), targets);
            String report = reporter.report(plan, CompositionPlanReporter.Format.TEXT);
            assertThat(report).contains("Included artifacts: 1");
            assertThat(report).contains("Excluded artifacts: 1");
        }

        @Test
        @DisplayName("JSON report is valid JSON with numeric counts")
        void jsonReport(@TempDir Path targets) throws IOException {
            writeArtifact(targets, "skills/x-spring/SKILL.md", "framework.spring-boot.mvc");
            CompositionPlan plan = composer.plan(springActiveSet(), targets);
            String json = reporter.report(plan, CompositionPlanReporter.Format.JSON);
            assertThat(json).contains("\"included\": 1").contains("\"excluded\": 0");
        }
    }
}
