package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Smoke tests for EPIC-0065 (Feature Creation Chain Refactor).
 *
 * <p>Validates end-to-end structural invariants introduced by stories 0001–0010:
 *
 * <ul>
 *   <li>{@code x-feature-ideate} is present as a public skill (model: opus)
 *   <li>{@code x-feature-create} is present as a public skill (replaces x-epic-decompose)
 *   <li>{@code x-internal-epic-create}, {@code x-internal-epic-map}, {@code
 *       x-internal-story-create} are present as internal skills
 *   <li>{@code x-epic-decompose}, {@code x-epic-create}, {@code x-epic-map}, {@code x-story-create}
 *       are NOT present as public skills (hard-cut per Rule 19 §Hard-cut autorizado)
 *   <li>Rule 09 contains the {@code docs/} branch type addition
 *   <li>Rule 19 contains the "Hard-cut autorizado" clause
 *   <li>Rule 22 contains the 3 new internal skills table
 *   <li>{@code audit-epic-branches.sh} contains the new Check D for docs/ branches
 * </ul>
 */
@DisplayName("Epic0065SmokeIT — Feature Creation Chain Refactor structural invariants")
class Epic0065SmokeIT extends SmokeTestBase {

    // Public skills that must exist after EPIC-0065
    private static final List<String> EXPECTED_PUBLIC_SKILLS =
            List.of("x-ideate-feature", "x-create-feature");

    // Internal skills that must exist (under x-internal-* naming)
    private static final List<String> EXPECTED_INTERNAL_SKILLS =
            List.of("x-internal-create-epic", "x-internal-map-epic", "x-internal-create-story");

    // Skills that must NOT exist as public skills after EPIC-0065 hard-cut.
    // NOTE: x-epic-create and x-story-create were removed here (EPIC-0077) — both were
    // re-introduced by EPIC-0077 as Product-First lifecycle skills with narrower responsibilities
    // (Feature-derived creation only), completely different from the old orchestrators hard-cut
    // by EPIC-0065. x-epic-map and x-epic-decompose remain hard-cut.
    private static final List<String> HARD_CUT_SKILLS =
            List.of("x-epic-decompose", "x-epic-map");

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("smoke_featureIdeateExists — x-feature-ideate ships with model: opus")
    void smoke_featureIdeateExists(String profile) throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");
        Path ideateSkill = skillsDir.resolve("x-ideate-feature/SKILL.md");

        assertThat(Files.isRegularFile(ideateSkill))
                .as("profile %s: x-feature-ideate/SKILL.md must exist (story-0065-0002)", profile)
                .isTrue();

        String content = Files.readString(ideateSkill, StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "profile %s: x-feature-ideate must declare model: opus (Deep Planner tier, Rule 23)",
                        profile)
                .contains("model: opus");
        assertThat(content)
                .as("profile %s: x-feature-ideate must be user-invocable", profile)
                .contains("user-invocable: true");
        assertThat(content)
                .as("profile %s: x-feature-ideate must reference RULE-005 (no auto-chain)", profile)
                .contains("RULE-005");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("smoke_featureCreateExists — x-feature-create ships with model: sonnet")
    void smoke_featureCreateExists(String profile) throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");
        Path createSkill = skillsDir.resolve("x-create-feature/SKILL.md");

        assertThat(Files.isRegularFile(createSkill))
                .as("profile %s: x-feature-create/SKILL.md must exist (story-0065-0003)", profile)
                .isTrue();

        String content = Files.readString(createSkill, StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "profile %s: x-feature-create must declare model: sonnet (Orchestrator tier, Rule 23)",
                        profile)
                .contains("model: sonnet");
        assertThat(content)
                .as(
                        "profile %s: x-feature-create must declare Phase P7 (CI-watch, Rule 45)",
                        profile)
                .contains("x-watch-pr-ci");
        assertThat(content)
                .as("profile %s: x-feature-create must reference docs/ branch", profile)
                .contains("docs/");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_internalSkillsExist — 3 internalized skills present with visibility: internal")
    void smoke_internalSkillsExist(String profile) throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");

