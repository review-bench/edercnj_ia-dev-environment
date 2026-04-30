package dev.iadev.audit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Java equivalent of {@code audit-phase-gates.sh} (Rule 25).
 *
 * <p>Checks that orchestrator SKILL.md files invoke {@code x-internal-phase-gate} in each {@code ##
 * Phase N} section that has a {@code TaskCreate}. Phases with tasks but no gate (and no {@code
 * phase-no-gate} exemption) are violations.
 */
public final class PhaseGatesAuditor implements Auditor {

    private static final String VIOLATION_NAME = "PHASE_GATE_VIOLATION";
    private static final Pattern PHASE_HEADER = Pattern.compile("^## Phase \\d+");

    @Override
    public AuditResult audit(AuditCorpus corpus) {
        List<AuditViolation> violations = new ArrayList<>();
        corpus.walkSkills()
                .filter(PhaseGatesAuditor::isOrchestrator)
                .forEach(skill -> checkSkill(skill, violations));
        return violations.isEmpty()
                ? AuditResult.ok()
                : AuditResult.violation(VIOLATION_NAME, violations);
    }

    @Override
    public String name() {
        return "phase-gates";
    }

    @Override
    public Path bashEquivalentTemplate() {
        return Path.of(
                "src/main/resources/targets/claude/scripts/java-maven/audit-phase-gates.sh.tpl");
    }

    private static boolean isOrchestrator(Path skillPath) {
        try {
            String content = Files.readString(skillPath);
            return content.contains("TaskCreate(") && content.contains("## Phase");
        } catch (IOException e) {
            return false;
        }
    }

    private void checkSkill(Path skillPath, List<AuditViolation> violations) {
        String content = readFile(skillPath);
        String[] lines = content.split("\n");
        int phaseLineNum = -1;
        boolean hasTask = false;
        boolean hasGate = false;
        boolean hasNoGateExemption = false;
        String phaseLine = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (PHASE_HEADER.matcher(line.trim()).find()) {
                if (phaseLineNum >= 0 && hasTask && !hasGate && !hasNoGateExemption) {
                    violations.add(
                            new AuditViolation(
                                    skillPath,
                                    phaseLineNum,
                                    "MISSING_PHASE_GATE",
                                    "Phase has TaskCreate but no x-internal-phase-gate: "
                                            + phaseLine.strip()));
                }
                phaseLineNum = i + 1;
                phaseLine = line;
                hasTask = false;
                hasGate = false;
                hasNoGateExemption = (i > 0 && lines[i - 1].contains("phase-no-gate"));
            }
            if (line.contains("TaskCreate(")) hasTask = true;
            if (line.contains("x-internal-phase-gate")) hasGate = true;
        }
        if (phaseLineNum >= 0 && hasTask && !hasGate && !hasNoGateExemption) {
            violations.add(
                    new AuditViolation(
                            skillPath,
                            phaseLineNum,
                            "MISSING_PHASE_GATE",
                            "Last phase has TaskCreate but no x-internal-phase-gate"));
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
