package dev.iadev.application.assembler.gates;

import dev.iadev.domain.model.ProjectConfig;
import java.util.ArrayList;
import java.util.List;

/**
 * Contributes security-scanning skills based on SAST, DAST, secret-scan, container-scan, infra-scan
 * flags, and the quality-gate provider selection.
 */
public final class SecurityScanningGate implements SkillGateEvaluator {

    @Override
    public List<String> evaluate(ProjectConfig config) {
        List<String> skills = new ArrayList<>();
        var scanning = config.security().scanning();
        if (scanning.sast()) {
            skills.add("x-run-sast");
        }
        if (scanning.dast()) {
            skills.add("x-run-dast");
        }
        if (scanning.secretScan()) {
            skills.add("x-scan-secrets");
        }
        if (scanning.containerScan()) {
            skills.add("x-scan-container-security");
        }
        if (scanning.infraScan()) {
            skills.add("x-assess-infrastructure-security");
        }
        String qgProvider = config.security().qualityGate().provider();
        if (!"none".equalsIgnoreCase(qgProvider)) {
            skills.add("x-run-sonar-security");
        }
        return skills;
    }
}
