package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java equivalent of {@code audit-epic-branches.sh} (Rule 21).
 *
 * <p>Checks every {@code execution-state.json} that declares {@code flowVersion="2"} for the
 * presence of a non-null {@code epicBranch} field. Missing or empty epicBranch on a v2 state file
 * is a violation.
 *
 * <p>Note: Branch existence on the remote (git/gh checks) is skipped in this Java implementation —
 * that check requires network access and is the responsibility of the bash CI script.
 */
public final class EpicBranchesAuditor implements Auditor {

    private static final String VIOLATION_NAME = "EPIC_BRANCH_VIOLATION";
    private static final Pattern FLOW_VERSION_PATTERN =
            Pattern.compile("\"flowVersion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EPIC_BRANCH_PATTERN =
            Pattern.compile("\"epicBranch\"\\s*:\\s*\"([^\"]+)\"");

    @Override
    public AuditResult audit(AuditCorpus corpus) {
        List<AuditViolation> violations = new ArrayList<>();
        corpus.walkExecutionStates().forEach(stateFile -> checkStateFile(stateFile, violations));
        return violations.isEmpty()
                ? AuditResult.ok()
                : AuditResult.violation(VIOLATION_NAME, violations);
    }

    @Override
    public String name() {
        return "epic-branches";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "java/src/main/resources/targets/claude/scripts/java-maven/audit-epic-branches.sh.tpl");
    }

    private void checkStateFile(Path stateFile, List<AuditViolation> violations) {
        String content = readFile(stateFile);
        Matcher flowMatcher = FLOW_VERSION_PATTERN.matcher(content);
        if (!flowMatcher.find() || !flowMatcher.group(1).equals("2")) {
            return;
        }
        Matcher branchMatcher = EPIC_BRANCH_PATTERN.matcher(content);
        if (!branchMatcher.find() || branchMatcher.group(1).isBlank()) {
            violations.add(
                    new AuditViolation(
                            stateFile,
                            0,
                            "MISSING_EPIC_BRANCH",
                            "flowVersion=2 state file has no epicBranch declared"));
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
