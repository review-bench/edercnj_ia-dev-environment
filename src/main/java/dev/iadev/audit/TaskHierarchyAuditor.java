package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Java equivalent of {@code audit-task-hierarchy.sh} (Rule 25).
 *
 * <p>Checks that orchestrator SKILL.md files declare {@code TaskCreate} inside each {@code ## Phase
 * N} section. Phases without TaskCreate are a hierarchy violation.
 */
public final class TaskHierarchyAuditor implements Auditor {

    private static final String VIOLATION_NAME = "TASK_HIERARCHY_VIOLATION";
    private static final Pattern PHASE_HEADER = Pattern.compile("^## Phase \\d+");

    @Override
    public AuditResult audit(AuditCorpus corpus) {
        List<AuditViolation> violations = new ArrayList<>();
        corpus.walkSkills()
                .filter(TaskHierarchyAuditor::isOrchestrator)
                .forEach(skill -> checkSkill(skill, violations));
        return violations.isEmpty()
                ? AuditResult.ok()
                : AuditResult.violation(VIOLATION_NAME, violations);
    }

    @Override
    public String name() {
        return "task-hierarchy";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "src/main/resources/targets/claude/scripts/java-maven/audit-task-hierarchy.sh.tpl");
    }

    private static boolean isOrchestrator(Path skillPath) {
        try {
            String content = Files.readString(skillPath);
            return content.contains("x-epic-implement")
                    || content.contains("x-implement-story")
                    || content.contains("x-implement-task");
        } catch (IOException e) {
            return false;
        }
    }

    private void checkSkill(Path skillPath, List<AuditViolation> violations) {
        String content = readFile(skillPath);
        String[] lines = content.split("\n");
        int currentPhaseStart = -1;
        boolean phaseHasTaskCreate = false;
        boolean phaseHasNoGate = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (PHASE_HEADER.matcher(line.trim()).find()) {
                if (currentPhaseStart >= 0 && !phaseHasTaskCreate && !phaseHasNoGate) {
                    violations.add(
                            new AuditViolation(
                                    skillPath,
                                    currentPhaseStart,
                                    "MISSING_TASK_CREATE",
                                    "Phase section lacks TaskCreate: "
                                            + lines[currentPhaseStart].strip()));
                }
                currentPhaseStart = i + 1;
                phaseHasTaskCreate = false;
                phaseHasNoGate = (i > 0) && lines[i - 1].contains("phase-no-gate");
            }
            if (line.contains("TaskCreate(") && currentPhaseStart >= 0) {
                phaseHasTaskCreate = true;
            }
        }
        if (currentPhaseStart >= 0 && !phaseHasTaskCreate && !phaseHasNoGate) {
            violations.add(
                    new AuditViolation(
                            skillPath,
                            currentPhaseStart,
                            "MISSING_TASK_CREATE",
                            "Last phase section lacks TaskCreate"));
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