        for (String internal : EXPECTED_INTERNAL_SKILLS) {
            Path skillMd = skillsDir.resolve(internal + "/SKILL.md");
            assertThat(Files.isRegularFile(skillMd))
                    .as(
                            "profile %s: %s/SKILL.md must exist (story-0065-0004/0005/0006)",
                            profile, internal)
                    .isTrue();

            String content = Files.readString(skillMd, StandardCharsets.UTF_8);
            assertThat(content)
                    .as(
                            "profile %s: %s must have visibility: internal (Rule 22)",
                            profile, internal)
                    .contains("visibility: internal");
            assertThat(content)
                    .as(
                            "profile %s: %s must have user-invocable: false (Rule 22)",
                            profile, internal)
                    .contains("user-invocable: false");
            assertThat(content)
                    .as(
                            "profile %s: %s must have INTERNAL SKILL body marker (Rule 22)",
                            profile, internal)
                    .contains("INTERNAL SKILL");
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_hardCutSkillsAbsent — hard-cut public skills NOT present as public (Rule 19 EPIC-0065)")
    void smoke_hardCutSkillsAbsent(String profile) throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");

        for (String removed : HARD_CUT_SKILLS) {
            Path skillDir = skillsDir.resolve(removed);
            // The directory should not exist at all (complete removal)
            if (Files.isDirectory(skillDir)) {
                // If somehow present, check it doesn't have user-invocable: true
                Path skillMd = skillDir.resolve("SKILL.md");
                if (Files.isRegularFile(skillMd)) {
                    String content = Files.readString(skillMd, StandardCharsets.UTF_8);
                    assertThat(content)
                            .as(
                                    "profile %s: %s was hard-cut (Rule 19 EPIC-0065) — if present, must NOT be user-invocable: true",
                                    profile, removed)
                            .doesNotContain("user-invocable: true");
                }
            }
            // Primary assertion: x-epic-decompose must not exist at all
            if ("x-epic-decompose".equals(removed)) {
                assertThat(Files.isDirectory(skillDir))
                        .as(
                                "profile %s: x-epic-decompose must be completely removed (story-0065-0003 hard-cut)",
                                profile)
                        .isFalse();
            }
        }
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("smoke_rule09DocsType — Rule 09 contains docs/ branch type (story-0065-0001)")
    void smoke_rule09DocsType(String profile) throws IOException {
        runPipeline(profile);
        Path rulesDir = getOutputDir(profile).resolve(".claude/rules");
        Path rule09 = rulesDir.resolve("09-branching-model.md");

        assertThat(Files.isRegularFile(rule09))
                .as("profile %s: 09-branching-model.md must exist", profile)
                .isTrue();

        String content = Files.readString(rule09, StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "profile %s: Rule 09 must document docs/ branch type (story-0065-0001)",
                        profile)
                .contains("`docs/*`");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_rule19HardCut — Rule 19 contains Hard-cut autorizado clause (story-0065-0001)")
    void smoke_rule19HardCut(String profile) throws IOException {
        runPipeline(profile);
        Path rulesDir = getOutputDir(profile).resolve(".claude/rules");
        Path rule19 = rulesDir.resolve("19-backward-compatibility.md");

        assertThat(Files.isRegularFile(rule19))
                .as("profile %s: 19-backward-compatibility.md must exist", profile)
                .isTrue();

        String content = Files.readString(rule19, StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "profile %s: Rule 19 must contain Hard-cut autorizado clause (story-0065-0001)",
                        profile)
                .contains("Hard-cut autorizado");
        assertThat(content)
                .as("profile %s: Rule 19 Hard-cut must document x-epic-decompose case", profile)
                .contains("x-epic-decompose");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_rule22InternalSkills — Rule 22 documents 3 EPIC-0065 internals (story-0065-0001)")
    void smoke_rule22InternalSkills(String profile) throws IOException {
        runPipeline(profile);
        Path rulesDir = getOutputDir(profile).resolve(".claude/rules");
        Path rule22 = rulesDir.resolve("22-skill-visibility.md");

        assertThat(Files.isRegularFile(rule22))
                .as("profile %s: 22-skill-visibility.md must exist", profile)
                .isTrue();

        String content = Files.readString(rule22, StandardCharsets.UTF_8);
        assertThat(content)
                .as("profile %s: Rule 22 must document x-internal-create-epic", profile)
                .contains("x-internal-create-epic");
        assertThat(content)
                .as("profile %s: Rule 22 must document x-internal-map-epic", profile)
                .contains("x-internal-map-epic");
        assertThat(content)
                .as("profile %s: Rule 22 must document x-internal-create-story", profile)
                .contains("x-internal-create-story");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName(
            "smoke_auditEpicBranchesCheckD — audit-epic-branches.sh has Check D for docs/ (story-0065-0001)")
    void smoke_auditEpicBranchesCheckD(String profile) throws IOException {
        runPipeline(profile);
        Path scriptsDir = getOutputDir(profile).resolve(".claude/scripts");
        Path auditScript = scriptsDir.resolve("audit-epic-branches.sh");

        assertThat(Files.isRegularFile(auditScript))
                .as("profile %s: audit-epic-branches.sh must exist", profile)
                .isTrue();

        String content = Files.readString(auditScript, StandardCharsets.UTF_8);
        assertThat(content)
                .as(
                        "profile %s: audit-epic-branches.sh must have Check D for docs/ branches",
                        profile)
                .contains("Check D");
        assertThat(content)
                .as(
                        "profile %s: audit-epic-branches.sh must have v4 layout PathResolver (EPICS_DIR)",
                        profile)
                .contains("EPICS_DIR");
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("dev.iadev.smoke.SmokeProfiles#profiles")
    @DisplayName("smoke_featureChainComplete — all 5 public skills of the chain ship together")
    void smoke_featureChainComplete(String profile) throws IOException {
        runPipeline(profile);
        Path skillsDir = getOutputDir(profile).resolve(".claude/skills");

        // x-feature-ideate + x-feature-create (new public entry points)
        for (String pub : EXPECTED_PUBLIC_SKILLS) {
            assertThat(Files.isRegularFile(skillsDir.resolve(pub + "/SKILL.md")))
                    .as(
                            "profile %s: %s/SKILL.md must exist (Feature Creation Chain, EPIC-0065)",
                            profile, pub)
                    .isTrue();
        }

        // x-orchestrate-epic still exists (planning-only, not hard-cut)
        assertThat(Files.isRegularFile(skillsDir.resolve("x-orchestrate-epic/SKILL.md")))
                .as(
                        "profile %s: x-orchestrate-epic/SKILL.md must still exist (story-0065-0007, RULE-006)",
                        profile)
                .isTrue();

        // x-orchestrate-epic SKILL.md must warn about not creating epic/stories
        String orchContent =
                Files.readString(
                        skillsDir.resolve("x-orchestrate-epic/SKILL.md"), StandardCharsets.UTF_8);
        assertThat(orchContent)
                .as(
                        "profile %s: x-orchestrate-epic must warn it does NOT create epic/stories (story-0065-0007)",
                        profile)
                .contains("NÃO cria épico nem stories");
    }
}
