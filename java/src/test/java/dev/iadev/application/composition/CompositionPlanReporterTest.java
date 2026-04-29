package dev.iadev.application.composition;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iadev.domain.capability.CapabilityId;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompositionPlanReporter")
class CompositionPlanReporterTest {

    private final CompositionPlanReporter reporter = new CompositionPlanReporter();

    private static CompositionPlan.ArtifactEntry included(String rel) {
        return new CompositionPlan.ArtifactEntry(Path.of("/fake/" + rel), rel);
    }

    private static CompositionPlan.ArtifactEntry excluded(String rel, String reason) {
        return new CompositionPlan.ArtifactEntry(Path.of("/fake/" + rel), rel, reason);
    }

    @Nested
    @DisplayName("text format")
    class TextFormat {

        @Test
        @DisplayName("plan listed in text with included/excluded counts (happy)")
        void textFormat() {
            CompositionPlan plan = new CompositionPlan(
                    List.of(included("skills/x-test/SKILL.md"), included("rules/01-project.md")),
                    List.of(excluded("skills/x-db/SKILL.md", "no matching capability")),
                    List.of());
            String report = reporter.report(plan, CompositionPlanReporter.Format.TEXT);
            assertThat(report).contains("Included artifacts: 2");
            assertThat(report).contains("Excluded artifacts: 1");
            assertThat(report).contains("+ skills/x-test/SKILL.md");
            assertThat(report).contains("- skills/x-db/SKILL.md");
        }

        @Test
        @DisplayName("empty plan produces zero counts (degenerate)")
        void emptyPlan() {
            String report = reporter.report(CompositionPlan.empty(), CompositionPlanReporter.Format.TEXT);
            assertThat(report).contains("Included artifacts: 0");
            assertThat(report).contains("Excluded artifacts: 0");
        }
    }

    @Nested
    @DisplayName("text with warnings")
    class TextWithWarnings {

        @Test
        @DisplayName("text report includes warning count")
        void textReportWithWarnings() {
            CompositionPlan plan = new CompositionPlan(
                    List.of(),
                    List.of(),
                    List.of("warning 1"));
            String report = reporter.report(plan, CompositionPlanReporter.Format.TEXT);
            assertThat(report).contains("Warnings: 1");
        }
    }

    @Nested
    @DisplayName("JSON format")
    class JsonFormat {

        @Test
        @DisplayName("JSON output is parseable and contains counts")
        void jsonFormat() {
            CompositionPlan plan = new CompositionPlan(
                    List.of(included("skills/x-test/SKILL.md")),
                    List.of(),
                    List.of());
            String json = reporter.report(plan, CompositionPlanReporter.Format.JSON);
            assertThat(json).contains("\"included\": 1");
            assertThat(json).contains("\"excluded\": 0");
            assertThat(json).startsWith("{");
            assertThat(json).endsWith("}");
        }
    }
}
