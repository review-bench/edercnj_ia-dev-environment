package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Regression test for the hotfix that taught {@code telemetry-lib.sh::resolve_context} to recognise
 * the {@code epic/NNNN} branch convention introduced by Rule 21 (EPIC-0049).
 *
 * <p>Before the hotfix, only {@code feature/epic-NNNN} / {@code feat/epic-NNNN} matched, so any
 * tool call from an {@code epic/NNNN} branch fell through to the {@code "unknown"} fallback and
 * polluted {@code plans/unknown/telemetry/events.ndjson}.
 */
@DisplayName("TelemetryLibResolveContextTest — epic/NNNN branch resolution (Rule 21)")
@DisabledOnOs(
        value = OS.WINDOWS,
        disabledReason = "POSIX bash + git CLI; mirrors the rest of the smoke suite.")
class TelemetryLibResolveContextTest {

    @Test
    @DisplayName("resolves EPIC-0063 from a bare epic/0063 branch")
    void resolveContext_epicSlashBranch_returnsEpicId() throws Exception {
        assertThat(resolveOnBranch("epic/0063")).isEqualTo("EPIC-0063");
    }

    @Test
    @DisplayName("still resolves EPIC-0042 from legacy feat/epic-0042-foo branch")
    void resolveContext_legacyFeatEpicBranch_returnsEpicId() throws Exception {
        assertThat(resolveOnBranch("feat/epic-0042-foo")).isEqualTo("EPIC-0042");
    }

    @Test
    @DisplayName("falls back to 'unknown' on develop (no match)")
    void resolveContext_nonMatchingBranch_returnsUnknown() throws Exception {
        assertThat(resolveOnBranch("develop")).isEqualTo("unknown");
    }

    private String resolveOnBranch(String branch) throws Exception {
        Path fakeRepo = Files.createTempDirectory("telemetry-resolve-");
        try {
            runCommand(fakeRepo, "git", "init", "-q", "-b", "main");
            runCommand(
                    fakeRepo,
                    "git",
                    "-c",
                    "user.email=t@t",
                    "-c",
                    "user.name=t",
                    "commit",
                    "--allow-empty",
                    "-q",
                    "-m",
                    "init");
            runCommand(fakeRepo, "git", "checkout", "-q", "-b", branch);

            Path libSrc =
                    repoRoot()
                            .resolve(
                                    "src/main/resources/targets/claude/hooks/"
                                            + "telemetry-lib.sh");
            Path libDest = fakeRepo.resolve(".claude/hooks/telemetry-lib.sh");
            Files.createDirectories(libDest.getParent());
            Files.copy(libSrc, libDest);

            ProcessBuilder pb =
                    new ProcessBuilder(
                            "bash",
                            "-c",
                            "source .claude/hooks/telemetry-lib.sh"
                                    + " && resolve_context"
                                    + " && printf '%s' \"$TELEMETRY_EPIC_ID\"");
            pb.directory(fakeRepo.toFile());
            pb.redirectErrorStream(true);
            pb.environment().put("CLAUDE_PROJECT_DIR", fakeRepo.toString());
            pb.environment().remove("CLAUDE_TELEMETRY_CONTEXT");
            Process p = pb.start();
            String stdout = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                throw new RuntimeException("resolve_context timeout");
            }
            assertThat(p.exitValue()).as("resolve_context must exit 0").isEqualTo(0);
            return stdout.trim();
        } finally {
            deleteRecursive(fakeRepo);
        }
    }

    private void runCommand(Path workdir, String... cmd) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workdir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        p.getInputStream().readAllBytes();
        if (!p.waitFor(15, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new RuntimeException("Timeout: " + String.join(" ", cmd));
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException(
                    "Command failed (exit " + p.exitValue() + "): " + String.join(" ", cmd));
        }
    }

    private Path repoRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        return cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
    }

    private void deleteRecursive(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.compareTo(a))
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (Exception ignored) {
                                    // best-effort cleanup
                                }
                            });
        }
    }
}
