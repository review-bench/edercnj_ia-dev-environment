package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Java equivalent of {@code audit-bypass-flags.sh} (Rule 45).
 *
 * <p>Checks that {@code --no-ci-watch} and {@code --skip-verification} flags appear only inside
 * {@code ## Recovery} blocks in SKILL.md files. Occurrences outside Recovery blocks are violations.
 */
public final class BypassFlagsAuditor implements Auditor {

    private static final String VIOLATION_NAME = "BYPASS_FLAG_VIOLATION";
    private static final Pattern BYPASS_PATTERN =
            Pattern.compile("--no-ci-watch|--skip-verification");
    private static final String RECOVERY_MARKER = "## Recovery";
    private static final String AUDIT_EXEMPT = "audit-exempt";

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
        return "bypass-flags";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "java/src/main/resources/targets/claude/scripts/java-maven/audit-bypass-flags.sh.tpl");
    }

    private void checkSkill(Path skillPath, List<AuditViolation> violations) {
        String content = readFile(skillPath);
        if (!BYPASS_PATTERN.matcher(content).find()) {
            return;
        }

        String[] lines = content.split("\n");
        boolean inRecovery = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("## ")) {
                inRecovery = line.contains("Recovery");
            }
            if (BYPASS_PATTERN.matcher(line).find() && !inRecovery) {
                boolean exempt = i > 0 && lines[i - 1].contains(AUDIT_EXEMPT);
                if (!exempt) {
                    violations.add(
                            new AuditViolation(
                                    skillPath,
                                    i + 1,
                                    "BYPASS_FLAG_OUTSIDE_RECOVERY",
                                    "Bypass flag used outside ## Recovery block: " + line.strip()));
                }
            }
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
