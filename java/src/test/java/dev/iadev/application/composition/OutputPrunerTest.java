package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("OutputPruner")
class OutputPrunerTest {

    private final OutputPruner pruner = new OutputPruner();

    private static CompositionPlan.ArtifactEntry included(Path source, String rel) {
        return new CompositionPlan.ArtifactEntry(source, rel);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("empty plan writes nothing and returns 0 written (degenerate)")
        void emptyPlanWritesNothing(@TempDir Path output) throws IOException {
            var result = pruner.execute(CompositionPlan.empty(), output);
            assertThat(result.written()).isZero();
            assertThat(result.skipped()).isZero();
        }

        @Test
        @DisplayName("writes included artifacts to output root")
        void writesIncludedArtifacts(@TempDir Path source, @TempDir Path output) throws IOException {
            Path src = source.resolve("skill.md");
            Files.writeString(src, "# Content");
            var plan = new CompositionPlan(
                    List.of(included(src, "skills/skill.md")),
                    List.of(), List.of());
            var result = pruner.execute(plan, output);
            assertThat(result.written()).isEqualTo(1);
            assertThat(Files.exists(output.resolve("skills/skill.md"))).isTrue();
            assertThat(Files.readString(output.resolve("skills/skill.md"))).isEqualTo("# Content");
        }

        @Test
        @DisplayName("creates parent directories as needed")
        void createsParentDirs(@TempDir Path source, @TempDir Path output) throws IOException {
            Path src = source.resolve("file.md");
            Files.writeString(src, "content");
            var plan = new CompositionPlan(
                    List.of(included(src, "a/b/c/file.md")),
                    List.of(), List.of());
            pruner.execute(plan, output);
            assertThat(Files.exists(output.resolve("a/b/c/file.md"))).isTrue();
        }

        @Test
        @DisplayName("returns excluded count as skipped")
        void excludedCountInSkipped(@TempDir Path source, @TempDir Path output) throws IOException {
            var plan = new CompositionPlan(
                    List.of(),
                    List.of(new CompositionPlan.ArtifactEntry(source.resolve("x.md"), "x.md", "no match")),
                    List.of());
            var result = pruner.execute(plan, output);
            assertThat(result.skipped()).isEqualTo(1);
            assertThat(result.written()).isZero();
        }

        @Test
        @DisplayName("execute is idempotent — second run overwrites same content")
        void idempotent(@TempDir Path source, @TempDir Path output) throws IOException {
            Path src = source.resolve("skill.md");
            Files.writeString(src, "# Content");
            var plan = new CompositionPlan(List.of(included(src, "skill.md")), List.of(), List.of());
            pruner.execute(plan, output);
            pruner.execute(plan, output);
            assertThat(Files.readString(output.resolve("skill.md"))).isEqualTo("# Content");
        }
    }
}
