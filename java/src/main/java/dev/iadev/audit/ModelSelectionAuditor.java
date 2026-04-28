package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Java equivalent of {@code audit-model-selection.sh} (Rule 23).
 *
 * <p>Checks every orchestrator SKILL.md for a {@code model:} declaration in frontmatter.
 * A SKILL.md that has {@code user-invocable: true} but no {@code model:} field is a violation.
 */
public final class ModelSelectionAuditor implements Auditor {

    private static final String VIOLATION_NAME = "MODEL_SELECTION_VIOLATION";
    private static final String RULE = "MISSING_MODEL";

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
        return "model-selection";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of("java/src/main/resources/targets/claude/scripts/java-maven/audit-model-selection.sh.tpl");
    }

    private void checkSkill(Path skillPath, List<AuditViolation> violations) {
        String content = readFile(skillPath);
        boolean isUserInvocable = content.contains("user-invocable: true");
        boolean hasModel = content.contains("model:");
        if (isUserInvocable && !hasModel) {
            violations.add(new AuditViolation(skillPath, 0, RULE,
                    "SKILL.md has user-invocable: true but no model: declaration"));
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
