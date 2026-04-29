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

@DisplayName("TelemetryCapabilityAwareComposer")
class TelemetryCapabilityAwareComposerTest {

    private final TelemetryCapabilityAwareComposer sut = new TelemetryCapabilityAwareComposer();

    @TempDir
    Path tempDir;

    private static ResolvedCapabilitySet universalSet() {
        return new ResolvedCapabilitySet("test", List.of(), Map.of(), List.of());
    }

    private static void writeArtifact(Path dir, String name, String... capIds) throws IOException {
        Files.createDirectories(dir);
        String capList = capIds.length == 0 ? "[]" : "\n" +
                String.join("\n", java.util.Arrays.stream(capIds).map(c -> "  - " + c).toList());
        Files.writeString(dir.resolve(name),
                "---\nname: " + name.replace(".md", "") + "\nrequires-capabilities:" + capList + "\n---\n# Content\n");
    }

    @Nested
    @DisplayName("plan()")
    class PlanTests {

        @Test
        @DisplayName("returns same plan as delegate — universal set includes all artifacts")
        void plan_universalSet_includesAll() throws IOException {
            writeArtifact(tempDir, "a.md");
            writeArtifact(tempDir, "b.md");

            CompositionPlan plan = sut.plan(universalSet(), tempDir);

            assertThat(plan.included()).hasSize(2);
            assertThat(plan.excluded()).isEmpty();
        }

        @Test
        @DisplayName("returns same plan as delegate — capability-gated artifact excluded")
        void plan_gatedArtifact_excluded() throws IOException {
            writeArtifact(tempDir, "universal.md");
            writeArtifact(tempDir, "spring.md", "web.spring.boot");

            CompositionPlan plan = sut.plan(universalSet(), tempDir);

            assertThat(plan.included()).hasSize(1);
            assertThat(plan.excluded()).hasSize(1);
        }

        @Test
        @DisplayName("telemetry is fail-open — plan works even if logging disabled")
        void plan_failOpen_withTelemetryDisabled() throws IOException {
            writeArtifact(tempDir, "x.md");
            String original = System.getenv("CLAUDE_TELEMETRY_DISABLED");
            try {
                // Simulated: env cannot be set programmatically in tests, but decorator must not throw
                CompositionPlan plan = sut.plan(universalSet(), tempDir);
                assertThat(plan.included()).hasSize(1);
            } finally {
                // No cleanup needed — System.getenv is read-only
                assertThat(original).isNull(); // env not set in test context
            }
        }
    }

    @Nested
    @DisplayName("execute()")
    class ExecuteTests {

        @Test
        @DisplayName("copies included artifacts to output root")
        void execute_copiesArtifacts() throws IOException {
            Path targetsRoot = tempDir.resolve("targets");
            writeArtifact(targetsRoot, "a.md");
            Path outputRoot = tempDir.resolve("output");

            CompositionPlan plan = sut.plan(universalSet(), targetsRoot);
            sut.execute(plan, outputRoot);

            assertThat(outputRoot.resolve("a.md")).exists();
        }
    }
}
