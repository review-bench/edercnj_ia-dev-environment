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
 * Java equivalent of {@code audit-execution-integrity.sh} (Rules 24, 27).
 *
 * <p>For each story with {@code status=COMPLETE} and {@code prMergeStatus=MERGED} in
 * {@code execution-state.json}, checks that the story completion report exists on disk.
 * Absent evidence is an {@code EIE_EVIDENCE_MISSING} violation.
 */
public final class ExecutionIntegrityAuditor implements Auditor {

    private static final String VIOLATION_NAME = "EIE_EVIDENCE_MISSING";
    private static final Pattern STORY_STATUS_PATTERN =
            Pattern.compile("\"(story-[0-9]+-[0-9]+)\"\\s*:\\s*\\{[^}]*\"status\"\\s*:\\s*\"COMPLETE\"[^}]*\"prMergeStatus\"\\s*:\\s*\"MERGED\"");

    @Override
    public AuditResult audit(AuditCorpus corpus) {
        List<AuditViolation> violations = new ArrayList<>();
        corpus.walkExecutionStates().forEach(stateFile -> checkStateFile(stateFile, corpus.rootDir(), violations));
        return violations.isEmpty()
                ? AuditResult.ok()
                : AuditResult.violation(VIOLATION_NAME, violations);
    }

    @Override
    public String name() {
        return "execution-integrity";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of("java/src/main/resources/targets/claude/scripts/java-maven/audit-execution-integrity.sh.tpl");
    }

    private void checkStateFile(Path stateFile, Path rootDir, List<AuditViolation> violations) {
        String content = readFile(stateFile);
        Matcher m = STORY_STATUS_PATTERN.matcher(content);
        while (m.find()) {
            String storyId = m.group(1);
            String epicId = extractEpicId(content);
            if (epicId == null) continue;
            String reportPath = "ai/epics/" + epicId.toLowerCase().replace("epic-", "epic-") + "-*/reports/story-completion-report-" + storyId + ".md";
            boolean evidenceFound = findEvidence(rootDir, storyId);
            if (!evidenceFound) {
                violations.add(new AuditViolation(stateFile, 0, "EIE_EVIDENCE_MISSING",
                        "Merged story " + storyId + " has no story-completion-report on disk"));
            }
        }
    }

    private boolean findEvidence(Path rootDir, String storyId) {
        String reportName = "story-completion-report-" + storyId + ".md";
        try {
            return Files.walk(rootDir)
                    .anyMatch(p -> p.getFileName().toString().equals(reportName));
        } catch (IOException e) {
            return false;
        }
    }

    private static String extractEpicId(String content) {
        Pattern p = Pattern.compile("\"epicId\"\\s*:\\s*\"(EPIC-[0-9]+)\"");
        Matcher m = p.matcher(content);
        return m.find() ? m.group(1) : null;
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read: " + path, e);
        }
    }
}
