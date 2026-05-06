package dev.iadev.smoke;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Regression test — verifies that skills modified by story-0078-0010 (Read KP directives)
 * still have valid frontmatter and their existing required sections intact.
 *
 * <p>Story: story-0078-0010 (Consumer Skills — Read KP Directives)
 */
@DisplayName("Epic0078SkillsRegressionIT — Read directive insertion regression")
class Epic0078SkillsRegressionIT {

    private static final Path SKILLS_ROOT =
            Path.of("src", "main", "resources", "targets", "claude", "skills");

    /** Skills modified by story-0078-0010. Map: skill-name → required section header. */
    private static final List<SkillCheck> MODIFIED_SKILLS = List.of(
            new SkillCheck("x-plan-architecture", "## Workflow"),
            new SkillCheck("x-plan-task", "## Planning Status Propagation"),
            new SkillCheck("x-plan-tests", "## Integration Notes"),
            new SkillCheck("x-review-security", "## Workflow"),
            new SkillCheck("x-scan-owasp", "## Integration Notes"),
            new SkillCheck("x-model-threats", "## Integration Notes"),
            new SkillCheck("x-implement-epic", "## Full Protocol"),
            new SkillCheck("x-implement-story", "## Integration Notes"),
            new SkillCheck("x-implement-task", "## Full Protocol"),
            new SkillCheck("x-internal-verify-phase-gates", "## Rule References"),
            new SkillCheck("x-internal-update-status", "## Integration Notes"),
            new SkillCheck("x-internal-verify-story", "## Integration Notes"),
            new SkillCheck("x-watch-pr-ci", "## Rule Compliance"),
            new SkillCheck("x-internal-write-story-report", "## Integration Notes"),
            new SkillCheck("x-create-pr", "## Integration Notes"),
            new SkillCheck("x-manage-pr-merge-train", "## Full Protocol"),
            new SkillCheck("x-refine-story", "## Integration Notes"),
            new SkillCheck("x-refine-epic", "## Integration Notes"),
            new SkillCheck("x-migrate-frontmatter", "## Integration Notes")
    );

    @TestFactory
    @DisplayName("modifiedSkillHasValidFrontmatterAndRequiredSection")
    Stream<DynamicTest> modifiedSkillHasValidFrontmatterAndRequiredSection() {
        return MODIFIED_SKILLS.stream().map(check ->
                DynamicTest.dynamicTest(
                        check.skillName + " — frontmatter + " + check.requiredSection,
                        () -> {
                            Path skillFile = findSkillFile(check.skillName);
                            assertThat(skillFile)
                                    .as("SKILL.md for %s must exist", check.skillName)
                                    .exists();
                            String content = Files.readString(skillFile, StandardCharsets.UTF_8);

                            assertThat(content)
                                    .as("%s SKILL.md must start with frontmatter delimiter",
                                            check.skillName)
                                    .startsWith("---");
                            assertThat(content)
                                    .as("%s SKILL.md must contain 'name:' in frontmatter",
                                            check.skillName)
                                    .contains("name:");
                            assertThat(content)
                                    .as("%s SKILL.md must retain section %s after Read directive insertion",
                                            check.skillName, check.requiredSection)
                                    .contains(check.requiredSection);
                            assertThat(content)
                                    .as("%s SKILL.md must contain Knowledge Pack References section",
                                            check.skillName)
                                    .contains("## Knowledge Pack References");
                        }));
    }

    private Path findSkillFile(String skillName) throws IOException {
        try (Stream<Path> stream = Files.walk(SKILLS_ROOT)) {
            return stream
                    .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                    .filter(p -> p.getParent().getFileName().toString().equals(skillName))
                    .findFirst()
                    .orElseThrow(() ->
                            new AssertionError("SKILL.md not found for skill: " + skillName));
        }
    }

    private record SkillCheck(String skillName, String requiredSection) {}
}
