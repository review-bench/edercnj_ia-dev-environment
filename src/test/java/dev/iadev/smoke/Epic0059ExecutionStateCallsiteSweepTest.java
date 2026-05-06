package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * TASK-0059-0004-001: Sweep and verify call-sites of execution-state.json.
 *
 * <p>Validates the Acceptance Criterion of story-0059-0004: no SKILL.md outside {@code
 * x-internal-status-update} WRITES directly to {@code execution-state.json} via {@code Edit} or
 * {@code Write} tool calls without delegating through the canonical skill.
 *
 * <p>The sweep checks SKILL.md files under the source-of-truth directory {@code
 * src/main/resources/targets/claude/skills/} for direct write patterns that bypass {@code
 * x-internal-status-update}.
 *
 * <p>Specifically, this test detects the pattern where a skill body instructs the LLM to use {@code
 * Edit} or {@code Write} tools directly on {@code execution-state.json}, which bypasses the
 * flock-based atomic write contract and would allow commits without the required trailer.
 *
 * @see <a href="plans/epic-0059/story-0059-0004.md">story-0059-0004</a>
 */
@DisplayName("Epic0059ExecutionStateCallsiteSweepTest — TASK-0059-0004-001")
class Epic0059ExecutionStateCallsiteSweepTest {

    /**
     * Pattern that signals a direct Edit/Write mutation of execution-state.json outside the
     * canonical skill. Matches lines like: Edit(file_path: "...execution-state.json"...)
     * Write(file_path: "...execution-state.json"...)
     */
    private static final String DIRECT_WRITE_PATTERN =
            "(?i)(Edit|Write)\\s*\\(.*execution-state\\.json";

    /** The canonical skill that is allowed to mutate execution-state.json. */
    private static final String CANONICAL_SKILL = "x-internal-update-status";

    @Test
    @DisplayName(
            "sweep_noDirectWriteToExecutionState_outsideCanonicalSkill — "
                    + "no SKILL.md except x-internal-status-update "
                    + "writes execution-state.json via Edit/Write tools directly "
                    + "(story-0059-0004 TASK-001 acceptance criterion)")
    void sweep_noDirectWriteToExecutionState_outsideCanonicalSkill() throws IOException {
        Path skillsRoot = resolveSkillsRoot();
        assertThat(skillsRoot).as("skills source-of-truth root must exist").isDirectory();

        List<String> violations = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(skillsRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                    .filter(p -> !isCanonicalSkill(p))
                    .forEach(
                            skillMd -> {
                                try {
                                    checkForDirectWrite(skillMd, violations);
                                } catch (IOException e) {
                                    throw new RuntimeException("Failed to read: " + skillMd, e);
                                }
                            });
        }

        assertThat(violations)
                .as(
                        "Found SKILL.md files with direct Edit/Write calls "
                                + "to execution-state.json outside %s. "
                                + "Migrate these call-sites to use "
                                + "x-internal-update-status instead:\n%s",
                        CANONICAL_SKILL, String.join("\n", violations))
                .isEmpty();
    }

    @Test
    @DisplayName(
            "sweep_canonicalSkillExists — "
                    + "x-internal-status-update/SKILL.md must exist "
                    + "at source-of-truth path")
    void sweep_canonicalSkillExists() throws IOException {
        Path skillsRoot = resolveSkillsRoot();
        boolean found;
        try (Stream<Path> walk = Files.walk(skillsRoot)) {
            found =
                    walk.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                            .anyMatch(this::isCanonicalSkill);
        }
        assertThat(found)
                .as(
                        "x-internal-update-status/SKILL.md must exist "
                                + "under skills source-of-truth")
                .isTrue();
    }

    private boolean isCanonicalSkill(Path skillMd) {
        // Walk up parent dirs to check if CANONICAL_SKILL is in the path
        Path parent = skillMd.getParent();
        while (parent != null) {
            if (parent.getFileName() != null
                    && parent.getFileName().toString().equals(CANONICAL_SKILL)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void checkForDirectWrite(Path skillMd, List<String> violations) throws IOException {
        String content = Files.readString(skillMd, StandardCharsets.UTF_8);
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.matches(DIRECT_WRITE_PATTERN)) {
                violations.add(skillMd + ":" + (i + 1) + ": " + line.trim());
            }
        }
    }

    private Path resolveSkillsRoot() {
        Path cwd = Path.of("").toAbsolutePath();
        Path repoRoot = cwd.getFileName().toString().equals("java") ? cwd.getParent() : cwd;
        return repoRoot.resolve("src/main/resources/targets/claude/skills");
    }
}
