package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java equivalent of {@code audit-flow-version.sh} (Rule 19).
 *
 * <p>Checks every {@code execution-state.json} in the corpus for a valid {@code flowVersion} field.
 * Valid values are {@code "1"}, {@code "2"}, and {@code "4"}. Any other value — or absent field —
 * is a violation.
 */
public final class FlowVersionAuditor implements Auditor {

    private static final String VIOLATION_NAME = "FLOW_VERSION_VIOLATION";
    private static final Set<String> VALID_VERSIONS = Set.of("1", "2", "4");
    private static final Pattern FLOW_VERSION_PATTERN =
            Pattern.compile("\"flowVersion\"\\s*:\\s*\"([^\"]+)\"");

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
        return "flow-version";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "java/src/main/resources/targets/claude/scripts/java-maven/audit-flow-version.sh.tpl");
    }

    private void checkStateFile(Path stateFile, List<AuditViolation> violations) {
        String content = readFile(stateFile);
        Matcher m = FLOW_VERSION_PATTERN.matcher(content);
        if (!m.find()) {
            violations.add(
                    new AuditViolation(
                            stateFile,
                            0,
                            "MISSING_FLOW_VERSION",
                            "execution-state.json has no flowVersion field"));
            return;
        }
        String version = m.group(1);
        if (!VALID_VERSIONS.contains(version)) {
            violations.add(
                    new AuditViolation(
                            stateFile,
                            0,
                            "INVALID_FLOW_VERSION",
                            "flowVersion='" + version + "' is not in valid set " + VALID_VERSIONS));
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
