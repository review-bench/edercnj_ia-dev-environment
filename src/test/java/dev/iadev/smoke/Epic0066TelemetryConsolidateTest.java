package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Validates telemetry-consolidate.sh: 5 scenarios (degenerate, happy, malformed, error, self-check).
 *
 * <p>TPP order: degenerate (empty) → constant (self-check) → happy (100 events) →
 * boundary (malformed lines) → error (missing jq).
 */
@DisplayName("Epic0066 — telemetry-consolidate.sh")
class Epic0066TelemetryConsolidateTest {

    private static final String SCRIPT_PATH =
            System.getProperty("user.dir") + "/scripts/telemetry-consolidate.sh";

    @TempDir Path tempDir;

    private Path ndjsonPath;

    @BeforeEach
    void setUp() throws IOException {
        ndjsonPath = tempDir.resolve("events.ndjson");
    }

    private ProcessResult run(List<String> args, Path workDir, String customPath)
            throws IOException, InterruptedException {
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("/bin/bash");
        cmd.add(SCRIPT_PATH);
        cmd.addAll(args);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.environment().put("CLAUDE_PROJECT_DIR", tempDir.toString());
        if (customPath != null) {
            pb.environment().put("PATH", customPath);
        }

        Process proc = pb.start();
        boolean finished = proc.waitFor(15, TimeUnit.SECONDS);
        assertThat(finished).as("Process should complete within 15s").isTrue();

        String stdout = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(proc.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        return new ProcessResult(proc.exitValue(), stdout, stderr);
    }

    private ProcessResult run(List<String> args) throws IOException, InterruptedException {
        Path plansDir = tempDir.resolve("ai/epics/epic-0066-test/telemetry");
        Files.createDirectories(plansDir);
        Files.copy(ndjsonPath, plansDir.resolve("events.ndjson"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return run(args, tempDir, null);
    }

    private String buildEvent(String storyId, String tool, int durationMs, long timestamp) {
        return String.format(
                "{\"type\":\"tool.call\",\"tool\":\"%s\",\"storyId\":\"%s\","
                        + "\"epicId\":\"epic-0066\",\"durationMs\":%d,\"timestamp\":%d,"
                        + "\"metadata\":{\"storyId\":\"%s\",\"taskId\":\"TASK-0066-0002-001\"}}",
                tool, storyId, durationMs, timestamp, storyId);
    }

    @Nested
    @DisplayName("degenerate — empty NDJSON")
    class Degenerate {

        @Test
        @DisplayName("empty events.ndjson returns exit 1 and NO_TELEMETRY")
        void emptyNdjson_returnsNoTelemetry() throws IOException, InterruptedException {
            Files.writeString(ndjsonPath, "", StandardCharsets.UTF_8);

            ProcessResult result = run(List.of("--story=story-0066-0002"));

            assertThat(result.exitCode()).isEqualTo(1);
            assertThat(result.stderr()).contains("NO_TELEMETRY");
            assertThat(result.stdout()).isEmpty();
        }
    }

    @Nested
    @DisplayName("self-check")
    class SelfCheck {

        @Test
        @DisplayName("--self-check exits 0 and prints OK when jq present")
        void selfCheck_jqPresent_exits0() throws IOException, InterruptedException {
            Path plansDir = tempDir.resolve("ai/epics");
            Files.createDirectories(plansDir);

            ProcessResult result = run(List.of("--self-check"), tempDir, null);

            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.stdout()).contains("OK");
        }
    }

    @Nested
    @DisplayName("happy path — 100 events")
    class HappyPath {

        @Test
        @DisplayName("100 valid events produce complete JSON with tasks and topTools")
        void hundredEvents_producesValidJson() throws IOException, InterruptedException {
            StringBuilder sb = new StringBuilder();
            long base = 1_700_000_000_000L;
            for (int i = 0; i < 100; i++) {
                String tool = i % 3 == 0 ? "Bash" : (i % 3 == 1 ? "Read" : "Write");
                sb.append(buildEvent("story-0066-0002", tool, 100 + i * 10, base + i * 1000L))
                  .append('\n');
            }
            Files.writeString(ndjsonPath, sb.toString(), StandardCharsets.UTF_8);

            ProcessResult result = run(List.of("--story=story-0066-0002"));

            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.stdout()).isNotEmpty();

            String json = result.stdout();
            assertThat(json).contains("\"scope\"");
            assertThat(json).contains("\"story\"");
            assertThat(json).contains("story-0066-0002");
            assertThat(json).contains("\"tasks\"");
            assertThat(json).contains("\"topTools\"");
            assertThat(json).contains("\"activeMs\"");
            assertThat(json).contains("\"elapsedMs\"");
            assertThat(json).contains("\"events\"");
        }
    }

    @Nested
    @DisplayName("boundary — malformed lines")
    class MalformedLines {

        @Test
        @DisplayName("50 valid + 5 malformed lines: exit 0, events==50, WARN in stderr")
        void malformedLines_skippedWithWarning() throws IOException, InterruptedException {
            StringBuilder sb = new StringBuilder();
            long base = 1_700_000_000_000L;
            for (int i = 0; i < 50; i++) {
                sb.append(buildEvent("story-0066-0002", "Bash", 200, base + i * 1000L))
                  .append('\n');
                if (i % 10 == 9) {
                    sb.append("{not valid json at all {{{\n");
                }
            }
            Files.writeString(ndjsonPath, sb.toString(), StandardCharsets.UTF_8);

            ProcessResult result = run(List.of("--story=story-0066-0002"));

            assertThat(result.exitCode()).isEqualTo(0);
            assertThat(result.stderr()).contains("WARN");
            String json = result.stdout();
            assertThat(json).contains("\"events\"");
            // events count should reflect only valid lines (50)
            assertThat(json).satisfiesAnyOf(
                j -> assertThat(j).contains("\"events\":50"),
                j -> assertThat(j).contains("\"events\": 50"));
        }
    }

    @Nested
    @DisplayName("error — missing dependency")
    class MissingDependency {

        @Test
        @DisplayName("--self-check exits 2 and mentions jq when PATH is empty")
        void jqMissing_selfCheckExits2() throws IOException, InterruptedException {
            Path plansDir = tempDir.resolve("ai/epics");
            Files.createDirectories(plansDir);

            // Run self-check with a PATH that has no jq
            ProcessResult result = run(List.of("--self-check"), tempDir, "/nonexistent/bin");

            assertThat(result.exitCode()).isEqualTo(2);
            assertThat(result.stderr()).contains("OPERATIONAL_ERROR");
            assertThat(result.stderr().toLowerCase()).contains("jq");
        }
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
