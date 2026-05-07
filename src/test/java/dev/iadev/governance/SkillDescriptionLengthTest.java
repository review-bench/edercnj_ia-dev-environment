package dev.iadev.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the ≤100-character ceiling on the {@code description:} field of every SKILL.md
 * frontmatter under the source-of-truth tree {@code src/main/resources/targets/claude/skills/}.
 *
 * <p>The skill description appears in the runtime catalog (system-reminder block injected on every
 * conversation turn), in {@code /help}, and in the generated README. Long descriptions inflate
 * every turn permanently. The cap keeps the catalog cheap to load.
 */
class SkillDescriptionLengthTest {

    private static final Path SKILLS_ROOT = Path.of("src/main/resources/targets/claude/skills");

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private static final Pattern DESCRIPTION_LINE = Pattern.compile("^description:\\s*(.*)$");

    @Test
    void allSkillDescriptions_areAtMost100Characters() throws IOException {
        assertThat(SKILLS_ROOT)
                .as("skills source-of-truth directory must exist")
                .exists()
                .isDirectory();

        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SKILLS_ROOT)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().equals("SKILL.md"))
                    .forEach(skillFile -> checkSkill(skillFile, violations));
        }

        assertThat(violations)
                .as(
                        "SKILL.md description field must be ≤%d characters; %d violations:%n%s",
                        MAX_DESCRIPTION_LENGTH, violations.size(), String.join("\n", violations))
                .isEmpty();
    }

    private static void checkSkill(Path skillFile, List<String> violations) {
        String description;
        try {
            description = extractDescription(skillFile);
        } catch (IOException e) {
            violations.add(skillFile + ": failed to read (" + e.getMessage() + ")");
            return;
        }
        if (description == null) {
            violations.add(skillFile + ": no `description:` field in frontmatter");
            return;
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            violations.add(
                    skillFile
                            + ": "
                            + description.length()
                            + " chars (limit "
                            + MAX_DESCRIPTION_LENGTH
                            + ")");
        }
    }

    /**
     * Returns the unquoted value of the first {@code description:} line in the YAML frontmatter, or
     * {@code null} if the field is absent.
     */
    private static String extractDescription(Path skillFile) throws IOException {
        List<String> lines = Files.readAllLines(skillFile);
        if (lines.isEmpty() || !lines.get(0).startsWith("---")) {
            return null;
        }
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("---")) {
                return null;
            }
            Matcher m = DESCRIPTION_LINE.matcher(line);
            if (m.matches()) {
                String raw = m.group(1).trim();
                if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() >= 2) {
                    raw = raw.substring(1, raw.length() - 1);
                }
                return raw;
            }
        }
        return null;
    }
}
