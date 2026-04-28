package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Java equivalent of {@code audit-skill-visibility.sh} (Rule 22).
 *
 * <p>Checks that every internal skill ({@code x-internal-*}) has {@code visibility: internal} in
 * frontmatter. Also checks that public skills do NOT set {@code visibility: internal}.
 */
public final class SkillVisibilityAuditor implements Auditor {

    private static final String VIOLATION_NAME = "SKILL_VISIBILITY_VIOLATION";

    @Override
    public AuditResult audit(AuditCorpus corpus) {
        List<AuditViolation> violations = new ArrayList<>();
        corpus.walkSkills().forEach(skill -> checkSkill(skill, violations));
        return violations.isEmpty()
                ? AuditResult.ok()
                : AuditResult.violation(VIOLATION_NAME, violations);
    }

    @Override
    public String name() {
        return "skill-visibility";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "java/src/main/resources/targets/claude/scripts/java-maven/audit-skill-visibility.sh.tpl");
    }

    private void checkSkill(Path skillPath, List<AuditViolation> violations) {
        boolean isInternal = skillPath.toString().contains("x-internal-");
        String content = readFile(skillPath);
        boolean hasInternalVisibility = content.contains("visibility: internal");

        if (isInternal && !hasInternalVisibility) {
            violations.add(
                    new AuditViolation(
                            skillPath,
                            0,
                            "INTERNAL_MISSING_FRONTMATTER",
                            "Internal skill missing 'visibility: internal' in frontmatter"));
        }
        if (!isInternal && hasInternalVisibility) {
            violations.add(
                    new AuditViolation(
                            skillPath,
                            0,
                            "PUBLIC_HAS_INTERNAL_VISIBILITY",
                            "Public skill incorrectly has 'visibility: internal'"));
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read: " + path, e);
        }
    }
}
