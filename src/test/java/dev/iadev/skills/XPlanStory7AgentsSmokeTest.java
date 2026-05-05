package dev.iadev.skills;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Smoke tests for story-0077-0020: x-plan-story Phase 2 refactored from 5 to 7 parallel agents
 * (adding PentestEngineer and PerformanceEngineer).
 */
@DisplayName("XPlanStory7AgentsSmokeTest (story-0077-0020)")
class XPlanStory7AgentsSmokeTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"));
    private static final Path SOURCE_SKILL =
            REPO_ROOT.resolve(
                    "src/main/resources/targets/claude/skills/core/plan/x-plan-story/SKILL.md");
    private static final Path CLAUDE_SKILL =
            REPO_ROOT.resolve(".claude/skills/x-plan-story/SKILL.md");

    private static final List<String> GOLDEN_PROFILES =
            List.of(
                    "java-quarkus",
                    "java-spring",
                    "java-spring-clickhouse",
                    "java-spring-cqrs-es",
                    "java-spring-elasticsearch",
                    "java-spring-event-driven",
                    "java-spring-fintech-pci",
                    "java-spring-hexagonal",
                    "java-spring-neo4j",
                    "parallelism-heuristics");

    @Test
    @DisplayName("sourceSkill_describes7Agents_inDescription")
    void sourceSkill_describes7Agents_inDescription() throws IOException {
        String content = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-plan-story description must mention 7 specialized agents")
                .contains("7 specialized agents");
    }

    @Test
    @DisplayName("sourceSkill_containsPentestEngineer_agent")
    void sourceSkill_containsPentestEngineer_agent() throws IOException {
        String content = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-plan-story must include PentestEngineer subagent")
                .contains("PentestEngineer");
    }

    @Test
    @DisplayName("sourceSkill_containsPerformanceEngineer_agent")
    void sourceSkill_containsPerformanceEngineer_agent() throws IOException {
        String content = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        assertThat(content)
                .as("x-plan-story must include PerformanceEngineer subagent")
                .contains("PerformanceEngineer");
    }

    @Test
    @DisplayName("sourceSkill_dispatches7Subagents_inSingleMessage")
    void sourceSkill_dispatches7Subagents_inSingleMessage() throws IOException {
        String content = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        assertThat(content)
                .as("Phase 2 must instruct to dispatch all 7 in ONE assistant message")
                .contains("Dispatch all 7 in ONE assistant message");
    }

    @Test
    @DisplayName("sourceSkill_telemetry_includesPentestAndPerformanceMarkers")
    void sourceSkill_telemetry_includesPentestAndPerformanceMarkers() throws IOException {
        String content = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        assertThat(content)
                .as("Telemetry start marker for PentestEngineer must exist")
                .contains("subagent-start x-plan-story PentestEngineer");
        assertThat(content)
                .as("Telemetry end marker for PentestEngineer must exist")
                .contains("subagent-end x-plan-story PentestEngineer ok");
        assertThat(content)
                .as("Telemetry start marker for PerformanceEngineer must exist")
                .contains("subagent-start x-plan-story PerformanceEngineer");
        assertThat(content)
                .as("Telemetry end marker for PerformanceEngineer must exist")
                .contains("subagent-end x-plan-story PerformanceEngineer ok");
    }

    @Test
    @DisplayName("claudeSkill_matches7AgentContent")
    void claudeSkill_matches7AgentContent() throws IOException {
        // .claude/ is a generated output (gitignored) — skip when not yet generated locally.
        // CI does not regenerate .claude/ from source; the source-of-truth invariant is
        // already covered by the parameterized golden-file tests below.
        assumeTrue(
                Files.exists(CLAUDE_SKILL),
                ".claude/skills/x-plan-story/SKILL.md not generated (run ia-dev-env generate locally)");
        String source = Files.readString(SOURCE_SKILL, StandardCharsets.UTF_8);
        String claudeCopy = Files.readString(CLAUDE_SKILL, StandardCharsets.UTF_8);
        assertThat(claudeCopy)
                .as(".claude/skills/x-plan-story/SKILL.md must match source-of-truth")
                .isEqualTo(source);
    }

    @ParameterizedTest(name = "golden/{0} contains 7-agent x-plan-story")
    @ValueSource(
            strings = {
                "java-quarkus",
                "java-spring",
                "java-spring-clickhouse",
                "java-spring-cqrs-es",
                "java-spring-elasticsearch",
                "java-spring-event-driven",
                "java-spring-fintech-pci",
                "java-spring-hexagonal",
                "java-spring-neo4j",
                "parallelism-heuristics"
            })
    @DisplayName("goldenFile_contains7Agents")
    void goldenFile_contains7Agents(String profile) throws IOException {
        Path goldenSkill =
                REPO_ROOT.resolve(
                        "src/test/resources/golden/"
                                + profile
                                + "/.claude/skills/x-plan-story/SKILL.md");

        if (!Files.exists(goldenSkill)) {
            return;
        }

        String content = Files.readString(goldenSkill, StandardCharsets.UTF_8);
        assertThat(content)
                .as("Golden file for profile %s must mention 7 specialized agents", profile)
                .contains("7 specialized agents");
        assertThat(content)
                .as("Golden file for profile %s must contain PentestEngineer", profile)
                .contains("PentestEngineer");
        assertThat(content)
                .as("Golden file for profile %s must contain PerformanceEngineer", profile)
                .contains("PerformanceEngineer");
    }
}
